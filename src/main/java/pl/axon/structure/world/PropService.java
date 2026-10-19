package pl.axon.structure.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.Placement;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Modele z paczki zasobów serwera stawiane w strukturach jako wyświetlacze przedmiotów: wpisy z sekcji
 * {@code modele:} pliku struktury (np. wrak śmigłowca) i meble zamieniane z bloków (łóżka, regały, piece...). Kolizję dają
 * niewidzialne bloki bariery z pliku struktury, model tylko rysuje.
 */
public final class PropService implements Listener {

    /**
     * Model łóżka, modele innych mebli według bloku (blok zamienia się na barierę), modele drzwi (drzwi
     * zostają i dalej się otwierają, model nad nimi) i struktury, w których meble zostają zwykłe.
     */
    public record Settings(String bedModel, Map<Material, String> blockModels, Map<Material, String> doorModels,
                           Set<String> skipStructures) {
    }

    /**
     * Model w świecie: środek modelu (punkt 8 8 8), skala wyświetlacza i obrót w stopniach. Dla drzwi
     * {@code center} to róg dolnego bloku drzwi, a położenie liczy się z kształtu drzwi.
     */
    public record WorldProp(Location center, String model, float scale, float yaw, boolean door) {
    }

    private static final String TAG = "axs_prop";
    private static final String DOOR_TAG = "axs_door";
    private static final BlockData BARRIER = Material.BARRIER.createBlockData();

    private final JavaPlugin plugin;
    private Settings settings = new Settings("", Map.of(), Map.of(), Set.of());

    public PropService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public boolean replacesFurniture(Blueprint bp) {
        return (!settings.bedModel().isBlank() || !settings.blockModels().isEmpty() || !settings.doorModels().isEmpty())
                && !settings.skipStructures().contains(bp.id().toLowerCase(Locale.ROOT));
    }

    /**
     * Zamienia mebel na barierę i dopisuje jego model do {@code props}. Zwraca blok do postawienia
     * (ten sam, gdy to nie mebel).
     */
    public BlockData furniture(World world, int x, int y, int z, BlockData data, List<WorldProp> props) {
        if (data instanceof Door door) {
            String doorModel = settings.doorModels().get(data.getMaterial());
            if (doorModel != null && door.getHalf() == Bisected.Half.BOTTOM) {
                props.add(new WorldProp(new Location(world, x, y, z), doorModel, 1f, 0f, true));
            }
            return data;
        }
        String model = settings.blockModels().get(data.getMaterial());
        if (model != null) {
            BlockFace facing = data instanceof Directional directional ? directional.getFacing() : BlockFace.SOUTH;
            props.add(new WorldProp(new Location(world, x + 0.5, y + 0.5, z + 0.5), model, 1f, yawOf(facing), false));
            return BARRIER;
        }
        if (!(data instanceof Bed bed) || settings.bedModel().isBlank()) {
            return data;
        }
        if (bed.getPart() == Bed.Part.HEAD) {
            BlockFace facing = bed.getFacing();
            Location center = new Location(world, x + 0.5 - facing.getModX() * 0.5, y + 0.5,
                    z + 0.5 - facing.getModZ() * 0.5);
            props.add(new WorldProp(center, settings.bedModel(), 1f, yawOf(facing), false));
        }
        return BARRIER;
    }

    /** Modele z pliku struktury przeliczone na świat. */
    public void fromBlueprint(World world, Blueprint bp, Placement placement, List<WorldProp> props) {
        int steps = placement.steps();
        for (Blueprint.PropSpec spec : bp.props()) {
            // obrót punktu ciągłego tak samo, jak obracane są bloki (wokół środka bloku 0 0)
            double rx = spec.x() - 0.5;
            double rz = spec.z() - 0.5;
            for (int i = 0; i < steps; i++) {
                double tmp = rx;
                rx = -rz;
                rz = tmp;
            }
            Location center = new Location(world, placement.originX() + rx + 0.5, placement.originY() + spec.y(),
                    placement.originZ() + rz + 0.5);
            props.add(new WorldProp(center, spec.model(), spec.scale(), spec.yaw() + 90f * steps, false));
        }
    }

