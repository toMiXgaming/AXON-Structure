package pl.axon.structure.loot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Pula losowania dla miejsca na skrzynię: klasa → waga. Klucz {@code brak} oznacza pustą pozycję. */
public record CratePool(String id, LinkedHashMap<String, Integer> weights) {

    public static final String NONE = "brak";

    /** @return id klasy albo {@code null}, gdy wypadło „brak”. */
    public String roll(Random random) {
        int total = 0;
        for (int w : weights.values()) {
            total += Math.max(0, w);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            roll -= Math.max(0, e.getValue());
            if (roll < 0) {
                return e.getKey().equals(NONE) ? null : e.getKey();
            }
        }
        return null;
    }
}
