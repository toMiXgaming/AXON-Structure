package pl.axon.structure.loot;

/**
 * Matematyka blokady skrzyni: ile zostało do otwarcia i jak to pokazać.
 *
 * <p>Blokada jest liczona jako znacznik czasu (epoch millis) zapisany w PDC hitboxa.
 * Dzięki temu przetrwa restart serwera i nie wymaga zmian w schemacie {@code dane.yml}.
 *
 * <p>Klasa jest czysta (bez Bukkita), żeby dała się przetestować jednostkowo.
 */
public final class CrateLockMath {

    /** Znacznik „brak blokady” – cokolwiek ≤ 0 znaczy, że skrzynia jest otwarta. */
    public static final long UNLOCKED = 0L;

    private CrateLockMath() {
    }

    /**
     * Znacznik odblokowania dla skrzyni postawionej w chwili {@code now}.
     *
     * @param now         czas postawienia (epoch millis)
     * @param lockMinutes długość blokady w minutach; ≤ 0 oznacza brak blokady
     * @return znacznik odblokowania albo {@link #UNLOCKED}
     */
    public static long unlockAt(long now, int lockMinutes) {
        if (lockMinutes <= 0) {
            return UNLOCKED;
        }
        return now + lockMinutes * 60_000L;
    }

    /** Czy skrzynia jest jeszcze zablokowana w chwili {@code now}. */
    public static boolean locked(long unlockAt, long now) {
        return unlockAt > now;
    }

    /** Ile milisekund zostało do odblokowania (0, gdy już otwarta). */
    public static long remainingMillis(long unlockAt, long now) {
        long left = unlockAt - now;
        return left > 0 ? left : 0L;
    }

    /**
     * Odliczanie w formacie {@code mm:ss} (albo {@code h:mm:ss} powyżej godziny).
     * Zaokrągla w górę, żeby „0:00” pokazywało się dopiero po faktycznym odblokowaniu.
     */
    public static String countdown(long remainingMillis) {
        long totalSeconds = (remainingMillis + 999L) / 1000L;
        if (totalSeconds <= 0) {
            return "0:00";
        }
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    /**
     * Linia wyświetlana nad modelem. Gdy blokada minęła, zwraca {@code null} –
     * wywołujący usuwa wtedy wyświetlacz zamiast pokazywać „0:00”.
     */
    public static String displayLine(long unlockAt, long now) {
        long left = remainingMillis(unlockAt, now);
        if (left <= 0) {
            return null;
        }
        return "&c&lZABLOKOWANA &7" + countdown(left);
    }

    /**
     * Czy blokada właśnie minęła między {@code previous} a {@code now} – używane, żeby
     * ogłosić odblokowanie dokładnie raz.
     */
    public static boolean justUnlocked(long unlockAt, long previous, long now) {
        return unlockAt > UNLOCKED && unlockAt > previous && unlockAt <= now;
    }
}
