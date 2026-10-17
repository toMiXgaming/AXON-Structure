package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Zrzuty zaopatrzenia: kilka razy dziennie, o losowej porze w podanych godzinach, nad jedną z postawionych struktur (miejsca z pulą zrzutów w pliku .axs,
 * np. lądowisko jednostki i dach szpitala) spada na spadochronie skrzynia z rzadkim lootem. Serwer dostaje
 * ogłoszenie ze współrzędnymi, a nad skrzynią unosi się dym, dopóki ktoś jej nie opróżni albo nie minie jej czas.
 */
public final class AirdropService {

    /**
     * @param perDay ile zrzutów dziennie; okno godzin dzielone jest na tyle równych części, w każdej jeden zrzut
     * @param hourFrom początek okna (pełna godzina)
     * @param hourTo koniec okna (pełna godzina, wyłącznie)
     * @param signalSmoke  wysoki czerwony słup dymu nad wylądowaną skrzynią i smuga za spadającą (widać z daleka)
     * @param flareSeconds co ile sekund nad skrzynią wybucha czerwona flara (0 = bez flar)
     * @param glowing      spadająca skrzynia i spadochron świecą na czerwono (widać przez ściany)
     */
    public record Settings(boolean enabled, int perDay, int hourFrom, int hourTo, ZoneId zone, int minPlayers, String pool,
                           int lifetimeMinutes, int fallHeight, int fallSeconds, String crateName,
                           String announce, String landed, String expired,
                           boolean signalSmoke, int flareSeconds, boolean glowing) {
    }

    /** Czerwień sygnałowa zrzutu (dym, flary, poświata). */
    private static final Color SIGNAL_RED = Color.fromRGB(0xE0, 0x1B, 0x1B);
    private static final Color SIGNAL_ORANGE = Color.fromRGB(0xFF, 0x8A, 0x1F);
    private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(SIGNAL_RED, 2.5f);
    private static final Particle.DustTransition FLARE_DUST = new Particle.DustTransition(SIGNAL_RED, SIGNAL_ORANGE, 2.0f);
    /** Wysokość słupa dymu nad skrzynią. */
    private static final int SMOKE_COLUMN = 36;
    /** Do kogo dociera huk lądowania (w blokach). */
    private static final double BOOM_RANGE = 300;
    private static final long EFFECT_PERIOD = 10L;

    private record Target(StructureInstance instance, Blueprint blueprint, World world, int x, int y, int z) {
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final CrateService crates;
    private final Random random = new Random();
    private Settings settings;
    private BukkitTask task;
    private final File stateFile;
    private LocalDate day;
    private final Set<Integer> done = new HashSet<>();
    private ZonedDateTime[] planned = new ZonedDateTime[0];
    /** Gdzie spadnie następny zrzut: losowane z wyprzedzeniem, żeby radiostacja w oazie mogła to zdradzić. */
    private Target nextTarget;

    // aktywny zrzut (jeden naraz)
    private Target active;
    private BlockDisplay crateDisplay;
    private BlockDisplay chuteDisplay;
    private BukkitTask fallTask;
    /** Dym i flary nad wylądowaną skrzynią (jeden zegar co 10 ticków, tylko gdy zrzut leży). */
    private BukkitTask effectTask;
    private int effectTicks;
    /** Chunk miejsca lądowania trzymany wczytany przez całe spadanie (bilet pluginu). */
    private World ticketWorld;
    private int ticketX;
    private int ticketZ;
    private long expiresAt;
    private boolean landed;
    /** Czy w skrzyni zrzutu coś leży (przy {@code loot.puste} jest pusta i znika dopiero po czasie). */
    private boolean stocked;

