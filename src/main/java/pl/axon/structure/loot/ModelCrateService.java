package pl.axon.structure.loot;

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

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey modelKey;
    private final NamespacedKey poolKey;
    private final NamespacedKey zoneKey;

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
    }

    public void configure(LootConfig loot, AxonLootBridge axonLoot) {
        this.loot = loot;
        this.axonLoot = axonLoot;
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
        if (world == null || !world.isChunkLoaded(w[0] >> 4, w[2] >> 4)) {
            return false;
        }
        remove(world, w);
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
            });
            display.addPassenger(hitbox);
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
