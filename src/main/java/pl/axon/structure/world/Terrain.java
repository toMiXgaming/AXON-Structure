package pl.axon.structure.world;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;

/** Odczyt naturalnego terenu: wysokość gruntu z pominięciem drzew i roślin, woda, materiał wierzchni. */
public final class Terrain {

    public record Column(int surfaceY, boolean liquid, Material top, Material under) {
    }

    private Terrain() {
    }

    public static Column scan(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        int min = world.getMinHeight();
        while (y > min) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (isLiquid(type)) {
                return new Column(y, true, Material.DIRT, Material.DIRT);
            }
            if (isGround(type)) {
                Material below = world.getBlockAt(x, y - 1, z).getType();
                return new Column(y, false, topMaterial(type), underMaterial(type, below));
            }
            y--;
        }
        return new Column(min, false, Material.STONE, Material.STONE);
    }

    public static boolean isLiquid(Material type) {
        return switch (type) {
            case WATER, LAVA, BUBBLE_COLUMN, KELP, KELP_PLANT, SEAGRASS, TALL_SEAGRASS, ICE, PACKED_ICE, BLUE_ICE -> true;
            default -> false;
        };
    }

    /** Bloki, które uznajemy za grunt (nie drzewa, nie rośliny, nie śnieg). */
    public static boolean isGround(Material type) {
        return type.isSolid() && !isVegetation(type);
    }

    public static boolean isVegetation(Material type) {
        String name = type.name();
        return name.endsWith("_LOG") || name.endsWith("_LEAVES") || name.endsWith("_WOOD")
                || name.endsWith("_STEM") || name.endsWith("_HYPHAE") || name.contains("MUSHROOM_BLOCK")
                || name.startsWith("BAMBOO") || name.equals("CACTUS") || name.equals("PUMPKIN")
                || name.equals("MELON") || name.equals("BEE_NEST") || name.equals("MOSS_CARPET")
                || name.equals("MANGROVE_ROOTS") || name.equals("MUDDY_MANGROVE_ROOTS");
    }

    private static Material topMaterial(Material surface) {
        return switch (surface) {
            case GRASS_BLOCK, PODZOL, MYCELIUM, SAND, RED_SAND, SNOW_BLOCK, COARSE_DIRT, DIRT, GRAVEL, MUD,
                 MOSS_BLOCK, STONE, DEEPSLATE, TERRACOTTA, CLAY, ROOTED_DIRT, SANDSTONE, RED_SANDSTONE -> surface;
            default -> name(surface).contains("TERRACOTTA") ? surface : Material.GRASS_BLOCK;
        };
    }

    private static Material underMaterial(Material surface, Material below) {
        return switch (surface) {
            case SAND -> Material.SANDSTONE;
            case RED_SAND -> Material.RED_SANDSTONE;
            case GRASS_BLOCK, PODZOL, MYCELIUM, DIRT_PATH, MOSS_BLOCK, SNOW_BLOCK -> Material.DIRT;
            default -> isGround(below) && !below.name().contains("ORE") ? below : Material.DIRT;
        };
    }

    private static String name(Material m) {
        return m.name();
    }
}
