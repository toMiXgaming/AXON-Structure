package pl.axon.structure.oasis;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobBoardTest {

    @Test
    void zlecenieOdDniaCzekaNaDzienWipeu() {
        assertTrue(JobBoard.unlocked(0, 0));
        assertFalse(JobBoard.unlocked(7, 6));
        assertTrue(JobBoard.unlocked(7, 7));
        assertTrue(JobBoard.unlocked(7, 12));
    }

    @Test
    void domyslnyConfigMaZleceniaZaHqmOdDniaSiedem() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (var in = getClass().getResourceAsStream("/config.yml")) {
            config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        List<Map<?, ?>> jobs = config.getMapList("oaza.zlecenia.lista");
        long hqm = jobs.stream().filter(j -> "axon:hqm".equals(j.get("nagroda-przedmiot"))).count();
        assertEquals(3, hqm);
        for (Map<?, ?> job : jobs) {
            if ("axon:hqm".equals(job.get("nagroda-przedmiot"))) {
                assertEquals(7, job.get("od-dnia"), "HQM ze zleceń dopiero od dnia 7");
            }
        }
    }

    @Test
    void zrzutLosujeTabeleZrzutZAxonItems() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (var in = getClass().getResourceAsStream("/config.yml")) {
            config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        String pool = config.getString("zrzuty.pula");
        assertEquals(100, config.getInt("pule." + pool + ".skrzynia-zrzutu"));
        List<Map<?, ?>> loot = config.getMapList("pojemniki.klasy.skrzynia-zrzutu.loot");
        assertEquals("tabela:zrzut", loot.get(0).get("przedmiot"));
    }
}
