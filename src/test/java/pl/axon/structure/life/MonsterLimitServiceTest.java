package pl.axon.structure.life;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterLimitServiceTest {

    @Test
    void zeroChanceBlocksEverything() {
        MonsterLimitService.Limit limit = new MonsterLimitService.Limit(0, 0);
        assertFalse(MonsterLimitService.allow(limit, 0.0, 0));
    }

    @Test
    void chanceLetsSomeThrough() {
        MonsterLimitService.Limit limit = new MonsterLimitService.Limit(0.25, 0);
        assertTrue(MonsterLimitService.allow(limit, 0.1, 100));
        assertFalse(MonsterLimitService.allow(limit, 0.3, 0));
    }

    @Test
    void maxCapsMonstersInside() {
        MonsterLimitService.Limit limit = new MonsterLimitService.Limit(1, 5);
        assertTrue(MonsterLimitService.allow(limit, 0.99, 4));
        assertFalse(MonsterLimitService.allow(limit, 0.0, 5));
    }

    @Test
    void structureWithoutOwnEntryGetsDefault() {
        MonsterLimitService.Limit gallery = new MonsterLimitService.Limit(0.25, 6);
        MonsterLimitService.Limit fallback = new MonsterLimitService.Limit(0.2, 0);
        Map<String, MonsterLimitService.Limit> limits = Map.of("galeria", gallery);
        assertSame(gallery, MonsterLimitService.limitFor(limits, fallback, "Galeria"));
        assertSame(fallback, MonsterLimitService.limitFor(limits, fallback, "szpital"));
        assertNull(MonsterLimitService.limitFor(limits, null, "szpital"));
    }

    @Test
    void defaultLetsAboutOneInFiveThrough() {
        MonsterLimitService.Limit fallback = new MonsterLimitService.Limit(0.2, 0);
        java.util.Random random = new java.util.Random(42);
        int passed = 0;
        for (int i = 0; i < 10_000; i++) {
            if (MonsterLimitService.allow(fallback, random.nextDouble(), 0)) {
                passed++;
            }
        }
        assertTrue(passed > 1_800 && passed < 2_200, "przeszło " + passed);
    }
}
