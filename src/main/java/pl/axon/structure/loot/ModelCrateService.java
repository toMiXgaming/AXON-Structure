package pl.axon.structure.loot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Pojemniki-modele: w świecie nie ma prawdziwego bloku, tylko {@link ItemDisplay} z modelem
 * {@code axon:<nazwa>} oraz {@link Interaction} jako hitbox. Klik w hitbox otwiera wirtualny
 * ekwipunek 27 slotów.
 *
 * <p>Ten sam mechanizm co beczki z etapu 4, ale bez bloku – dzięki temu skrzynia może mieć
 * dowolny wygląd z paczki (wojskowa, elitarna, zrzut) i nie da się jej zniszczyć ani
 * zduplikować przez postawienie bloku w tym miejscu.
 *
 * <p>Loot jest losowany przy pierwszym otwarciu po odświeżeniu (leniwie), a nie przy stawianiu –
 * dzięki temu skrzynia nie zajmuje pamięci, dopóki nikt do niej nie podejdzie.
 */
public final class ModelCrateService implements Listener {

    /** Rozmiar wirtualnego ekwipunku pojemnika-modelu. */
    public static final int SLOTS = 27;

    private static final String MODEL_TAG = "axon_model";
    private static final String POOL_TAG = "axon_pool";
    private static final String ZONE_TAG = "axon_zone";
    /** Znacznik odblokowania (epoch millis) – trzymany na hitboxie, więc przeżywa restart. */
    private static final String LOCK_TAG = "axon_lock_until";

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey modelKey;
    private final NamespacedKey poolKey;
    private final NamespacedKey zoneKey;
    private final NamespacedKey lockKey;

    /** Odliczanie nad zablokowanymi skrzyniami – 1 s, dotyka tylko wczytanych chunków. */
    private BukkitTask lockTask;

    /** Otwarte ekwipunki: gracz → stan pojemnika (żeby zapisać loot po zamknięciu). */
    private final Map<UUID, OpenCrate> open = new HashMap<>();

    /**
     * Zawartość pojemników: id hitboxa → ekwipunek. Loot losujemy raz, przy pierwszym otwarciu
     * po odświeżeniu, i trzymamy tutaj – kolejne otwarcia pokazują to samo, dopóki struktura
     * się nie odświeży (wtedy {@link #forget(UUID)} czyści wpis).
     */
    private final Map<UUID, ItemStack[]> contents = new HashMap<>();

    private AxonLootBridge axonLoot;
    private LootConfig loot;

