package pl.axon.structure.oasis;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.loot.AirdropService;
import pl.axon.structure.loot.LootConfig;
import pl.axon.structure.npc.GuardService;
import pl.axon.structure.world.InstanceStore;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Oaza: struktury ze {@code strefa: bezpieczna}. W ich obrysie nie ma PvP, strażnicy bronią tylko przed potworami,
 * a punkty z pliku struktury ({@code punkt-*}) to stanowiska: wymiana, sklep, depozyt, tablica zleceń, automaty,
 * radiostacja i strzelnica. Dane graczy (depozyty, zlecenia, ranking) są w {@code oaza.yml}.
 */
public final class OasisService implements Listener {

    /** Punkt oazy w świecie. */
    public record Point(StructureInstance instance, World world, int x, int y, int z, String facing, String pool) {

        public Location center() {
            return new Location(world, x + 0.5, y + 0.5, z + 0.5);
        }

        String key() {
            return instance.id() + ":" + x + ":" + y + ":" + z;
        }
    }

    /** Punkty-lady: klika się sam blok (albo zaraz obok). */
    private static final Set<String> COUNTERS = Set.of("punkt-handel", "punkt-sklep", "punkt-depozyt", "punkt-automat", "punkt-radio");
    private static final long POINTS_TTL_MS = 10_000;
    private static final long WARN_COOLDOWN_MS = 2_000;

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final GuardService guards;
    private final AirdropService airdrops;
    private final Vault vault = new Vault();
    private final File dataFile;
    private YamlConfiguration data;
    private boolean dirty;

    private final TradeService trade = new TradeService(this);
    private final ShopService shop = new ShopService(this);
    private final StashService stash = new StashService(this);
    private final JobBoard jobs = new JobBoard(this);
    private final SlotMachine slots = new SlotMachine(this);
    private final ShootingRange range = new ShootingRange(this);

    private boolean enabled = true;
    private String enterTitle = "";
    private String enterSubtitle = "";
    private String leaveTitle = "";
    private String leaveSubtitle = "";
    private String pvpMessage = "";
    private double radioPrice = 500;

    private List<Point> points = List.of();
    private long pointsAt;
    private final Map<UUID, String> inside = new HashMap<>();
    private final Map<UUID, Long> lastWarn = new HashMap<>();
    private BukkitTask task;

