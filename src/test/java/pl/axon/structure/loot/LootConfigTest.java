package pl.axon.structure.loot;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy wczytywania klas skrzyń z configu – w szczególności nowych wpisów
 * {@code przedmiot: axon:*} i {@code tabela: *} dodanych w etapie 4.
 */
class LootConfigTest {

    private static final Logger LOGGER = Logger.getLogger("LootConfigTest");

    private static LootConfig load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return LootConfig.load(configuration, LOGGER);
    }

    @Test
    void przedmiotAxonJestRozpoznawany() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: beczka
                    loot:
                      - {przedmiot: axon:scrap, ilosc: "2-6", szansa: 100}
                """);

        CrateClass crate = config.crateClass("testowa");
        assertNotNull(crate);
        assertEquals(1, crate.loot().size());
        CrateClass.LootEntry entry = crate.loot().get(0);
        assertTrue(entry.isAxon());
        assertFalse(entry.isTable());
        assertEquals("scrap", entry.axonId());
        assertEquals(2, entry.min());
        assertEquals(6, entry.max());
        assertEquals(100.0, entry.chance());
    }

    @Test
    void prefiksAxonJestOdpornyNaWielkieLitery() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: beczka
                    loot:
                      - {przedmiot: "AXON:Body_Pistol", szansa: 5}
                """);

        CrateClass.LootEntry entry = config.crateClass("testowa").loot().get(0);
        assertTrue(entry.isAxon());
        assertEquals("body_pistol", entry.axonId());
    }

    @Test
    void tabelaJestRozpoznawana() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  beczka-przydrozna:
                    blok: beczka
                    loot:
                      - {tabela: beczka_przydrozna, szansa: 100}
                """);

        CrateClass.LootEntry entry = config.crateClass("beczka-przydrozna").loot().get(0);
        assertTrue(entry.isTable());
        assertFalse(entry.isAxon());
        assertEquals("beczka_przydrozna", entry.tableId());
        assertNull(entry.material());
    }

    @Test
    void zwyklyPrzedmiotMinecraftaNadalDziala() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: skrzynia
                    loot:
                      - {przedmiot: iron_ingot, ilosc: "1-4", szansa: 50, nazwa: "&fSztaba"}
                """);

        CrateClass.LootEntry entry = config.crateClass("testowa").loot().get(0);
        assertFalse(entry.isAxon());
        assertFalse(entry.isTable());
        assertEquals(org.bukkit.Material.IRON_INGOT, entry.material());
        assertEquals("§fSztaba", entry.name());
    }

    @Test
    void nieznanyPrzedmiotMinecraftaJestPomijany() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: beczka
                    loot:
                      - {przedmiot: nie_ma_takiego, szansa: 100}
                      - {przedmiot: axon:scrap, szansa: 100}
                """);

        CrateClass crate = config.crateClass("testowa");
        assertEquals(1, crate.loot().size());
        assertTrue(crate.loot().get(0).isAxon());
    }

    @Test
    void wpisBezPrzedmiotuAniTabeliJestPomijany() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: beczka
                    loot:
                      - {ilosc: "1-2", szansa: 100}
                      - {przedmiot: axon:scrap, szansa: 100}
                """);

        assertEquals(1, config.crateClass("testowa").loot().size());
    }

    @Test
    void pustaNazwaTabeliJestPomijana() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  testowa:
                    blok: beczka
                    loot:
                      - {tabela: "   ", szansa: 100}
                      - {przedmiot: axon:scrap, szansa: 100}
                """);

        assertEquals(1, config.crateClass("testowa").loot().size());
    }

    @Test
    void domyslnyConfigStrukturySieWczytuje() throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        try (java.io.InputStream stream = LootConfigTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(stream, "config.yml musi być w resources");
            configuration.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        LootConfig config = LootConfig.load(configuration, LOGGER);
        assertFalse(config.classes().isEmpty());
        assertFalse(config.pools().isEmpty());

        CrateClass roadside = config.crateClass("beczka-przydrozna");
        assertNotNull(roadside, "klasa beczka-przydrozna musi istnieć");
        assertEquals(CrateClass.Kind.BARREL, roadside.kind());
        assertEquals(1, roadside.loot().size());
        assertTrue(roadside.loot().get(0).isTable());
        assertEquals("beczka_przydrozna", roadside.loot().get(0).tableId());

        assertNotNull(config.pool("droga"), "pula 'droga' dla beczek przy drogach musi istnieć");
        assertTrue(config.pool("droga").weights().containsKey("beczka-przydrozna"));
    }

    @Test
    void beczkaPaliwaMaPrawdziwyZlom() throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        try (java.io.InputStream stream = LootConfigTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            configuration.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        CrateClass fuel = LootConfig.load(configuration, LOGGER).crateClass("beczka-paliwa");
        assertNotNull(fuel);
        List<CrateClass.LootEntry> axonEntries = fuel.loot().stream().filter(CrateClass.LootEntry::isAxon).toList();
        assertEquals(1, axonEntries.size());
        assertEquals("scrap", axonEntries.get(0).axonId());
    }

    @Test
    void rodzajModelJestRozpoznawany() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  skrzynia-wojskowa:
                    blok: model
                    nazwa: "&cSkrzynia wojskowa"
                    model: skrzynia_wojskowa
                    strefa: "&6Baza wojskowa"
                    loot:
                      - {tabela: skrzynia_wojskowa, szansa: 100}
                """);

        CrateClass crate = config.crateClass("skrzynia-wojskowa");
        assertNotNull(crate);
        assertEquals(CrateClass.Kind.MODEL, crate.kind());
        assertFalse(crate.kind().isBlock(), "model nie stawia prawdziwego bloku");
        assertEquals("skrzynia_wojskowa", crate.modelName());
        assertEquals("§6Baza wojskowa", crate.zone());
    }

    @Test
    void rodzajModelPrzyjmujeAliasy() throws InvalidConfigurationException {
        for (String alias : List.of("model", "pojemnik", "pojemnik-model", "MODEL")) {
            LootConfig config = load("""
                    klasy:
                      testowa:
                        blok: %s
                        loot: []
                    """.formatted(alias));
            assertEquals(CrateClass.Kind.MODEL, config.crateClass("testowa").kind(), alias);
        }
    }

    @Test
    void modelBezKluczaModelUzywaIdKlasy() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  skrzynia-wojskowa:
                    blok: model
                    loot: []
                """);

        CrateClass crate = config.crateClass("skrzynia-wojskowa");
        assertEquals("skrzynia-wojskowa", crate.modelName());
        assertEquals("", crate.zone());
    }

    @Test
    void skrzynieBlokoweNadalSaBlokami() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  skrzynia:
                    blok: skrzynia
                    loot: []
                  beczka:
                    blok: beczka
                    loot: []
                  ramka:
                    blok: ramka
                    loot: []
                """);

        assertTrue(config.crateClass("skrzynia").kind().isBlock());
        assertTrue(config.crateClass("beczka").kind().isBlock());
        assertFalse(config.crateClass("ramka").kind().isBlock());
    }

    @Test
    void odswiezanieMinutJestWczytywane() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  skrzynia-wojskowa:
                    blok: model
                    odswiezanie-minut: 30
                    loot: []
                """);

        CrateClass crate = config.crateClass("skrzynia-wojskowa");
        assertEquals(30, crate.refreshMinutes());
        assertTrue(crate.hasOwnRefresh());
    }

    @Test
    void brakOdswiezaniaMinutZnaczyGlobalnyInterwal() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  zwykla:
                    blok: beczka
                    loot: []
                """);

        CrateClass crate = config.crateClass("zwykla");
        assertEquals(0, crate.refreshMinutes());
        assertFalse(crate.hasOwnRefresh());
    }

    @Test
    void ujemneOdswiezanieMinutJestZerowane() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  dziwna:
                    blok: beczka
                    odswiezanie-minut: -5
                    loot: []
                """);

        assertEquals(0, config.crateClass("dziwna").refreshMinutes());
    }

    @Test
    void prawdziwyConfigMaSkrzynieWojskowa() throws Exception {
        java.io.File file = new java.io.File("src/main/resources/config.yml");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        LootConfig config = LootConfig.load(configuration, LOGGER);

        CrateClass crate = config.crateClass("skrzynia-wojskowa");
        assertNotNull(crate, "config.yml musi mieć klasę skrzynia-wojskowa");
        assertEquals(CrateClass.Kind.MODEL, crate.kind());
        assertEquals("skrzynia_wojskowa", crate.modelName());
        assertEquals(30, crate.refreshMinutes());
        assertFalse(crate.zone().isBlank(), "skrzynia wojskowa musi mieć nazwę strefy do ogłoszeń");

        CratePool pool = config.pool("wojskowa");
        assertNotNull(pool, "config.yml musi mieć pulę wojskowa");
        assertTrue(pool.weights().containsKey("skrzynia-wojskowa"));
    }

    @Test
    void klasaBezLootuNieWysypujeSie() throws InvalidConfigurationException {
        LootConfig config = load("""
                klasy:
                  pusta:
                    blok: beczka
                    loot: []
                """);

        CrateClass crate = config.crateClass("pusta");
        assertNotNull(crate);
        assertTrue(crate.loot().isEmpty());
        assertTrue(crate.roll(new java.util.Random(1), null).isEmpty());
    }
}