    public AirdropService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, CrateService crates) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.crates = crates;
        this.stateFile = new File(plugin.getDataFolder(), "zrzuty.yml");
        loadState();
    }

    public void configure(Settings settings) {
        this.settings = settings;
        crates.dropPool(settings.pool());
        plan(ZonedDateTime.now(settings.zone()));
    }

    public Settings settings() {
        return settings;
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 10, 20L * 2);
    }

    public void stop() {
        stopTimer();
        finish(false);
    }

    private void stopTimer() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private int windowMinutes() {
        int from = Math.max(0, Math.min(23, settings.hourFrom()));
        int to = Math.max(from + 1, Math.min(24, settings.hourTo()));
        return (to - from) * 60;
    }

    private ZonedDateTime slotStart(LocalDate date, int slot) {
        int from = Math.max(0, Math.min(23, settings.hourFrom()));
        int perDay = Math.max(1, settings.perDay());
        return date.atStartOfDay(settings.zone()).plusMinutes(from * 60L + (long) windowMinutes() * slot / perDay);
    }

    private ZonedDateTime slotEnd(LocalDate date, int slot) {
        return slotStart(date, slot + 1);
    }

    /** Losuje godziny zrzutów na dziś dla części okna, które się jeszcze nie odbyły. */
    private void plan(ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        if (!today.equals(day)) {
            day = today;
            done.clear();
            saveState();
        }
        int perDay = Math.max(1, settings.perDay());
        planned = new ZonedDateTime[perDay];
        for (int slot = 0; slot < perDay; slot++) {
            ZonedDateTime start = slotStart(today, slot);
            ZonedDateTime end = slotEnd(today, slot);
            if (now.isAfter(start)) {
                start = now;
            }
            long span = Math.max(1, java.time.Duration.between(start, end).toMinutes());
            planned[slot] = start.plusMinutes(random.nextLong(span));
        }
    }

    /** Najbliższy zaplanowany zrzut na dziś albo null. */
    private ZonedDateTime nextPlanned() {
        ZonedDateTime now = ZonedDateTime.now(settings.zone());
        ZonedDateTime best = null;
        for (int slot = 0; slot < planned.length; slot++) {
            if (!done.contains(slot) && now.isBefore(slotEnd(day, slot)) && (best == null || planned[slot].isBefore(best))) {
                best = planned[slot];
            }
        }
        return best;
    }

    /** Godzina najbliższego zaplanowanego zrzutu (do komendy). */
    public String nextInfo() {
        ZonedDateTime best = nextPlanned();
        if (best == null) {
            return "dziś już nie będzie, jutro między " + settings.hourFrom() + ":00 a " + settings.hourTo() + ":00";
        }
        return "dziś ok. " + best.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void loadState() {
        if (!stateFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        try {
            day = LocalDate.parse(yaml.getString("dzien", ""));
            done.addAll(yaml.getIntegerList("wykonane"));
        } catch (RuntimeException e) {
            day = null;
            done.clear();
        }
    }

    private void saveState() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("dzien", day == null ? "" : day.toString());
        yaml.set("wykonane", new ArrayList<>(done));
        try {
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać " + stateFile.getName() + ": " + e.getMessage());
        }
    }

    private Target nextTarget() {
        if (nextTarget == null || store.get(nextTarget.instance().id()) == null) {
            List<Target> targets = targets();
            nextTarget = targets.isEmpty() ? null : targets.get(random.nextInt(targets.size()));
        }
        return nextTarget;
    }

    /**
     * Co wie radiostacja w oazie: gdzie leży trwający zrzut albo nad czym i o której spadnie następny.
     *
     * @return wiadomość dla gracza albo null, gdy dziś nic już nie spadnie
     */
    public String radioInfo() {
        if (active != null) {
            return "§fZrzut " + (landed ? "leży" : "spada") + " nad §e" + active.blueprint().name()
                    + " §7(" + active.x() + ", " + active.y() + ", " + active.z() + ")§f.";
        }
        ZonedDateTime best = settings.enabled() ? nextPlanned() : null;
        Target target = best == null ? null : nextTarget();
        if (target == null) {
            return null;
        }
        return "§fNastępny zrzut spadnie nad §e" + target.blueprint().name() + " §7(" + target.x() + ", "
                + target.z() + ")§f, około §e" + best.format(DateTimeFormatter.ofPattern("HH:mm")) + "§f.";
    }

    public boolean isActive() {
        return active != null;
    }

    private void tick() {
        if (active != null) {
            if (landed) {
                if (System.currentTimeMillis() >= expiresAt || (stocked ? crateEmptied() : crateGone())) {
                    finish(true);
                }
            }
            return;
        }
        if (!settings.enabled()) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now(settings.zone());
        if (!now.toLocalDate().equals(day)) {
            plan(now);
        }
        for (int slot = 0; slot < planned.length; slot++) {
            if (done.contains(slot) || now.isBefore(planned[slot])) {
                continue;
            }
            if (!now.isBefore(slotEnd(day, slot))) {
                // za mało graczy przez całą część okna – ten zrzut przepada
                done.add(slot);
                saveState();
                continue;
            }
            if (Bukkit.getOnlinePlayers().size() < settings.minPlayers()) {
                continue; // czekamy, aż wejdzie więcej graczy (do końca tej części okna)
            }
            done.add(slot);
            saveState();
            trigger(null);
            return;
        }
    }

    /**
     * Zrzuca skrzynię teraz.
     *
     * @return opis wyniku do komendy
     */
    public String trigger(CommandSender sender) {
        if (active != null) {
            return "§cJeden zrzut już trwa (" + active.x() + " " + active.y() + " " + active.z() + ").";
        }
        Target target = nextTarget();
        nextTarget = null;
        if (target == null) {
            return "§cŻadna postawiona struktura nie ma miejsca na zrzut (pula §e" + settings.pool() + "§c).";
        }
        World world = target.world();
        world.getChunkAt(target.x() >> 4, target.z() >> 4); // wczytuje chunk, jeśli trzeba
        // spadanie trwa długo (domyślnie 45 s): bez biletu chunk mógłby się wyładować, a z nim spadochron
        releaseTicket();
        if (world.addPluginChunkTicket(target.x() >> 4, target.z() >> 4, plugin)) {
            ticketWorld = world;
            ticketX = target.x() >> 4;
            ticketZ = target.z() >> 4;
        }
        active = target;
        landed = false;
        broadcast(format(settings.announce(), target));
        plugin.getLogger().info("Zrzut: spada nad " + target.blueprint().id() + " na " + target.x() + " "
                + target.y() + " " + target.z());
        startFall(target);
        return "§aZrzut spada nad §e" + target.blueprint().name() + " §a(" + target.x() + " " + target.y() + " "
                + target.z() + ").";
    }

    private List<Target> targets() {
        List<Target> out = new ArrayList<>();
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            World world = Bukkit.getWorld(instance.world());
            if (bp == null || world == null) {
                continue;
            }
            for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                if (point.pool().equalsIgnoreCase(settings.pool())) {
                    int[] w = instance.placement().toWorld(point.pos().x(), point.pos().y(), point.pos().z());
                    out.add(new Target(instance, bp, world, w[0], w[1], w[2]));
                }
            }
        }
        return out;
    }

    private void startFall(Target target) {
        World world = target.world();
        int ticks = Math.max(20, settings.fallSeconds() * 20);
        double height = Math.max(8, settings.fallHeight());
        Location start = new Location(world, target.x(), target.y() + height, target.z());
        // co 2 ticki nowa pozycja, a klient płynnie dociąga ją przez 2 ticki (teleport duration), więc nawet
        // 45 s spadania z 60 bloków (ok. 0,13 bloku na krok) wygląda jak ciągły ruch
        crateDisplay = world.spawn(start, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(Material.BARREL));
            d.setTeleportDuration(2);
            d.setPersistent(false);
            glow(d);
        });
        chuteDisplay = world.spawn(start, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(Material.WHITE_WOOL));
            d.setTransformation(new Transformation(new Vector3f(-1.25f, 2.2f, -1.25f), new AxisAngle4f(),
                    new Vector3f(3.5f, 0.35f, 3.5f), new AxisAngle4f()));
            d.setTeleportDuration(2);
            d.setPersistent(false);
            glow(d);
        });
        final int[] step = {0};
        fallTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            step[0] += 2;
            double t = Math.min(1.0, step[0] / (double) ticks);
            Location at = start.clone().subtract(0, height * t, 0);
            if (crateDisplay != null && crateDisplay.isValid()) {
                crateDisplay.teleport(at);
            }
            if (chuteDisplay != null && chuteDisplay.isValid()) {
                chuteDisplay.teleport(at);
            }
            world.spawnParticle(Particle.CLOUD, at.clone().add(0.5, 0, 0.5), 2, 0.2, 0.1, 0.2, 0.01);
            if (settings.signalSmoke()) {
                // smuga: dym sygnałowy i czerwony pył widoczne z daleka (force = do 512 bloków)
                Location trail = at.clone().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, trail, 1, 0.15, 0.1, 0.15, 0.01, null, true);
                world.spawnParticle(Particle.DUST, trail, 4, 0.3, 0.3, 0.3, 0, RED_DUST, true);
            }
            if (t >= 1.0) {
                land(target);
            }
        }, 2L, 2L);
    }

    private void land(Target target) {
        cancelFall();
        removeDisplays();
        World world = target.world();
        Block block = world.getBlockAt(target.x(), target.y(), target.z());
        if (!block.getType().isAir() && block.getType() != Material.CHEST && block.getType() != Material.BARREL) {
            plugin.getLogger().warning("Zrzut: miejsce " + target.x() + " " + target.y() + " " + target.z()
                    + " jest zajęte (" + block.getType() + "), skrzynia ląduje piętro wyżej.");
            block = block.getRelative(0, 1, 0);
        }
        block.setBlockData(Bukkit.createBlockData("minecraft:chest[facing=north,type=single]"), false);
        CrateClass crate = pick();
        if (block.getState() instanceof Container container) {
            setName(container, LootConfig.color(settings.crateName()));
            container.update(true, false);
        }
        stocked = false;
        if (block.getState() instanceof Container container) {
            container.getInventory().clear();
            if (crate != null && !crates.emptyLoot()) {
                crate.fill(container.getInventory(), random);
                stocked = !crateIsEmpty(container);
            }
        }
        active = new Target(target.instance(), target.blueprint(), world, block.getX(), block.getY(), block.getZ());
        landed = true;
        expiresAt = System.currentTimeMillis() + Math.max(1, settings.lifetimeMinutes()) * 60_000L;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.EXPLOSION, center, 1);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.6f);
        releaseTicket();
        landingSignals(world, center);
        startEffects();
        broadcast(format(settings.landed(), active));
        plugin.getLogger().info("Zrzut wylądował na " + block.getX() + " " + block.getY() + " " + block.getZ());
    }

    private CrateClass pick() {
        CratePool pool = crates.loot().pool(settings.pool());
        if (pool == null) {
            plugin.getLogger().warning("Brak puli '" + settings.pool() + "' w config.yml – zrzut będzie pusty.");
            return null;
        }
        String classId = pool.roll(random);
        return classId == null ? null : crates.loot().crateClass(classId);
    }

    private void releaseTicket() {
        if (ticketWorld != null) {
            ticketWorld.removePluginChunkTicket(ticketX, ticketZ, plugin);
            ticketWorld = null;
        }
    }

    private void glow(BlockDisplay display) {
        display.setViewRange(4f); // dalej niż zwykle (ile dokładnie, decyduje też zasięg śledzenia encji serwera)
        if (settings.glowing()) {
            display.setGlowing(true);
            display.setGlowColorOverride(SIGNAL_RED);
        }
    }

    /** Lądowanie: czerwony fajerwerk wysoko nad skrzynią i huk słyszany w promieniu {@link #BOOM_RANGE} bloków. */
    private void landingSignals(World world, Location center) {
        if (settings.signalSmoke() || settings.flareSeconds() > 0) {
            // wysoko, bo wybuch fajerwerku z gwiazdkami rani w promieniu kilku bloków
            Location sky = center.clone().add(0, 20, 0);
            try {
                Firework firework = world.spawn(sky, Firework.class, fw -> {
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(SIGNAL_RED).withFade(SIGNAL_ORANGE).flicker(true).trail(true).build());
                    meta.setPower(0);
                    fw.setFireworkMeta(meta);
                    fw.setPersistent(false);
                });
                firework.detonate();
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Zrzut: nie udało się odpalić fajerwerku: " + e.getMessage());
            }
        }
        double range = BOOM_RANGE * BOOM_RANGE;
        for (Player player : world.getPlayers()) {
            Location eye = player.getEyeLocation();
            double d2 = eye.distanceSquared(center);
            if (d2 > range) {
                continue;
            }
            // dźwięk kilkanaście bloków od gracza w stronę skrzyni: słychać kierunek, a nie dokładną odległość
            double d = Math.sqrt(d2);
            Location at = d < 12 ? center : eye.clone().add(center.toVector().subtract(eye.toVector()).multiply(12 / d));
            float volume = (float) Math.max(0.4, 1.6 - d / BOOM_RANGE);
            player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, volume, 0.5f);
            player.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, volume, 0.7f);
        }
    }

    private void startEffects() {
        cancelEffects();
        effectTicks = 0;
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, this::effects, EFFECT_PERIOD, EFFECT_PERIOD);
    }

    private void cancelEffects() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
    }

    /** Słup dymu i flary nad wylądowaną skrzynią; cząsteczki z force widać do 512 bloków. */
    private void effects() {
        if (active == null || !landed) {
            cancelEffects();
            return;
        }
        effectTicks += (int) EFFECT_PERIOD;
        World world = active.world();
        Location base = new Location(world, active.x() + 0.5, active.y() + 1.0, active.z() + 0.5);
        if (settings.signalSmoke()) {
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, base, 3, 0.15, 0.1, 0.15, 0.02, null, true);
            // słup: dym sygnałowy sam się wznosi, czerwony pył barwi go na całej wysokości
            for (int i = 0; i < 4; i++) {
                Location at = base.clone().add(0, 2 + random.nextDouble() * SMOKE_COLUMN, 0);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, at, 1, 0.3, 0.3, 0.3, 0.01, null, true);
                world.spawnParticle(Particle.DUST, at, 3, 0.4, 0.6, 0.4, 0, RED_DUST, true);
            }
        } else {
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, base, 3, 0.15, 0.1, 0.15, 0.02);
        }
        int flareTicks = settings.flareSeconds() * 20;
        if (flareTicks > 0 && effectTicks % flareTicks < EFFECT_PERIOD) {
            Location flare = base.clone().add(0, SMOKE_COLUMN * 0.7, 0);
            world.spawnParticle(Particle.FIREWORK, flare, 40, 0.3, 0.3, 0.3, 0.15, null, true);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, flare, 30, 1.5, 1.5, 1.5, 0, FLARE_DUST, true);
            world.playSound(flare, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 4f, 0.8f);
        }
    }

    private boolean crateEmptied() {
        if (!active.world().isChunkLoaded(active.x() >> 4, active.z() >> 4)) {
            return false;
        }
        Block block = active.world().getBlockAt(active.x(), active.y(), active.z());
        return !(block.getState() instanceof Container container) || crateIsEmpty(container);
    }

    /** Skrzynia zrzutu zniknęła (pusta skrzynia przy {@code loot.puste} nie kończy zrzutu samym otwarciem). */
    private boolean crateGone() {
        return active.world().isChunkLoaded(active.x() >> 4, active.z() >> 4)
                && !(active.world().getBlockAt(active.x(), active.y(), active.z()).getState() instanceof Container);
    }

    private static boolean crateIsEmpty(Container container) {
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Kończy zrzut: zabiera skrzynię (jeśli jeszcze stoi) i sprząta animację. */
    private void finish(boolean announce) {
        cancelFall();
        cancelEffects();
        releaseTicket();
        removeDisplays();
        if (active != null && landed) {
            Block block = active.world().getBlockAt(active.x(), active.y(), active.z());
            boolean emptied = !stocked || crateEmptied();
            if (block.getType() == Material.CHEST) {
                if (block.getState() instanceof Container container) {
                    container.getInventory().clear();
                }
                block.setBlockData(Bukkit.createBlockData(Material.AIR), false);
            }
            if (announce && !emptied) {
                broadcast(format(settings.expired(), active));
            }
        }
        active = null;
        landed = false;
    }

    private void cancelFall() {
        if (fallTask != null) {
            fallTask.cancel();
            fallTask = null;
        }
    }

    private void removeDisplays() {
        if (crateDisplay != null) {
            crateDisplay.remove();
            crateDisplay = null;
        }
        if (chuteDisplay != null) {
            chuteDisplay.remove();
            chuteDisplay = null;
        }
    }

    private String format(String text, Target target) {
        return LootConfig.color(text
                .replace("{nazwa}", target.blueprint().name())
                .replace("{x}", String.valueOf(target.x()))
                .replace("{y}", String.valueOf(target.y()))
                .replace("{z}", String.valueOf(target.z()))
                .replace("{minuty}", String.valueOf(settings.lifetimeMinutes())));
    }

    @SuppressWarnings("deprecation")
    private static void broadcast(String message) {
        if (!message.isBlank()) {
            Bukkit.broadcastMessage(message);
        }
    }

    @SuppressWarnings("deprecation")
    private static void setName(Container container, String name) {
        container.setCustomName(name);
    }
}
