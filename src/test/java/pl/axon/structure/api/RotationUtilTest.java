package pl.axon.structure.api;

import org.junit.jupiter.api.Test;
import pl.axon.structure.core.Placement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationUtilTest {

    private static final List<String> STATES = List.of(
            "minecraft:stone",
            "minecraft:oak_stairs[facing=east,half=bottom,shape=straight]",
            "minecraft:oak_log[axis=x]",
            "minecraft:oak_log[axis=y]",
            "minecraft:oak_wall_sign[facing=north]",
            "minecraft:oak_sign[rotation=8]",
            "minecraft:oak_fence[north=true,east=false,south=true,west=false,waterlogged=false]",
            "minecraft:glass_pane[north=true,east=true,south=false,west=false]",
            "minecraft:iron_bars[north=false,east=true,south=false,west=true]",
            "minecraft:cobblestone_wall[north=low,east=none,south=low,west=none,up=true]",
            "minecraft:chest[facing=south,type=single]",
            "minecraft:spruce_door[facing=north,half=lower,hinge=left,open=true,powered=false]");

    @Test
    void czteryObrotyWektoraSaTozsamoscia() {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int[] v = {x, z};
                for (int i = 0; i < 4; i++) {
                    v = RotationUtil.rotate(v[0], v[1], 1);
                }
                assertArrayEquals(new int[]{x, z}, v, "wektor " + x + "," + z);
            }
        }
    }

    @Test
    void czteryObrotyStanuBlokuSaTozsamoscia() {
        for (String state : STATES) {
            String rotated = state;
            for (int i = 0; i < 4; i++) {
                rotated = RotationUtil.rotateState(rotated, 1);
            }
            assertEquals(state, rotated);
        }
    }

    @Test
    void obrotJedenKrokZgodnyZRuchemWskazowek() {
        assertEquals("south", RotationUtil.rotateFacing("east", 1));
        assertEquals("north", RotationUtil.rotateFacing("west", 1));
        assertEquals("minecraft:oak_stairs[facing=south,half=bottom,shape=straight]",
                RotationUtil.rotateState("minecraft:oak_stairs[facing=east,half=bottom,shape=straight]", 1));
    }

    @Test
    void osieIPlotyObracaSieJakNaPowierzchni() {
        assertEquals("minecraft:oak_log[axis=z]", RotationUtil.rotateState("minecraft:oak_log[axis=x]", 1));
        assertEquals("minecraft:oak_log[axis=x]", RotationUtil.rotateState("minecraft:oak_log[axis=x]", 2));
        assertEquals("minecraft:oak_fence[east=true,south=false,west=false,north=false]",
                RotationUtil.rotateState("minecraft:oak_fence[north=true,east=false,south=false,west=false]", 1));
    }

    @Test
    void tablicaObracaSieOJedenKrok() {
        assertEquals("minecraft:oak_sign[rotation=12]", RotationUtil.rotateState("minecraft:oak_sign[rotation=8]", 1));
        assertEquals("minecraft:oak_sign[rotation=8]", RotationUtil.rotateState("minecraft:oak_sign[rotation=8]", 4));
    }

    @Test
    void liczbaObrotowJestSprowadzanaModulo4() {
        assertEquals(3, RotationUtil.normalize(-1));
        assertEquals(0, RotationUtil.normalize(4));
        assertEquals("south", RotationUtil.rotateFacing("east", 5));
        assertEquals("north", RotationUtil.rotateFacing("east", -1));
    }

    @Test
    void zgodnoscZWklejaniem() {
        // Ten sam Placement, którego używa PasteService, daje te same współrzędne światowe.
        for (int steps = -4; steps <= 7; steps++) {
            Placement placement = new Placement("world", 100, 64, -50, RotationUtil.normalize(steps));
            for (int lx = 0; lx < 5; lx++) {
                for (int lz = 0; lz < 5; lz++) {
                    assertArrayEquals(placement.toWorld(lx, 2, lz),
                            RotationUtil.toWorld(100, 64, -50, steps, lx, 2, lz),
                            "steps " + steps + " lx " + lx + " lz " + lz);
                }
            }
        }
    }

    @Test
    void jedenObrotPrzesuwaLokalnyPunktZgodnieZPlacement() {
        // Lokalny (1, 0, 0) po jednym obrocie leży na z = +1 od punktu zaczepienia.
        assertArrayEquals(new int[]{10, 65, 21}, RotationUtil.toWorld(10, 64, 20, 1, 1, 1, 0));
    }
}
