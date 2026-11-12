package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;

import java.util.Random;

import static pl.axon.structure.generator.Furniture.GRUZ;
import static pl.axon.structure.generator.Furniture.KALORYFER;
import static pl.axon.structure.generator.Furniture.KARTONY;
import static pl.axon.structure.generator.Furniture.LAWKA;
import static pl.axon.structure.generator.Furniture.MASZYNA;
import static pl.axon.structure.generator.Furniture.OPONY;
import static pl.axon.structure.generator.Furniture.SZAFA;
import static pl.axon.structure.generator.Furniture.WORKI;

/**
 * Doposażenie gotowej struktury w meble z paczki (kartony, opony, gruz, maszyny, szafy, kaloryfery), rozstawione
 * pod ścianami tam, gdzie nic nie zastawiają. Działa po {@code build}, przed postarzaniem. Oaza nie dostaje nic
 * (jest w {@code meble.bez-zamiany}).
 */
final class Furnishing {

    private Furnishing() {
    }

    static void apply(String id, Blueprint bp, Random rnd) {
        Furniture f = new Furniture(bp, 0, rnd);
        int top = bp.height() - 3;
        int g = bp.ground();
        switch (id) {
            case "oilen" -> both(f, top, g, 6, 6, new String[]{KARTONY, OPONY, KALORYFER}, OPONY, GRUZ);
            case "gospodarstwo" -> both(f, top, g, 6, 5, new String[]{WORKI, KARTONY, OPONY}, OPONY, GRUZ);
            case "kombinat" -> both(f, top, g, 10, 6, new String[]{WORKI, MASZYNA, OPONY, KARTONY}, OPONY, GRUZ);
            case "kopalnia" -> both(f, top, g, 12, 6, new String[]{MASZYNA, KARTONY, GRUZ, SZAFA}, GRUZ, OPONY);
            case "jednostka" -> both(f, top, g, 25, 10, new String[]{KARTONY, SZAFA, KALORYFER, OPONY}, OPONY, GRUZ, KARTONY);
            case "szpital" -> both(f, top, g, 25, 5, new String[]{KALORYFER, SZAFA, KARTONY, GRUZ}, GRUZ);
            case "czarny-rynek" -> both(f, top, g, 4, 2, new String[]{KARTONY, WORKI}, GRUZ);
            case "elektrownia" -> both(f, top, g, 14, 6, new String[]{MASZYNA, KARTONY, SZAFA, GRUZ}, GRUZ, OPONY);
            case "osiedle" -> both(f, top, g, 20, 8, new String[]{KALORYFER, SZAFA, KARTONY, GRUZ}, OPONY, GRUZ);
            case "zajezdnia" -> both(f, top, g, 10, 6, new String[]{MASZYNA, OPONY, KARTONY}, GRUZ, OPONY);
            case "galeria" -> both(f, top, g, 30, 10, new String[]{KARTONY, GRUZ, LAWKA}, OPONY, GRUZ);
            default -> {
            }
        }
    }

    /**
     * Usuwa bariery-sieroty: w strukturze bez dużych modeli ({@code modele:}) bariera ma sens tylko nad
     * dwukratkowym meblem (lodówka, dystrybutor, szafa, automat). Gdy mebel pod nią zniknął (zawalony strop,
     * nadpisana kratka), zostawała niewidzialna ściana. Zwraca liczbę usuniętych.
     */
    static int removeOrphanBarriers(Blueprint bp) {
        if (!bp.props().isEmpty()) {
            return 0; // bariery dają kolizję wrakom i lokomotywom
        }
        java.util.List<Blueprint.Pos> orphans = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Blueprint.Pos, String> e : bp.blocks().entrySet()) {
            if (!e.getValue().endsWith("barrier")) {
                continue;
            }
            Blueprint.Pos p = e.getKey();
            String below = bp.get(p.x(), p.y() - 1, p.z());
            if (!(below.contains("calibrated_sculk_sensor") || below.contains("damaged_anvil")
                    || below.contains(Furniture.SZAFA) || below.contains(Furniture.AUTOMAT_GIER))) {
                orphans.add(p);
            }
        }
        for (Blueprint.Pos p : orphans) {
            bp.set(p.x(), p.y(), p.z(), Blueprint.AIR);
        }
        return orphans.size();
    }

    private static void both(Furniture f, int top, int g, int inside, int outside, String[] in, String... out) {
        int a = f.scatter(1, top, inside, true, in);
        int b = f.scatter(Math.max(1, g), g + 2, outside, false, out);
        if (System.getenv("AXS_DEBUG") != null) {
            System.out.println("  meble: " + a + "/" + inside + " w środku, " + b + "/" + outside + " na zewnątrz");
        }
    }
}
