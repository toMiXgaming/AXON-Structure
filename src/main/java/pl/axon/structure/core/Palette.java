package pl.axon.structure.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Losowy wybór bloku z wagami, żeby ściany i podłogi wyglądały na zniszczone. */
public final class Palette {

    private final Random random;
    private final List<String> states = new ArrayList<>();
    private final List<Integer> weights = new ArrayList<>();
    private int total;

    public Palette(Random random) {
        this.random = random;
    }

    public Palette add(String state, int weight) {
        states.add(state);
        weights.add(weight);
        total += weight;
        return this;
    }

    public String next() {
        int roll = random.nextInt(total);
        for (int i = 0; i < states.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return states.get(i);
            }
        }
        return states.get(states.size() - 1);
    }
}
