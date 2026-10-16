package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Wspólne „postarzanie” każdej struktury: omszałe i popękane cegły, mech na ziemi przy ścianach i na dachach,
 * porosty na ścianach. Zmienia tylko wygląd: bloki zamieniane są na odpowiedniki o tym samym kształcie,
 * a nowe ozdoby (dywan z mchu, porosty) stawiane są w pustych miejscach i nie blokują przejścia
 * ani wspinaczki.
 */
final class Weathering {

    private static final String[] FACES = {"north", "south", "east", "west"};

    private Weathering() {
    }

    static void apply(Blueprint bp, Random rnd) {
        swapMaterials(bp, rnd);
        decorate(bp, rnd);
    }

    private static void swapMaterials(Blueprint bp, Random rnd) {
        List<Map.Entry<Blueprint.Pos, String>> entries = new ArrayList<>(bp.blocks().entrySet());
        for (Map.Entry<Blueprint.Pos, String> e : entries) {
            String state = e.getValue();
            String name = state.contains("[") ? state.substring(0, state.indexOf('[')) : state;
            String props = state.substring(name.length());
            String swapped = switch (name) {
                case "minecraft:stone_bricks" -> pick(rnd, 0.09, "minecraft:mossy_stone_bricks", 0.09, "minecraft:cracked_stone_bricks");
                case "minecraft:cobblestone" -> pick(rnd, 0.2, "minecraft:mossy_cobblestone", 0, null);
                case "minecraft:stone_brick_stairs" -> pick(rnd, 0.12, "minecraft:mossy_stone_brick_stairs", 0, null);
                case "minecraft:stone_brick_slab" -> pick(rnd, 0.12, "minecraft:mossy_stone_brick_slab", 0, null);
                case "minecraft:stone_brick_wall" -> pick(rnd, 0.15, "minecraft:mossy_stone_brick_wall", 0, null);
                case "minecraft:cobblestone_wall" -> pick(rnd, 0.2, "minecraft:mossy_cobblestone_wall", 0, null);
                case "minecraft:polished_blackstone_bricks" -> pick(rnd, 0.15, "minecraft:cracked_polished_blackstone_bricks", 0, null);
                case "minecraft:deepslate_bricks" -> pick(rnd, 0.15, "minecraft:cracked_deepslate_bricks", 0, null);
                case "minecraft:deepslate_tiles" -> pick(rnd, 0.15, "minecraft:cracked_deepslate_tiles", 0, null);
                default -> null;
            };
            if (swapped != null) {
                bp.set(e.getKey().x(), e.getKey().y(), e.getKey().z(), swapped + props);
            }
        }
    }

    private static String pick(Random rnd, double p1, String s1, double p2, String s2) {
        double r = rnd.nextDouble();
        if (r < p1) {
            return s1;
        }
        if (s2 != null && r < p1 + p2) {
            return s2;
        }
        return null;
    }

