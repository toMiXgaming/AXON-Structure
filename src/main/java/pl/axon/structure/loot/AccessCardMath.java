package pl.axon.structure.loot;

import java.util.List;
import java.util.Locale;

/**
 * Czysta logika kart dostępu (etap 16 #7) – bez Bukkita, w pełni testowalna.
 *
 * <p>System opiera się na trzech kartach:
 * <ul>
 *   <li><b>niebieska</b> – poziom 1, wypada w zwykłych skrzyniach,</li>
 *   <li><b>czerwona</b> – poziom 2, wypada w skrzyniach wojskowych,</li>
 *   <li><b>zielona</b> – poziom 3, powstaje z niebieskiej + czerwonej i otwiera skrzynie elitarne.</li>
 * </ul>
 *
 * <p>Karta jest <b>zużywana</b> przy użyciu: gracz trzyma ją w ręce, klika elitarną skrzynię,
 * a karta znika z ekwipunku. Dzięki temu nie da się opróżnić wszystkich elitarnych skrzyń
 * jedną kartą.
 */
public final class AccessCardMath {

    /** Identyfikatory kart – używane w config.yml (klucz {@code karta:}) i w PDC przedmiotu. */
    public static final String BLUE = "niebieska";
    public static final String RED = "czerwona";
    public static final String GREEN = "zielona";

    /** Kolejność od najsłabszej do najmocniejszej – do porównań poziomów. */
    private static final List<String> ORDER = List.of(BLUE, RED, GREEN);

    private AccessCardMath() {
    }

    /** Poziom karty: 1 = niebieska, 2 = czerwona, 3 = zielona, 0 = nieznana. */
    public static int level(String cardId) {
        if (cardId == null) {
            return 0;
        }
        int index = ORDER.indexOf(cardId.trim().toLowerCase(Locale.ROOT));
        return index < 0 ? 0 : index + 1;
    }

    /** Czy identyfikator to znana karta dostępu. */
    /** Id karty o danym poziomie (1 = niebieska … 3 = zielona); {@code null} poza zakresem. */
    public static String idOf(int level) {
        return level >= 1 && level <= ORDER.size() ? ORDER.get(level - 1) : null;
    }

    public static boolean known(String cardId) {
        return level(cardId) > 0;
    }

    /**
     * Czy karta o poziomie {@code cardLevel} otwiera skrzynię wymagającą {@code requiredLevel}.
     *
     * <p>Zasada: karta musi być <b>co najmniej</b> tak mocna jak wymóg. Zielona (3) otwiera
     * wszystko, czerwona (2) otwiera skrzynie wymagające 1 lub 2, niebieska (1) tylko poziom 1.
     * Wymóg 0 (zwykła skrzynia) nie potrzebuje żadnej karty.
     */
    public static boolean opens(int cardLevel, int requiredLevel) {
        if (requiredLevel <= 0) {
            return true;
        }
        return cardLevel >= requiredLevel;
    }

    /** Skrót: czy karta o danym id otwiera skrzynię o danym wymogu. */
    public static boolean opens(String cardId, int requiredLevel) {
        return opens(level(cardId), requiredLevel);
    }

    /**
     * Czy z dwóch kart można zrobić zieloną.
     *
     * <p>Wymagana jest dokładnie jedna niebieska i jedna czerwona – dwie niebieskie
     * ani dwie czerwone nie wystarczą (inaczej karty same by się mnożyły).
     */
    public static boolean canCombine(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        return (BLUE.equals(a) && RED.equals(b)) || (RED.equals(a) && BLUE.equals(b));
    }

    /** Wynik łączenia: id karty, która powstaje (albo {@code null}, gdy para jest zła). */
    public static String combine(String first, String second) {
        return canCombine(first, second) ? GREEN : null;
    }

    /** Nazwa karty do wyświetlenia (z kolorami) – używana w lore i komunikatach. */
    public static String displayName(String cardId) {
        return switch (normalize(cardId)) {
            case BLUE -> "&9Niebieska karta dostępu";
            case RED -> "&cCzerwona karta dostępu";
            case GREEN -> "&aZielona karta dostępu";
            default -> "&7Karta dostępu";
        };
    }

    /** Kolor karty (do paska akcji / czatu). */
    public static String color(String cardId) {
        return switch (normalize(cardId)) {
            case BLUE -> "&9";
            case RED -> "&c";
            case GREEN -> "&a";
            default -> "&7";
        };
    }

    /** Opis poziomu dostępu do lore karty. */
    public static String levelLine(String cardId) {
        int level = level(cardId);
        if (level <= 0) {
            return "&7Poziom dostępu: ?";
        }
        return "&7Poziom dostępu: &f" + level;
    }

    private static String normalize(String cardId) {
        return cardId == null ? "" : cardId.trim().toLowerCase(Locale.ROOT);
    }
}