    public void spawn(List<WorldProp> props) {
        for (WorldProp prop : props) {
            NamespacedKey key = NamespacedKey.fromString(prop.model());
            if (key == null) {
                plugin.getLogger().warning("Zły model: " + prop.model());
                continue;
            }
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setItemModel(key);
            item.setItemMeta(meta);
            Location location = prop.door() ? doorPose(prop.center().getBlock()) : prop.center().clone();
            // Wyświetlacz przedmiotu sam obraca model o 180°, obrót o 180° to znosi.
            location.setYaw((prop.door() ? location.getYaw() : prop.yaw()) + 180f);
            location.setPitch(0f);
            float size = 3f * prop.scale();
            prop.center().getWorld().spawn(location, ItemDisplay.class, display -> {
                display.setItemStack(item);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                        new Vector3f(prop.scale(), prop.scale(), prop.scale()), new Quaternionf()));
                display.setDisplayWidth(size);
                display.setDisplayHeight(size);
                display.setViewRange(Math.max(1f, size / 8f));
                display.addScoreboardTag(TAG);
                if (prop.door()) {
                    display.addScoreboardTag(DOOR_TAG);
                }
                display.setPersistent(true);
            });
        }
    }

    /**
     * Środek płyty drzwi i obrót (0 = płyta wzdłuż x, 90 = wzdłuż z) z kształtu kolizji dolnego bloku,
     * więc model idzie za drzwiami przy otwieraniu, niezależnie od kierunku i zawiasów.
     */
    private static Location doorPose(Block lower) {
        Location location = lower.getLocation().add(0.5, 0.5, 0.5);
        Collection<BoundingBox> boxes = lower.getCollisionShape().getBoundingBoxes();
        if (!boxes.isEmpty()) {
            BoundingBox box = boxes.iterator().next();
            location.setX(lower.getX() + box.getCenterX());
            location.setZ(lower.getZ() + box.getCenterZ());
            location.setYaw(box.getWidthX() >= box.getWidthZ() ? 0f : 90f);
        }
        return location;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            syncDoorLater(event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstone(BlockRedstoneEvent event) {
        syncDoorLater(event.getBlock());
    }

    /** Po przełączeniu drzwi (w następnym ticku) przestawia ich model. */
    private void syncDoorLater(Block block) {
        if (!(block.getBlockData() instanceof Door door) || !settings.doorModels().containsKey(block.getType())) {
            return;
        }
        Block lower = door.getHalf() == Bisected.Half.TOP ? block.getRelative(BlockFace.DOWN) : block;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(lower.getBlockData() instanceof Door)) {
                return;
            }
            BoundingBox cell = BoundingBox.of(lower).expand(0.05);
            for (Entity entity : lower.getWorld().getNearbyEntities(cell, e -> e.getScoreboardTags().contains(DOOR_TAG))) {
                Location pose = doorPose(lower);
                pose.setYaw(pose.getYaw() + 180f);
                entity.teleport(pose);
            }
        });
    }

    /** Usuwa modele stojące w obrysie struktury (przy cofaniu). */
    public void remove(World world, Box box, int minY, int maxY) {
        BoundingBox area = new BoundingBox(box.minX(), minY, box.minZ(), box.maxX() + 1, maxY + 1, box.maxZ() + 1);
        for (ItemDisplay display : world.getNearbyEntities(area, e -> e instanceof ItemDisplay)
                .stream().map(ItemDisplay.class::cast).toList()) {
            if (display.getScoreboardTags().contains(TAG)) {
                display.remove();
            }
        }
    }

    /** Yaw modelu, którego naturalny przód (wezgłowie łóżka, front mebla) patrzy na południe. */
    private static float yawOf(BlockFace facing) {
        return switch (facing) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }
}