    private static void decorate(Blueprint bp, Random rnd) {
        Set<Blueprint.Pos> reserved = new HashSet<>();
        for (Blueprint.SpawnPoint p : bp.spawnPoints()) {
            reserved.add(p.pos());
            reserved.add(new Blueprint.Pos(p.pos().x(), p.pos().y() + 1, p.pos().z()));
        }
        int ground = bp.ground();
        int[] top = new int[bp.width() * bp.depth()];
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.depth(); z++) {
                int t = -1;
                for (int y = bp.height() - 1; y >= 0; y--) {
                    if (!bp.isAir(x, y, z)) {
                        t = y;
                        break;
                    }
                }
                top[x * bp.depth() + z] = t;
            }
        }
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.depth(); z++) {
                for (int y = 1; y < bp.height(); y++) {
                    if (!bp.isAir(x, y, z) || reserved.contains(new Blueprint.Pos(x, y, z))) {
                        continue;
                    }
                    String below = bp.get(x, y - 1, z);
                    boolean open = y > top[x * bp.depth() + z];
                    int wall = wallSide(bp, x, y, z);
                    if (open && isFullBlock(below)) {
                        // mech na ziemi przy ścianach i plamy mchu na dachach
                        double p = y == ground + 1 ? (wall >= 0 ? 0.22 : 0.0) : (y > ground + 3 ? 0.035 : 0.0);
                        if (rnd.nextDouble() < p) {
                            bp.set(x, y, z, "moss_carpet");
                            continue;
                        }
                    }
                    if (wall >= 0 && y >= ground + 1 && rnd.nextDouble() < (open ? 0.012 : 0.008)) {
                        bp.set(x, y, z, lichen(FACES[wall]));
                    }
                }
            }
        }
    }

    /** Indeks strony (N, S, E, W), po której stoi pełny blok ściany, albo -1. */
    private static int wallSide(Blueprint bp, int x, int y, int z) {
        int[][] d = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            int nx = x + d[i][0];
            int nz = z + d[i][1];
            if (bp.inBounds(nx, y, nz) && isWallBlock(bp.get(nx, y, nz))) {
                return i;
            }
        }
        return -1;
    }

    private static String lichen(String face) {
        StringBuilder sb = new StringBuilder("glow_lichen[down=false");
        for (String f : new String[]{"east", "north", "south"}) {
            sb.append(',').append(f).append('=').append(f.equals(face));
        }
        sb.append(",up=false,waterlogged=false,west=").append("west".equals(face)).append(']');
        return sb.toString();
    }

    private static boolean isFullBlock(String state) {
        String n = state.replace("minecraft:", "");
        if (n.equals("air") || n.contains("[") && !n.contains("axis=")) {
            return false;
        }
        return !(n.contains("slab") || n.contains("stairs") || n.contains("fence") || n.contains("wall") || n.contains("pane")
                || n.contains("bars") || n.contains("carpet") || n.contains("ladder") || n.contains("door") || n.contains("water")
                || n.contains("lava") || n.contains("leaves") || n.contains("glass") || n.contains("grass") && !n.equals("grass_block")
                || n.contains("fern") || n.contains("cobweb") || n.contains("chain") || n.contains("lantern") || n.contains("rail")
                || n.contains("sign") || n.contains("pot") || n.contains("bush") || n.contains("candle") || n.contains("rod")
                || n.contains("cauldron") || n.contains("hopper") || n.contains("bed") || n.contains("chest") || n.contains("lichen")
                || n.contains("dirt_path") || n.contains("farmland") || n.contains("soul_sand") || n.contains("mud") && !n.contains("brick")
                || n.contains("composter") || n.contains("anvil") || n.contains("stand") || n.contains("lectern") || n.contains("skull")
                || n.contains("head") || n.contains("snow") || n.contains("torch") || n.contains("button") || n.contains("lever")
                || n.contains("plate") || n.contains("hook") || n.contains("wire") || n.contains("vine") || n.contains("roots")
                || n.contains("flower") || n.contains("sapling") || n.contains("campfire") || n.contains("grindstone") || n.contains("bell")
                || n.contains("scaffolding") || n.contains("ice") || n.contains("honey") || n.contains("slime") || n.contains("cactus"));
    }

    private static boolean isWallBlock(String state) {
        String n = state.replace("minecraft:", "");
        if (!isFullBlock(state)) {
            return false;
        }
        return !(n.startsWith("grass_block") || n.equals("dirt") || n.equals("coarse_dirt") || n.equals("podzol") || n.contains("ore")
                || n.equals("gravel") || n.equals("sand") || n.contains("log") || n.contains("wood") || n.contains("planks")
                || n.contains("wool") || n.contains("hay") || n.contains("barrel") || n.contains("crafting") || n.contains("furnace")
                || n.contains("smoker") || n.contains("bookshelf") || n.contains("observer") || n.contains("target")
                || n.contains("note_block") || n.contains("lamp") || n.contains("lantern") || n.contains("iron_block")
                || n.contains("gold") || n.contains("copper") || n.contains("melon") || n.contains("pumpkin")
                || n.contains("tnt") || n.contains("coal_block") || n.contains("moss_block") || n.contains("water"));
    }
}
