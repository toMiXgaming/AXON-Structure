package pl.axon.structure.loot;

import java.util.Locale;
import java.util.Map;

/**
 * Czysta matematyka rozbijania beczek (jak ścinanie drzew w AxonMapGenerator): beczka ma pulę HP, a każde uderzenie
 * zabiera tyle, ile daje przedmiot w ręce. Bez Bukkita, więc testowalna bez serwera.
 */
public final class BarrelMath {

    private BarrelMath() {
    }

    /**
     * Obrażenia przedmiotów z configu ({@code pojemniki.niszczenie}).
     *
     * @param hand    goła ręka
     * @param other   cokolwiek, co nie pasuje do {@code types} ani {@code items} (blok, jedzenie, broń palna)
     * @param types   obrażenia rodzaju narzędzia: końcówka nazwy materiału ({@code axe}, {@code sword}, {@code mace})
     * @param tiers   mnożnik materiału narzędzia: początek nazwy ({@code wooden}, {@code iron}, {@code netherite})
     * @param items   dokładne obrażenia konkretnego materiału; nadpisuje rodzaj i materiał
     * @param enchant ile procent obrażeń dodaje każdy poziom Ostrości albo Wydajności (0.15 = +15%)
     */
    public record Damage(double hand, double other, Map<String, Double> types, Map<String, Double> tiers,
                         Map<String, Double> items, double enchant) {

        public static final Damage DEFAULT = new Damage(3, 4,
                Map.of("axe", 8.0, "sword", 7.0, "pickaxe", 6.0, "shovel", 5.0, "hoe", 4.0, "mace", 12.0,
                        "trident", 8.0),
                Map.of("wooden", 1.0, "stone", 1.25, "iron", 1.5, "golden", 1.1, "diamond", 1.75, "netherite", 2.0),
                Map.of(), 0.15);

        /**
         * Obrażenia bazowe przedmiotu o nazwie materiału {@code material} (np. {@code IRON_AXE}); {@code null} albo
         * {@code AIR} = goła ręka.
         */
        public double base(String material) {
            if (material == null || material.isBlank() || material.equalsIgnoreCase("air")) {
                return Math.max(0, hand);
            }
            String name = material.toLowerCase(Locale.ROOT);
            Double exact = items.get(name);
            if (exact != null) {
                return Math.max(0, exact);
            }
            Double whole = types.get(name); // mace, trident: bez materiału w nazwie
            if (whole != null) {
                return Math.max(0, whole);
            }
            int cut = name.lastIndexOf('_');
            if (cut > 0) {
                Double type = types.get(name.substring(cut + 1));
                if (type != null) {
                    Double tier = tiers.get(name.substring(0, cut));
                    return Math.max(0, type * (tier == null ? 1.0 : tier));
                }
            }
            return Math.max(0, other);
        }
    }

    /**
     * Obrażenia uderzenia.
     *
     * @param base         obrażenia bazowe przedmiotu ({@link Damage#base})
     * @param enchantLevel poziom Ostrości albo Wydajności (wyższy z dwóch)
     * @param enchantBonus bonus za poziom ({@link Damage#enchant})
     * @param charge       naładowanie zamachu z gry, 0–1 ({@code Player#getAttackCooldown}); spam LPM = mało
     */
    public static double damage(double base, int enchantLevel, double enchantBonus, double charge) {
        if (base <= 0) {
            return 0;
        }
        double c = clamp(charge, 0, 1);
        // jak w Minecrafcie: niedoładowany cios zadaje 20% + 80% * naładowanie²
        double swing = 0.2 + 0.8 * c * c;
        return base * (1 + Math.max(0, enchantLevel) * Math.max(0, enchantBonus)) * swing;
    }

    /** HP z configu klasy; co najmniej 1. */
    public static double hp(double configured) {
        return Math.max(1, configured);
    }

    /** Ile procent życia zostało, w górę: dopóki beczka stoi, gracz widzi 1%, a nie 0%. */
    public static int percent(double hp, double maxHp) {
        if (maxHp <= 0) {
            return 0;
        }
        return (int) clamp(Math.ceil(hp / maxHp * 100.0), 0, 100);
    }

    /** Pasek złożony z {@code width} znaków: część pełna to pozostałe HP. */
    public static String bar(double hp, double maxHp, int width, String full, String empty) {
        int w = Math.max(1, width);
        double fraction = maxHp <= 0 ? 0 : clamp(hp / maxHp, 0, 1);
        int filled = (int) Math.ceil(fraction * w - 1e-9);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w; i++) {
            sb.append(i < filled ? full : empty);
        }
        return sb.toString();
    }

    /** Kolor paska jak przy drzewach: zielony powyżej 60%, żółty powyżej 30%, potem czerwony. */
    public static String color(int percent) {
        return percent > 60 ? "§a" : percent > 30 ? "§e" : "§c";
    }

    /** Ile jeszcze takich samych uderzeń (pełnych) potrzeba, żeby rozbić; 0 gdy obrażenia są zerowe. */
    public static int hitsLeft(double hp, double damagePerHit) {
        if (damagePerHit <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(hp / damagePerHit - 1e-9));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