    public OasisService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, GuardService guards,
                        AirdropService airdrops) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.guards = guards;
        this.airdrops = airdrops;
        this.dataFile = new File(plugin.getDataFolder(), "oaza.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void configure(ConfigurationSection section, ZoneId zone) {
        enabled = section == null || section.getBoolean("wlaczona", true);
        enterTitle = text(section, "tytul-wejscie", "&a&lOaza Świt");
        enterSubtitle = text(section, "podtytul-wejscie", "&7Strefa bez przemocy. Broń w kaburze.");
        leaveTitle = text(section, "tytul-wyjscie", "&c&lOpuszczasz oazę");
        leaveSubtitle = text(section, "podtytul-wyjscie", "&7Dalej już strzelają.");
        pvpMessage = text(section, "komunikat-pvp", "&cW oazie się nie strzela do ludzi.");
        radioPrice = section == null ? 500 : Math.max(0, section.getDouble("radio.cena", 500));
        shop.configure(section == null ? null : section.getConfigurationSection("sklep"), plugin.getLogger());
        stash.configure(section == null ? null : section.getConfigurationSection("depozyt"));
        jobs.configure(section == null ? null : section.getConfigurationSection("zlecenia"), plugin.getLogger());
        slots.configure(section == null ? null : section.getConfigurationSection("automaty"));
        range.configure(section == null ? null : section.getConfigurationSection("strzelnica"), zone);
        pointsAt = 0;
    }

    private static String text(ConfigurationSection section, String key, String fallback) {
        return LootConfig.color(section == null ? fallback : section.getString(key, fallback));
    }

    public void start() {
        stop();
        range.hookQualityArmory();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            range.rollWeek();
            save(false);
        }, 20L * 60, 20L * 60);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        trade.cancelAll();
        stash.saveAll();
        save(true);
    }

    JavaPlugin plugin() {
        return plugin;
    }

    Vault vault() {
        return vault;
    }

    YamlConfiguration data() {
        return data;
    }

    void markDirty() {
        dirty = true;
    }

    private void save(boolean force) {
        if (!dirty && !force) {
            return;
        }
        try {
            data.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać " + dataFile.getName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ strefa i punkty

    /** Oaza, w której obrysie stoi to miejsce, albo null. */
    public StructureInstance safeAt(Location location) {
        if (!enabled || location.getWorld() == null) {
            return null;
        }
        String world = location.getWorld().getName();
        for (StructureInstance instance : store.inWorld(world)) {
            Blueprint bp = library.get(instance.structureId());
            if (bp == null || !bp.safeZone()) {
                continue;
            }
            Box box = instance.footprint() != null ? instance.footprint() : Box.of(bp, instance.placement());
            if (box.contains(location.getBlockX(), location.getBlockZ())) {
                return instance;
            }
        }
        return null;
    }

    /** Wszystkie punkty {@code punkt-*} postawionych oaz (odświeżane co kilka sekund). */
    public List<Point> points() {
        long now = System.currentTimeMillis();
        if (now - pointsAt < POINTS_TTL_MS) {
            return points;
        }
        List<Point> out = new ArrayList<>();
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            World world = Bukkit.getWorld(instance.world());
            if (bp == null || world == null || !bp.safeZone()) {
                continue;
            }
            for (Blueprint.SpawnPoint p : bp.spawnPoints()) {
                String pool = p.pool().toLowerCase(Locale.ROOT);
                if (!pool.startsWith("punkt-")) {
                    continue;
                }
                int[] w = instance.placement().toWorld(p.pos().x(), p.pos().y(), p.pos().z());
                out.add(new Point(instance, world, w[0], w[1], w[2], instance.placement().rotateFacing(p.facing()), pool));
            }
        }
        points = out;
        pointsAt = now;
        return out;
    }

    public int count(String pool) {
        int n = 0;
        for (Point p : points()) {
            if (p.pool().equals(pool)) {
                n++;
            }
        }
        return n;
    }

    /** Najbliższy punkt danej puli w promieniu {@code maxDistance} od środka bloku albo null. */
    Point pointAt(Block block, String pool, double maxDistance) {
        Point best = null;
        double bestDistance = maxDistance * maxDistance + 1e-6;
        for (Point p : points()) {
            if (!p.pool().equals(pool) || !p.world().equals(block.getWorld())) {
                continue;
            }
            double dx = p.x() - block.getX();
            double dy = p.y() - block.getY();
            double dz = p.z() - block.getZ();
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= bestDistance) {
                best = p;
                bestDistance = d;
            }
        }
        return best;
    }

    Point nearestPoint(Location location, String pool) {
        Point best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Point p : points()) {
            if (p.pool().equals(pool) && p.world().equals(location.getWorld())) {
                double d = p.center().distanceSquared(location);
                if (d < bestDistance) {
                    best = p;
                    bestDistance = d;
                }
            }
        }
        return best;
    }

    @SuppressWarnings("deprecation")
    static void actionBar(Player player, String text) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    // ------------------------------------------------------------------ bez PvP

    private static Player attacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        ProjectileSource source = null;
        if (damager instanceof Projectile projectile) {
            source = projectile.getShooter();
        } else if (damager instanceof AreaEffectCloud cloud) {
            source = cloud.getSource();
        } else if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        return source instanceof Player p ? p : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || guards.kitOf(attacker) != null) {
            return; // strażnik oazy strzelający do potworów
        }
        Entity victim = event.getEntity();
        GuardService.Kit kit = guards.kitOf(victim);
        if (kit != null) {
            if (!kit.attacksPlayers()) {
                event.setCancelled(true); // strażnicy i handlarz oazy są nietykalni
            }
            return;
        }
        if (victim instanceof Player && !victim.equals(attacker)
                && (safeAt(victim.getLocation()) != null || safeAt(attacker.getLocation()) != null)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            Long last = lastWarn.get(attacker.getUniqueId());
            if (last == null || now - last > WARN_COOLDOWN_MS) {
                lastWarn.put(attacker.getUniqueId(), now);
                actionBar(attacker, pvpMessage);
            }
        }
    }

    // ------------------------------------------------------------------ wejście i wyjście

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to.getBlockX() == event.getFrom().getBlockX() && to.getBlockZ() == event.getFrom().getBlockZ()
                && to.getWorld() == event.getFrom().getWorld()) {
            return;
        }
        Player player = event.getPlayer();
        StructureInstance now = safeAt(to);
        String was = inside.get(player.getUniqueId());
        if (now != null && !now.id().equals(was)) {
            inside.put(player.getUniqueId(), now.id());
            player.sendTitle(enterTitle, enterSubtitle, 10, 50, 15);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.4f);
        } else if (now == null && was != null) {
            inside.remove(player.getUniqueId());
            player.sendTitle(leaveTitle, leaveSubtitle, 10, 40, 15);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.0f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inside.remove(event.getPlayer().getUniqueId());
        lastWarn.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ stanowiska

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (safeAt(block.getLocation()) == null) {
            return;
        }
        Player player = event.getPlayer();
        Point point = null;
        for (String pool : COUNTERS) {
            Point p = pointAt(block, pool, 1.0);
            if (p != null && (point == null || p.center().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5))
                    < point.center().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)))) {
                point = p;
            }
        }
        if (point == null) {
            point = pointAt(block, "punkt-zlecenia", 3.0);
        }
        if (point == null) {
            return;
        }
        event.setCancelled(true);
        switch (point.pool()) {
            case "punkt-handel" -> trade.use(player, point);
            case "punkt-sklep" -> shop.open(player);
            case "punkt-depozyt" -> stash.open(player);
            case "punkt-automat" -> slots.play(player);
            case "punkt-radio" -> radio(player);
            case "punkt-zlecenia" -> jobs.open(player);
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        GuardService.Kit kit = guards.kitOf(event.getRightClicked());
        if (kit != null && kit.id().equals(GuardService.TRADER_KIT)) {
            event.setCancelled(true);
            shop.open(event.getPlayer());
        }
    }

    private void radio(Player player) {
        String info = airdrops.radioInfo();
        if (info == null) {
            player.sendMessage("§7Radio milczy: dziś nie będzie już żadnego zrzutu.");
            return;
        }
        if (radioPrice > 0) {
            if (!vault.available() || !vault.withdraw(player, radioPrice)) {
                player.sendMessage("§cNamiar na zrzut kosztuje " + vault.format(radioPrice) + ".");
                return;
            }
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 0.7f);
        player.sendMessage("§2§l[Radio] §7*szum* §f" + info);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() != null && event.getEntity().getShooter() instanceof Player player) {
            range.hit(player, event.getHitBlock());
        }
    }

    // ------------------------------------------------------------------ okna

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.click(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.drag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.close(event);
        }
    }

    // ------------------------------------------------------------------ do komend

    public void showRanking(org.bukkit.command.CommandSender sender) {
        range.showRanking(sender);
    }

    /** Krótki opis stanu oazy (do /struktury oaza). */
    public List<String> status() {
        List<String> lines = new ArrayList<>();
        lines.add("§eOaz postawionych: §f" + points().stream().map(p -> p.instance().id()).distinct().count()
                + "§e, ekonomia (Vault): " + (vault.available() ? "§atak" : "§cnie"));
        lines.add("§eStanowisk wymiany: §f" + count("punkt-handel") + "§e, sklep: §f" + count("punkt-sklep")
                + " §e(" + shop.size() + " towarów), depozyt: §f" + count("punkt-depozyt") + "§e, automaty: §f"
                + count("punkt-automat") + "§e, radio: §f" + count("punkt-radio") + "§e, tarcze: §f" + count("punkt-tarcza"));
        return lines;
    }
}
