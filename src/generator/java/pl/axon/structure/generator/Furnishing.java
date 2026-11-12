package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;

import java.util.Random;

import static pl.axon.structure.generator.Furniture.AMUNICJA;
import static pl.axon.structure.generator.Furniture.BEBEN_KABLA;
import static pl.axon.structure.generator.Furniture.BUTLE;
import static pl.axon.structure.generator.Furniture.FOTEL;
import static pl.axon.structure.generator.Furniture.GRUZ;
import static pl.axon.structure.generator.Furniture.KOMODA;
import static pl.axon.structure.generator.Furniture.KANAPA;
import static pl.axon.structure.generator.Furniture.KRZESLO;
import static pl.axon.structure.generator.Furniture.KRZESLO_BIUROWE;
import static pl.axon.structure.generator.Furniture.LAMPA;
import static pl.axon.structure.generator.Furniture.LODOWKA_DOMOWA;
import static pl.axon.structure.generator.Furniture.LOZKO_SZPITALNE;
import static pl.axon.structure.generator.Furniture.PALETA;
import static pl.axon.structure.generator.Furniture.PRALKA;
import static pl.axon.structure.generator.Furniture.PRYCZA;
import static pl.axon.structure.generator.Furniture.REGAL_MAGAZYNOWY;
import static pl.axon.structure.generator.Furniture.SKRZYNKA_WARZYW;
import static pl.axon.structure.generator.Furniture.SPIND;
import static pl.axon.structure.generator.Furniture.STOL;
import static pl.axon.structure.generator.Furniture.STOL_ODPRAW;
import static pl.axon.structure.generator.Furniture.STOL_OPERACYJNY;
import static pl.axon.structure.generator.Furniture.TOKARKA;
import static pl.axon.structure.generator.Furniture.WOZEK_INWALIDZKI;
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
            case "oilen" -> both(f, top, g, 10, 8, new String[]{KARTONY, PALETA, REGAL_MAGAZYNOWY, KRZESLO_BIUROWE, BUTLE, KALORYFER, LAMPA},
                    new String[]{OPONY, BUTLE, PALETA, GRUZ});
            case "gospodarstwo" -> both(f, top, g, 16, 8, new String[]{KOMODA, FOTEL, LODOWKA_DOMOWA, LAMPA, WORKI, SKRZYNKA_WARZYW, KARTONY, STOL, KRZESLO},
                    new String[]{OPONY, GRUZ, PALETA, WORKI, BEBEN_KABLA});
            case "kombinat" -> both(f, top, g, 24, 10, new String[]{PALETA, WORKI, REGAL_MAGAZYNOWY, MASZYNA, TOKARKA, BUTLE, SKRZYNKA_WARZYW, SPIND, KRZESLO_BIUROWE},
                    new String[]{OPONY, PALETA, WORKI, BEBEN_KABLA, GRUZ});
            case "kopalnia" -> both(f, top, g, 22, 8, new String[]{SPIND, BEBEN_KABLA, BUTLE, AMUNICJA, PALETA, KRZESLO_BIUROWE, STOL, TOKARKA, MASZYNA, LAMPA},
                    new String[]{GRUZ, OPONY, BEBEN_KABLA, PALETA});
            case "jednostka" -> both(f, top, g, 70, 14, new String[]{PRYCZA, PRYCZA, SPIND, SPIND, AMUNICJA, STOL_ODPRAW, KRZESLO_BIUROWE, REGAL_MAGAZYNOWY, PALETA, KALORYFER, BEBEN_KABLA, BUTLE, LAMPA},
                    new String[]{OPONY, AMUNICJA, PALETA, BEBEN_KABLA, GRUZ});
            case "szpital" -> both(f, top, g, 70, 8, new String[]{LOZKO_SZPITALNE, LOZKO_SZPITALNE, WOZEK_INWALIDZKI, SPIND, STOL_OPERACYJNY, KRZESLO_BIUROWE, KALORYFER, LAMPA, SZAFA, KARTONY, LODOWKA_DOMOWA},
                    new String[]{GRUZ, WOZEK_INWALIDZKI, KARTONY});
            case "czarny-rynek" -> both(f, top, g, 10, 3, new String[]{STOL, KRZESLO, KOMODA, LODOWKA_DOMOWA, AMUNICJA, PALETA, KARTONY, LAMPA},
                    new String[]{GRUZ, KARTONY});
            case "elektrownia" -> both(f, top, g, 40, 12, new String[]{BEBEN_KABLA, TOKARKA, MASZYNA, SPIND, BUTLE, REGAL_MAGAZYNOWY, PALETA, KRZESLO_BIUROWE, STOL, LAMPA},
                    new String[]{GRUZ, OPONY, BEBEN_KABLA, BUTLE, PALETA});
            case "osiedle" -> both(f, top, g, 70, 10, new String[]{KOMODA, FOTEL, LODOWKA_DOMOWA, LAMPA, KANAPA, SZAFA, KALORYFER, PRALKA, STOL, KRZESLO, KARTONY},
                    new String[]{OPONY, GRUZ, KARTONY});
            case "zajezdnia" -> both(f, top, g, 24, 12, new String[]{TOKARKA, SPIND, BEBEN_KABLA, BUTLE, PALETA, MASZYNA, REGAL_MAGAZYNOWY, STOL, KRZESLO_BIUROWE},
                    new String[]{GRUZ, OPONY, BEBEN_KABLA, BUTLE, PALETA});
            case "galeria" -> both(f, top, g, 70, 12, new String[]{KARTONY, GRUZ, LAWKA, REGAL_MAGAZYNOWY, PALETA, KANAPA, KRZESLO, STOL, LAMPA, WOZEK_INWALIDZKI},
                    new String[]{OPONY, GRUZ, PALETA});
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
            if (!(below.contains("calibrated_sculk_sensor") || below.contains("damaged_anvil") || Furniture.isTall(below))) {
                orphans.add(p);
            }
        }
        for (Blueprint.Pos p : orphans) {
            bp.set(p.x(), p.y(), p.z(), Blueprint.AIR);
        }
        return orphans.size();
    }

    private static void both(Furniture f, int top, int g, int inside, int outside, String[] in, String[] out) {
        int a = f.scatter(1, top, inside, true, in);
        int b = f.scatter(Math.max(1, g), g + 2, outside, false, out);
        if (System.getenv("AXS_DEBUG") != null) {
            System.out.println("  meble: " + a + "/" + inside + " w środku, " + b + "/" + outside + " na zewnątrz");
        }
    }
}
