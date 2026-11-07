package pl.axon.structure.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Testy HP beczek: obrażenia przedmiotów, naładowanie zamachu i pasek. */
class BarrelMathTest {

    private static final BarrelMath.Damage D = BarrelMath.Damage.DEFAULT;

    @Test
    void rekaIInnePrzedmioty() {
        assertEquals(3, D.base(null));
        assertEquals(3, D.base("AIR"));
        assertEquals(4, D.base("COBBLESTONE"));
        assertEquals(4, D.base("CROSSBOW"));
    }

    @Test
    void rodzajRazyMaterial() {
        assertEquals(8, D.base("WOODEN_AXE"), 1e-9);
        assertEquals(12, D.base("IRON_AXE"), 1e-9);
        assertEquals(16, D.base("NETHERITE_AXE"), 1e-9);
        assertEquals(7 * 1.75, D.base("DIAMOND_SWORD"), 1e-9);
        // pickaxe nie łapie się jako axe
        assertEquals(6 * 1.5, D.base("IRON_PICKAXE"), 1e-9);
        assertEquals(12, D.base("MACE"), 1e-9);
        assertEquals(8, D.base("TRIDENT"), 1e-9);
    }

    @Test
    void dokladnyPrzedmiotNadpisuje() {
        BarrelMath.Damage d = new BarrelMath.Damage(3, 4, D.types(), D.tiers(), Map.of("iron_axe", 50.0, "stick", 5.0),
                0.15);
        assertEquals(50, d.base("IRON_AXE"));
        assertEquals(5, d.base("STICK"));
    }

    @Test
    void niedoladowanyZamachZadajeMniej() {
        assertEquals(10, BarrelMath.damage(10, 0, 0.15, 1f), 1e-9);
        assertEquals(2, BarrelMath.damage(10, 0, 0.15, 0f), 1e-9);
        assertTrue(BarrelMath.damage(10, 0, 0.15, 0.5f) < 5);
        assertEquals(0, BarrelMath.damage(0, 5, 0.15, 1f));
    }

    @Test
    void zaczarowanieDodajeProcent() {
        assertEquals(10 * 1.75, BarrelMath.damage(10, 5, 0.15, 1f), 1e-9);
    }

    @Test
    void liczbaUderzenDlaDomyslnejBeczki() {
        assertEquals(8, BarrelMath.hitsLeft(24, D.base(null)));
        assertEquals(3, BarrelMath.hitsLeft(24, D.base("WOODEN_AXE")));
        assertEquals(2, BarrelMath.hitsLeft(24, D.base("IRON_AXE")));
        assertEquals(0, BarrelMath.hitsLeft(24, 0));
    }

    @Test
    void procentIPasek() {
        assertEquals(100, BarrelMath.percent(24, 24));
        assertEquals(1, BarrelMath.percent(0.01, 24));
        assertEquals(0, BarrelMath.percent(0, 24));
        assertEquals("|||||.....", BarrelMath.bar(12, 24, 10, "|", "."));
        assertEquals("|.........", BarrelMath.bar(0.1, 24, 10, "|", "."));
        assertEquals("..........", BarrelMath.bar(0, 24, 10, "|", "."));
        assertEquals("§a", BarrelMath.color(80));
        assertEquals("§e", BarrelMath.color(50));
        assertEquals("§c", BarrelMath.color(10));
    }
}
