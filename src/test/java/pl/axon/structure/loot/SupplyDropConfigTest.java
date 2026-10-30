package pl.axon.structure.loot;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pilnuje, że prawdziwy {@code config.yml} AXON-Structure ma kompletny zestaw
 * potrzebny zrzutom: sekcję {@code zrzuty}, pulę {@code zrzut} i klasę
 * {@code zrzut-zaopatrzenia} wskazującą na tabelę lootu {@code zrzut}.
 *
 * <p>Bez tego zrzut w grze nie ma czego postawić – komenda i timer milczą.
 */
class SupplyDropConfigTest {

    private static final Logger LOGGER = Logger.getLogger("SupplyDropConfigTest");

    private static YamlConfiguration config;

    @BeforeAll
    static void loadRealConfig() throws IOException, InvalidConfigurationException {
        File file = new File("src/main/resources/config.yml");
        assertTrue(file.isFile(), "brak pliku config.yml w src/main/resources");
        config = new YamlConfiguration();
        config.load(file);
    }

    @Test
    void sekcjaZrzutyIstnieje() {
        assertTrue(config.isConfigurationSection("zrzuty"), "brak sekcji zrzuty");
        assertTrue(config.getBoolean("zrzuty.wlaczone", false), "zrzuty powinny być domyślnie włączone");
        assertFalse(config.getStringList("zrzuty.swiaty").isEmpty(), "pusta lista światów zrzutów");
        assertTrue(config.getInt("zrzuty.co-ile-minut", 0) > 0, "interwał zrzutów musi być dodatni");
        assertTrue(config.getInt("zrzuty.promien", 0) >= 50, "promień zrzutów musi być sensowny");
        assertTrue(config.getInt("zrzuty.min-promien", -1) >= 0, "minimalny promień nie może być ujemny");
    }

    @Test
    void pulaZrzutWskazujeNaKlaseZrzutZaopatrzenia() {
        assertTrue(config.isConfigurationSection("pule.zrzut"), "brak puli 'zrzut'");
        assertEquals(100, config.getInt("pule.zrzut.zrzut-zaopatrzenia", 0),
                "pula zrzut powinna zawsze losować klasę zrzut-zaopatrzenia");
    }

    @Test
    void klasaZrzutZaopatrzeniaJestModelem() {
        String base = "klasy.zrzut-zaopatrzenia";
        assertTrue(config.isConfigurationSection(base), "brak klasy 'zrzut-zaopatrzenia'");
        assertEquals("model", config.getString(base + ".blok"), "zrzut musi być modelem, nie blokiem");
        assertEquals("zrzut", config.getString(base + ".model"), "nazwa modelu musi pasować do resourcepacka");
        assertNotNull(config.getString(base + ".nazwa"), "klasa zrzutu musi mieć nazwę");
        assertTrue(config.getInt(base + ".min-przedmiotow", 0) > 0, "zrzut musi mieć minimum przedmiotów");
    }

    @Test
    void klasaZrzutZaopatrzeniaCiagnieLootZTabeliZrzut() {
        List<?> loot = config.getList("klasy.zrzut-zaopatrzenia.loot");
        assertNotNull(loot, "klasa zrzut-zaopatrzenia nie ma listy loot");
        assertFalse(loot.isEmpty(), "lista loot zrzutu jest pusta");
        boolean found = false;
        for (Object raw : loot) {
            if (raw instanceof java.util.Map<?, ?> map) {
                Object table = map.get("tabela");
                if (table != null && SupplyDropService.TABLE.equals(String.valueOf(table))) {
                    found = true;
                }
            }
        }
        assertTrue(found, "klasa zrzut-zaopatrzenia musi czerpać z tabeli '" + SupplyDropService.TABLE + "'");
    }

    @Test
    void klasaZrzutZaopatrzeniaWczytujeSieBezBledow() {
        LootConfig loot = LootConfig.load(config, LOGGER);
        CrateClass crate = loot.crateClass("zrzut-zaopatrzenia");
        assertNotNull(crate, "LootConfig nie rozpoznał klasy zrzut-zaopatrzenia");
        assertEquals(CrateClass.Kind.MODEL, crate.kind(), "zrzut musi być rodzaju MODEL");
        assertEquals("zrzut", crate.modelName(), "nazwa modelu zrzutu");
        // Zrzut nie ma własnego interwału – odświeżaniem zajmuje się timer zrzutów.
        assertFalse(crate.hasOwnRefresh(), "zrzut nie powinien mieć własnego odświeżania");
    }

    @Test
    void pulaZrzutLosujeZawszeKlaseZrzutZaopatrzenia() {
        LootConfig loot = LootConfig.load(config, LOGGER);
        CratePool pool = loot.pool(SupplyDropService.POOL);
        assertNotNull(pool, "LootConfig nie rozpoznał puli '" + SupplyDropService.POOL + "'");
        java.util.Random random = new java.util.Random(1L);
        for (int i = 0; i < 200; i++) {
            assertEquals("zrzut-zaopatrzenia", pool.roll(random));
        }
    }
}