    public ModelCrateService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modelKey = new NamespacedKey(plugin, MODEL_TAG);
        this.poolKey = new NamespacedKey(plugin, POOL_TAG);
        this.zoneKey = new NamespacedKey(plugin, ZONE_TAG);
        this.lockKey = new NamespacedKey(plugin, LOCK_TAG);
    }

    public void configure(LootConfig loot, AxonLootBridge axonLoot) {
        this.loot = loot;
        this.axonLoot = axonLoot;
    }

    // ------------------------------------------------------------------
    //  Odliczanie blokady
    // ------------------------------------------------------------------

    /** Uruchamia odliczanie nad zablokowanymi skrzyniami (1 s). */
    public void start() {
        stop();
        lockTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLocks, 20L, 20L);
    }

    /** Zatrzymuje odliczanie (przy wyłączaniu / przeładowaniu pluginu). */
    public void stop() {
        if (lockTask != null) {
            lockTask.cancel();
            lockTask = null;
        }
    }

    /**
     * Odświeża odliczanie nad zablokowanymi skrzyniami i ogłasza odblokowanie.
     *
     * <p>Iterujemy po encjach świata, ale każdą encję sprawdzamy tanio (typ + PDC), a świat
     * i tak trzyma tylko wczytane encje – nie ma tu żadnego loadowania chunków.
     */
    private void tickLocks() {
        long now = System.currentTimeMillis();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Interaction hitbox) || !isModelEntity(hitbox)) {
                    continue;
                }
                Long unlockAt = hitbox.getPersistentDataContainer().get(lockKey, PersistentDataType.LONG);
                if (unlockAt == null || unlockAt <= CrateLockMath.UNLOCKED) {
                    continue;
                }
                if (unlockAt > now) {
                    updateCountdown(hitbox, unlockAt, now);
                } else {
                    unlock(hitbox, world);
                }
            }
        }
    }

    /** Ustawia (albo odświeża) tekst odliczania nad modelem. */
    private void updateCountdown(Interaction hitbox, long unlockAt, long now) {
        String line = CrateLockMath.displayLine(unlockAt, now);
        if (line == null) {
            return;
        }
        Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
        TextDisplay display = countdownOf(hitbox);
        if (display == null) {
            Location at = hitbox.getLocation().add(0, 1.35, 0);
            try {
                display = hitbox.getWorld().spawn(at, TextDisplay.class, entity -> {
                    entity.text(text);
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setSeeThrough(true);
                    entity.setShadowed(true);
                    entity.setPersistent(false);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setDefaultBackground(false);
                });
                hitbox.addPassenger(display);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Nie udało się pokazać odliczania skrzyni: " + e.getMessage());
                return;
            }
        } else {
            display.text(text);
        }
    }

    /** Zdejmuje blokadę: usuwa odliczanie, czyści znacznik i ogłasza otwarcie. */
    private void unlock(Interaction hitbox, World world) {
        TextDisplay display = countdownOf(hitbox);
        if (display != null) {
            display.remove();
        }
        PersistentDataContainer pdc = hitbox.getPersistentDataContainer();
        pdc.remove(lockKey);
        String zone = pdc.get(zoneKey, PersistentDataType.STRING);
        String modelName = pdc.get(modelKey, PersistentDataType.STRING);
        String label = modelName == null || modelName.isBlank() ? "skrzynia" : modelName;
        String message = zone == null || zone.isBlank()
                ? "&a&l[Zaopatrzenie] &fSkrzynia " + label + " &fzostała odblokowana!"
                : "&a&l[Zaopatrzenie] &fW strefie " + zone + " &fskrzynia " + label
                        + " &fzostała odblokowana!";
        String colored = LootConfig.color(message);
        for (Player player : world.getPlayers()) {
            player.sendMessage(colored);
        }
    }

    /** Wyświetlacz odliczania będący pasażerem hitboxa (albo {@code null}). */
    private TextDisplay countdownOf(Interaction hitbox) {
        for (Entity passenger : hitbox.getPassengers()) {
            if (passenger instanceof TextDisplay display) {
                return display;
            }
        }
        return null;
    }

    /** Klasy skrzyń i pule z config.yml – potrzebne zrzutom do losowania klasy. */
    public LootConfig loot() {
        return loot;
    }

    // ------------------------------------------------------------------
    //  Stawianie i usuwanie
    // ------------------------------------------------------------------

    /**
     * Stawia pojemnik-model w podanych współrzędnych świata.
     *
     * @param modelName nazwa modelu z paczki (bez prefiksu {@code axon:})
     * @param facing    kierunek patrzenia (north/south/east/west/up/down)
     * @param poolId    pula klas skrzyń z config.yml
     * @param zoneName  nazwa strefy do ogłoszeń (może być pusta)
     * @return {@code true}, gdy udało się postawić
     */
    public boolean place(World world, int[] w, String modelName, String facing, String poolId, String zoneName) {
        return place(world, w, modelName, facing, poolId, zoneName, 0);
    }

    /**
     * Stawia pojemnik-model z opcjonalną blokadą czasową.
     *
     * @param lockMinutes ile minut skrzynia ma być zablokowana (0 = brak blokady)
     */
    public boolean place(World world, int[] w, String modelName, String facing, String poolId, String zoneName,
                         int lockMinutes) {
        if (world == null || !world.isChunkLoaded(w[0] >> 4, w[2] >> 4)) {
            return false;
        }
        remove(world, w);
        long unlockAt = CrateLockMath.unlockAt(System.currentTimeMillis(), lockMinutes);
        Location base = new Location(world, w[0] + 0.5, w[1], w[2] + 0.5);
        try {
            ItemDisplay display = world.spawn(base, ItemDisplay.class, entity -> {
                entity.setItemStack(modelItem(modelName));
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                entity.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 1f, 0f),
                        new Vector3f(1f, 1f, 1f),
                        new AxisAngle4f(0f, 0f, 1f, 0f)));
                entity.setPersistent(true);
                entity.setInvulnerable(true);
                entity.setGravity(false);
                tag(entity.getPersistentDataContainer(), modelName, poolId, zoneName);
            });
            Interaction hitbox = world.spawn(base, Interaction.class, entity -> {
                entity.setInteractionWidth(1.0f);
                entity.setInteractionHeight(1.0f);
                entity.setResponsive(true);
                entity.setPersistent(true);
                entity.setInvulnerable(true);
                tag(entity.getPersistentDataContainer(), modelName, poolId, zoneName);
                if (unlockAt > CrateLockMath.UNLOCKED) {
                    entity.getPersistentDataContainer().set(lockKey, PersistentDataType.LONG, unlockAt);
                }
            });
            display.addPassenger(hitbox);
            if (unlockAt > CrateLockMath.UNLOCKED) {
                updateCountdown(hitbox, unlockAt, System.currentTimeMillis());
            }
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Nie udało się postawić pojemnika-modelu na "
                    + w[0] + " " + w[1] + " " + w[2] + ": " + e.getMessage());
            return false;
        }
    }

    /** Usuwa pojemnik-model (ItemDisplay + Interaction) w podanych współrzędnych. */
    public void remove(World world, int[] w) {
        if (world == null) {
            return;
        }
        Location center = new Location(world, w[0] + 0.5, w[1] + 0.5, w[2] + 0.5);
        for (Entity entity : world.getNearbyEntities(center, 1.2, 1.2, 1.2)) {
            if (isModelEntity(entity)) {
                entity.remove();
            }
        }
    }

    /** Czy w podanych współrzędnych stoi już pojemnik-model. */
    public boolean exists(World world, int[] w) {
        if (world == null) {
            return false;
        }
        Location center = new Location(world, w[0] + 0.5, w[1] + 0.5, w[2] + 0.5);
        for (Entity entity : world.getNearbyEntities(center, 1.2, 1.2, 1.2)) {
            if (isModelEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  Klik i wirtualny ekwipunek
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction hitbox)) {
            return;
        }
        PersistentDataContainer pdc = hitbox.getPersistentDataContainer();
        String poolId = pdc.get(poolKey, PersistentDataType.STRING);
        if (poolId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            return; // sneak = nie otwieraj (pozwala obejrzeć model)
        }
        Long unlockAt = pdc.get(lockKey, PersistentDataType.LONG);
        if (unlockAt != null && CrateLockMath.locked(unlockAt, System.currentTimeMillis())) {
            long left = CrateLockMath.remainingMillis(unlockAt, System.currentTimeMillis());
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&c&lZABLOKOWANA &7jeszcze &f" + CrateLockMath.countdown(left)));
            return;
        }
        openCrate(player, hitbox, poolId, pdc.get(zoneKey, PersistentDataType.STRING));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        OpenCrate crate = open.remove(player.getUniqueId());
        if (crate == null) {
            return;
        }
        // Zapisujemy, co gracz zostawił, żeby skrzynia nie „odnowiła się” po zamknięciu.
        ItemStack[] snapshot = event.getInventory().getContents();
        crate.holder.contents = snapshot;
        crate.holder.opened = true;
        contents.put(crate.hitbox.getUniqueId(), snapshot);
    }

    private void openCrate(Player player, Interaction hitbox, String poolId, String zoneName) {
        CrateClass crate = pick(poolId);
        if (crate == null) {
            return;
        }
        CrateHolder holder = new CrateHolder(hitbox.getUniqueId(), crate.displayName());
        Inventory inventory = Bukkit.createInventory(holder, SLOTS, crate.displayName());
        holder.inventory = inventory;
        // Loot losujemy tylko raz po odświeżeniu – potem gracz widzi to, co zostawił.
        ItemStack[] stored = contents.get(hitbox.getUniqueId());
        if (stored == null) {
            crate.fill(inventory, random, axonLoot);
            stored = inventory.getContents();
            contents.put(hitbox.getUniqueId(), stored);
        } else {
            inventory.setContents(stored);
        }
        holder.contents = stored;
        holder.opened = true;
        open.put(player.getUniqueId(), new OpenCrate(holder, hitbox, zoneName));
        player.openInventory(inventory);
    }

    /**
     * Czyści zapamiętaną zawartość pojemnika – wywoływane przy odświeżeniu struktury,
     * żeby następne otwarcie wylosowało loot od nowa.
     */
    public void forget(UUID hitboxId) {
        if (hitboxId != null) {
            contents.remove(hitboxId);
        }
    }

    /** Czyści zawartość wszystkich pojemników w podanych współrzędnych (przed ponownym postawieniem). */
    public void forgetAt(World world, int[] w) {
        if (world == null) {
            return;
        }
        Location center = new Location(world, w[0] + 0.5, w[1] + 0.5, w[2] + 0.5);
        for (Entity entity : world.getNearbyEntities(center, 1.2, 1.2, 1.2)) {
            if (entity instanceof Interaction && isModelEntity(entity)) {
                contents.remove(entity.getUniqueId());
            }
        }
    }

    private CrateClass pick(String poolId) {
        if (loot == null || poolId == null) {
            return null;
        }
        CratePool pool = loot.pool(poolId);
        if (pool == null) {
            return null;
        }
        String classId = pool.roll(random);
        return classId == null ? null : loot.crateClass(classId);
    }

    // ------------------------------------------------------------------
    //  Pomocnicze
    // ------------------------------------------------------------------

    private void tag(PersistentDataContainer pdc, String modelName, String poolId, String zoneName) {
        pdc.set(modelKey, PersistentDataType.STRING, modelName == null ? "" : modelName);
        pdc.set(poolKey, PersistentDataType.STRING, poolId == null ? "" : poolId);
        pdc.set(zoneKey, PersistentDataType.STRING, zoneName == null ? "" : zoneName);
    }

    private boolean isModelEntity(Entity entity) {
        if (!(entity instanceof ItemDisplay) && !(entity instanceof Interaction)) {
            return false;
        }
        return entity.getPersistentDataContainer().has(modelKey, PersistentDataType.STRING);
    }

    /** Przedmiot-model z paczki: {@code axon:<nazwa>} przez PDC, żeby nie zależeć od AxonItems. */
    private ItemStack modelItem(String modelName) {
        ItemStack item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(modelKey, PersistentDataType.STRING,
                    modelName == null ? "" : modelName);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Stan jednego otwartego pojemnika. */
    private record OpenCrate(CrateHolder holder, Interaction hitbox, String zoneName) {
    }

    /** Holder wirtualnego ekwipunku – trzyma zawartość między otwarciami. */
    public static final class CrateHolder implements InventoryHolder {

        private final UUID sourceId;
        private final String title;
        private ItemStack[] contents;
        private boolean opened;
        private Inventory inventory;

        CrateHolder(UUID sourceId, String title) {
            this.sourceId = sourceId;
            this.title = title;
        }

        public UUID sourceId() {
            return sourceId;
        }

        public String title() {
            return title;
        }

        public boolean opened() {
            return opened;
        }

        public ItemStack[] contents() {
            return contents;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** Lista wszystkich pojemników-modeli w danym świecie (do diagnostyki). */
    public List<Location> list(World world) {
        List<Location> result = new ArrayList<>();
        if (world == null) {
            return result;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ItemDisplay && isModelEntity(entity)) {
                result.add(entity.getLocation());
            }
        }
        return result;
    }
}
