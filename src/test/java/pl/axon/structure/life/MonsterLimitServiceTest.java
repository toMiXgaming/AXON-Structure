package pl.axon.structure.life;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
