package pl.axon.structure.api;

import org.junit.jupiter.api.Test;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Traits;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureTemplateTest {

    private static final String[] STATES = {
            "minecraft:stone", "minecraft:oak_planks", "minecraft:oak_stairs[facing=north,half=top]",
            "minecraft:glass_pane[north=true,east=false,south=true,west=false]"
    };

    private static Blueprint randomBlueprint(long seed, int w, int h, int d) {
        Random random = new Random(seed);
        Blueprint bp = new Blueprint(w, h, d).meta("losowa", "Losowa", "");
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    int roll = random.nextInt(STATES.length + 2);
                    // dwa ostatnie indeksy to powietrze, żeby w szablonie było sporo pustych miejsc
                    bp.set(x, y, z, roll < STATES.length ? STATES[roll] : "minecraft:air");
                }
            }
        }
        return bp;
    }

    @Test
    void blockAtZgadzaSieZBlueprintEwszedzie() {
        Blueprint bp = randomBlueprint(7, 6, 4, 9);
        StructureTemplate template = StructureTemplate.snapshot(bp);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 9; z++) {
                    String expected = bp.get(x, y, z);
                    String actual = template.blockAt(x, y, z);
                    if (expected.equals(Blueprint.AIR)) {
                        assertNull(actual, "powietrze w " + x + " " + y + " " + z);
                    } else {
                        assertEquals(expected, actual, "blok w " + x + " " + y + " " + z);
                    }
                }
            }
        }
    }

    @Test
    void pozaSzablonemJestNull() {
        StructureTemplate template = StructureTemplate.snapshot(randomBlueprint(1, 3, 3, 3));
        assertNull(template.blockAt(-1, 0, 0));
        assertNull(template.blockAt(3, 0, 0));
        assertNull(template.blockAt(0, 3, 0));
        assertNull(template.blockAt(0, 0, 3));
    }

    @Test
    void kopiaNieZmieniaSieGdyBlueprintSieZmienia() {
        Blueprint bp = new Blueprint(2, 2, 2).meta("a", "A", "");
        bp.set(0, 0, 0, "minecraft:stone");
        StructureTemplate template = StructureTemplate.snapshot(bp);
        bp.set(1, 1, 1, "minecraft:dirt");
        bp.set(0, 0, 0, "minecraft:glass");
        assertNull(template.blockAt(1, 1, 1));
        assertEquals("minecraft:stone", template.blockAt(0, 0, 0));
    }

    @Test
    void klasaRozmiaruLiczonaZWymiarow() {
        assertEquals(SizeClass.DUZA, StructureTemplate.snapshot(new Blueprint(100, 5, 10)).sizeClass());
        assertEquals(SizeClass.SREDNIA, StructureTemplate.snapshot(new Blueprint(60, 5, 10)).sizeClass());
        assertEquals(SizeClass.MALA, StructureTemplate.snapshot(new Blueprint(10, 5, 10)).sizeClass());
    }

    @Test
    void jawnaKlasaRozmiaruMaPierwszenstwo() {
        Blueprint bp = new Blueprint(100, 5, 100).traits(Traits.defaults().withMapAllowed(true));
        Traits explicit = new Traits(0, List.of(), true, SizeClass.MALA, Traits.defaults().zones(),
                Traits.defaults().climates(), false, Map.of());
        assertEquals(SizeClass.MALA, StructureTemplate.snapshot(bp.traits(explicit)).sizeClass());
    }

    @Test
    void polaNaglowkaSaPrzekazywaneDalej() {
        Blueprint bp = new Blueprint(20, 5, 20).traits(new Traits(21, List.of(new Entrance(0, 5, "west")),
                false, null, Traits.defaults().zones(), Traits.defaults().climates(), true, Map.of("autor", "x")));
        StructureTemplate template = StructureTemplate.snapshot(bp);
        assertEquals(21, template.groundLevel());
        assertEquals(List.of(new Entrance(0, 5, "west")), template.entrances());
        assertFalse(template.mapAllowed());
        assertTrue(template.safe());
        assertEquals("x", template.tags().get("autor"));
    }

    @Test
    void tagiSaNiemutowalne() {
        StructureTemplate template = StructureTemplate.snapshot(new Blueprint(2, 2, 2));
        assertThrows(UnsupportedOperationException.class, () -> template.tags().put("a", "b"));
        assertThrows(UnsupportedOperationException.class, () -> template.zones().add("las"));
    }
}
