package pl.axon.structure.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimMathTest {

    @Test
    void yawPitchMatchesMinecraftAxes() {
        // +Z to yaw 0, -X to yaw 90, +X to -90; patrzenie w górę to ujemny pitch
        assertEquals(0f, AimMath.yawPitch(0, 0, 0, 0, 0, 5)[0], 1e-4);
        assertEquals(90f, AimMath.yawPitch(0, 0, 0, -5, 0, 0)[0], 1e-4);
        assertEquals(-90f, AimMath.yawPitch(0, 0, 0, 5, 0, 0)[0], 1e-4);
        assertEquals(-45f, AimMath.yawPitch(0, 0, 0, 0, 5, 5)[1], 1e-4);
        assertEquals(45f, AimMath.yawPitch(0, 0, 0, 0, -5, 5)[1], 1e-4);
    }

    @Test
    void stepTakesShortWayAround() {
        // z 170 do -170 to 20 stopni przez 180, nie 340 w drugą stronę
        float[] next = AimMath.step(170f, 0f, -170f, 0f);
        float moved = AimMath.wrap(next[0] - 170f);
        assertTrue(moved > 0 && moved <= 20f, "obrót: " + moved);
    }

    @Test
    void stepIsLimitedAndConverges() {
        float yaw = 0f;
        float pitch = 0f;
        float[] first = AimMath.step(yaw, pitch, 180f - 1f, 60f);
        assertTrue(Math.abs(first[0]) <= AimMath.MAX_YAW_STEP + 1e-3);
        assertTrue(Math.abs(first[1]) <= AimMath.MAX_PITCH_STEP + 1e-3);
        for (int i = 0; i < 30; i++) {
            float[] n = AimMath.step(yaw, pitch, 179f, 60f);
            yaw = n[0];
            pitch = n[1];
        }
        assertEquals(179f, yaw, 1e-3);
        assertEquals(60f, pitch, 1e-3);
    }

    @Test
    void accuracyIsScaledDown() {
        assertEquals(0.0, AimMath.sentinelAccuracy(0), 1e-9);
        assertEquals(0.01, AimMath.sentinelAccuracy(0.25), 1e-9);
        assertEquals(0.0, AimMath.sentinelAccuracy(-1), 1e-9);
    }
}
