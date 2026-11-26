package pl.axon.structure.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/** Tiery struktur: wczytywanie z configu, ulepszenia klas i mnożnik ilości – bez serwera. */
class TierConfigTest {

    private static final Logger LOG = Logger.getLogger("test");

    private static CrateClass crate(String id, CrateClass.Kind kind) {
        CrateClass.ContainerSpec spec = kind == CrateClass.Kind.MODEL
                ? new CrateClass.ContainerSpec("", 30, -1, "", 1, 0.9f, 1f, 0) : null;
        return new CrateClass(id, id, kind, "", kind == CrateClass.Kind.MODEL ? "axon:x" : "", 1, List.of(), spec);
    }

    private static Map<String, CrateClass> classes() {
        Map<String, CrateClass> map = new LinkedHashMap<>();
        for (String id : List.of("mala", "lepsza", "wojskowa")) {
            map.put(id, crate(id, CrateClass.Kind.MODEL));
        }
        map.put("brazowa", crate("brazowa", CrateClass.Kind.CHEST));
        map.put("karta", crate("karta", CrateClass.Kind.FRAME));
        return map;
    }

    private static TierConfig load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return TierConfig.load(config.getConfigurationSection("tiery"), classes(), LOG);
    }

    private static final String YAML = """
            tiery:
              wlaczone: true
              domyslny: 1
              przy-drogach: 2
              struktury:
                galeria: 3
                oaza: 0
                Kombinat: 2
              poziomy:
                1: {nazwa: "&aTier I", ilosc: 1.0}
                2:
                  ilosc: 1.15
                  ulepszenia:
                    mala: {lepsza: 20}
                3:
                  nazwa: "&cTier III"
                  ilosc: 1.3
                  ulepszenia:
                    mala: {lepsza: 35}
                    lepsza: {wojskowa: 80, mala: 60}
                    brazowa: {wojskowa: 10}
                    mala-nieznana: {lepsza: 10}
                    karta: {lepsza: 50}
                    wojskowa: {brazowa: 50}
            """;

    @Test
    void strukturyDostajaPoziomyZConfigu() throws Exception {
        TierConfig tiers = load(YAML);
        assertTrue(tiers.enabled());
        assertEquals(3, tiers.forStructure("galeria").level());
        assertEquals(3, tiers.forStructure("GALERIA").level());
        assertEquals(2, tiers.forStructure("kombinat").level());
        assertEquals(1, tiers.forStructure("osiedle").level(), "bez wpisu: domyslny");
        assertSame(TierConfig.Tier.NONE, tiers.forStructure("oaza"), "0 = bez tieru");
        assertEquals(2, tiers.roadside().level());
        assertEquals("Tier 2", tiers.level(2).label(), "bez nazwy: Tier N");
        assertSame(TierConfig.Tier.NONE, tiers.level(9), "nieznany poziom = bez tieru");
    }

    @Test
    void wylaczoneTieryToWszedzieBezTieru() throws Exception {
        TierConfig tiers = load("tiery:\n  wlaczone: false\n  struktury: {galeria: 3}\n");
        assertFalse(tiers.enabled());
        assertSame(TierConfig.Tier.NONE, tiers.forStructure("galeria"));
        assertSame(TierConfig.Tier.NONE, TierConfig.load(null, classes(), LOG).forStructure("galeria"));
    }

    @Test
    void ulepszeniaOdrzucajaNieznaneINiezgodneKlasyISkalujaSumePonad100() throws Exception {
        TierConfig.Tier tier = load(YAML).level(3);
        Map<String, LinkedHashMap<String, Double>> up = tier.upgrades();
        assertFalse(up.containsKey("mala-nieznana"));
        assertFalse(up.containsKey("karta"), "ramka nie zamienia się w model");
        assertFalse(up.containsKey("wojskowa"), "model nie zamienia się w skrzynię-blok");
        assertEquals(10.0, up.get("brazowa").get("wojskowa"), 1e-9, "skrzynia-blok może stać się modelem");
        assertEquals(100.0 * 80 / 140, up.get("lepsza").get("wojskowa"), 1e-9, "80+60 przeskalowane do 100");
        assertEquals(100.0 * 60 / 140, up.get("lepsza").get("mala"), 1e-9);
    }

    @Test
    void ulepszenieWypadaZSzansaZConfigu() throws Exception {
        TierConfig.Tier tier = load(YAML).level(3);
        Random random = new Random(42);
        int upgraded = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            if (tier.upgrade("mala", random).equals("lepsza")) {
                upgraded++;
            }
        }
        assertEquals(0.35, upgraded / (double) n, 0.005);
        assertEquals("brak-w-ulepszeniach", tier.upgrade("brak-w-ulepszeniach", random));
        assertEquals(null, tier.upgrade(null, random), "„brak” z puli zostaje pustym miejscem");
    }

    @Test
    void mnoznikIlosciJestSrednioDokladnyIOmijaPojedynczePrzedmioty() throws Exception {
        TierConfig.Tier tier = load(YAML).level(3);
        Random random = new Random(7);
        long sum = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int scaled = tier.scale(5, 64, random);
            assertTrue(scaled == 6 || scaled == 7, "1.3 × 5 = 6,5");
            sum += scaled;
        }
        assertEquals(6.5, sum / (double) n, 0.01);
        assertEquals(1, tier.scale(1, 64, random), "1 HQM, korpus – bez mnożenia");
        assertEquals(2, tier.scale(2, 1, random), "niestackowalne bez zmian");
        assertEquals(64, tier.scale(60, 64, random), "nie więcej niż maksymalny stos");
        assertEquals(5, TierConfig.Tier.NONE.scale(5, 64, random));
    }

    @Test
    void lootStraznikowBezDodatkow() {
        CrateClass.LootEntry hqm = new CrateClass.LootEntry(null, "hqm", 1, 1, 3, null, List.of(), null, 7);
        TierConfig.Tier tier = new TierConfig.Tier(3, "", 1.3, Map.of(), List.of(hqm));
        assertEquals(1, tier.extras().size());
        assertTrue(tier.withoutExtras().extras().isEmpty());
        assertEquals(1.3, tier.withoutExtras().amount(), 1e-9);
    }

    @Test
    void zgodnoscRodzajowKlas() {
        assertTrue(TierConfig.compatible(CrateClass.Kind.CHEST, CrateClass.Kind.MODEL));
        assertTrue(TierConfig.compatible(CrateClass.Kind.BARREL, CrateClass.Kind.CHEST));
        assertTrue(TierConfig.compatible(CrateClass.Kind.MODEL, CrateClass.Kind.MODEL));
        assertFalse(TierConfig.compatible(CrateClass.Kind.MODEL, CrateClass.Kind.CHEST));
        assertFalse(TierConfig.compatible(CrateClass.Kind.FRAME, CrateClass.Kind.MODEL));
        assertFalse(TierConfig.compatible(CrateClass.Kind.CHEST, CrateClass.Kind.FRAME));
    }

    @Test
    void domyslnyConfigZJaraMaPoprawneTiery() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (var in = getClass().getResourceAsStream("/config.yml")) {
            config.loadFromString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        java.util.List<String> warnings = new java.util.ArrayList<>();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        LootConfig loot = LootConfig.load(config, logger);
        TierConfig tiers = loot.tiers();
        assertTrue(warnings.isEmpty(), () -> "ostrzeżenia przy wczytaniu: " + warnings);
        assertEquals(3, tiers.levels().size());
        assertEquals(3, tiers.forStructure("jednostka").level());
        assertSame(TierConfig.Tier.NONE, tiers.forStructure("oaza"));
        TierConfig.Tier top = tiers.level(3);
        assertTrue(top.upgrades().get("rzadka-skrzynia").containsKey("skrzynia-wojskowa"));
        assertTrue(top.extras().stream().anyMatch(e -> "body_rifle".equals(e.axonId()) && e.fromDay() == 10));
        assertTrue(top.extras().stream().anyMatch(e -> "hqm".equals(e.axonId()) && e.fromDay() == 7));
        for (TierConfig.Tier tier : tiers.levels().values()) {
            tier.upgrades().forEach((from, to) -> to.keySet().forEach(target ->
                    assertTrue(loot.classes().containsKey(target), from + " -> " + target)));
        }
    }
}
