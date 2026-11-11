package pl.axon.structure.npc;

/**
 * Matematyka celowania strażników: dokąd patrzeć i jak płynnie obrócić głowę.
 * <p>
 * Sentinel obraca NPC tylko co swój „update rate” (domyślnie co 10 ticków), więc bez tego głowa przeskakuje
 * skokami. Tu co tick robimy kawałek obrotu w stronę celu: szybko, gdy różnica jest duża, i miękko na końcu.
 */
public final class AimMath {

    /** Jaką część brakującego kąta nadrabiamy w jednym ticku. */
    static final double EASE = 0.45;
    /** Najszybszy obrót w poziomie i w pionie (stopnie na tick). */
    static final double MAX_YAW_STEP = 35.0;
    static final double MAX_PITCH_STEP = 20.0;
    /** Poniżej tego (stopnie) ustawiamy od razu dokładnie na cel. */
    static final double SNAP = 0.75;

    /**
     * Skala „celnosc” z configu → pole {@code accuracy} Sentinela. Sentinel dodaje losowe ±accuracy do każdej osi
     * kierunku (wektora długości 1), a QualityArmory mnoży przez nie jeszcze rozrzut broni, więc surowe 0,25
     * to już kilkanaście stopni i prawie same pudła na 20 bloków. Po przeskalowaniu: 0,25 trafia niemal zawsze,
     * 0,5 – około 2/3 strzałów na 20 bloków, 1 – co czwarty.
     */
    public static final double ACCURACY_SCALE = 0.04;

    private AimMath() {
    }

    /** Pole accuracy dla Sentinela z wartości „celnosc” z configu. */
    public static double sentinelAccuracy(double celnosc) {
        return Math.max(0.0, celnosc) * ACCURACY_SCALE;
    }

    /** Kąty Minecrafta (yaw, pitch w stopniach) patrzenia z punktu {@code from} na punkt {@code to}. */
    public static float[] yawPitch(double fx, double fy, double fz, double tx, double ty, double tz) {
        double dx = tx - fx;
        double dy = ty - fy;
        double dz = tz - fz;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{wrap(yaw), pitch};
    }

    /** Kąt w zakresie [-180, 180). */
    public static float wrap(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        } else if (d < -180.0) {
            d += 360.0;
        }
        return (float) d;
    }

    /** Jeden tick obrotu z (yaw, pitch) w stronę (wantYaw, wantPitch). */
    public static float[] step(float yaw, float pitch, float wantYaw, float wantPitch) {
        float newYaw = wrap(yaw + move(wrap(wantYaw - yaw), MAX_YAW_STEP));
        float newPitch = pitch + move(wantPitch - pitch, MAX_PITCH_STEP);
        return new float[]{newYaw, Math.max(-90f, Math.min(90f, newPitch))};
    }

    private static float move(double delta, double max) {
        if (Math.abs(delta) <= SNAP) {
            return (float) delta;
        }
        double part = delta * EASE;
        // co najmniej SNAP stopnia, żeby końcówka nie ciągnęła się w nieskończoność
        if (Math.abs(part) < SNAP) {
            part = Math.copySign(SNAP, delta);
        }
        return (float) Math.max(-max, Math.min(max, part));
    }
}
