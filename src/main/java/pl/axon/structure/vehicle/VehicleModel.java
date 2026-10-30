package pl.axon.structure.vehicle;

import java.util.List;
import java.util.Locale;

/**
 * Katalog wraków pojazdów PRL dostępnych w paczce AxonCore.
 *
 * <p>Modele powstają w {@code QualityArmory-texturepack/zrodla/axoncore/vehicles.py}. Każdy wrak jest
 * budowany w decymetrach (1 kratka = 1 m), a plugin rysuje go jako {@code ItemDisplay} ze skalą
 * {@code scale} (patrz {@code gen_axoncore.py}). Spód kół leży na {@code y = 0} modelu, więc środek
 * modelu (8 8 8) jest {@code scale / 2} kratki nad gruntem i leży w środku obrysu pojazdu.
 *
 * <p>Wymiary w kratkach ({@code length} × {@code width}) służą do wyliczenia obrysu kolizji
 * (bloki {@code barrier}) i podziału dużych wraków na 2–3 części, żeby hitbox i model nie były
 * jednym wielkim klockiem.
 *
 * <p>Klasa jest czysta (bez Bukkita) — testy jednostkowe liczą na niej całą geometrię.
 */
public record VehicleModel(String id, String displayName, double length, double width, double scale,
                           int parts, String lootTable) {

    /** Najdłuższy wrak (Autosan, 10 m) dzielimy na 3 części; resztę na 1 albo 2. */
    public static final int MAX_PARTS = 3;

    /**
     * Wszystkie wraki z paczki. Kolejność = kolejność losowania w puli (bez znaczenia dla logiki).
     * Wymiary w kratkach wynikają z {@code vehicles.py}: {@code Vehicle(length, width)} w decymetrach / 10.
     */
    public static final List<VehicleModel> ALL = List.of(
            new VehicleModel("wrak_fiat_126p", "&7Wrak Fiata 126p", 3.1, 1.4, 1.0, 1, "wrak_auto"),
            new VehicleModel("wrak_polonez", "&7Wrak Poloneza", 4.3, 1.7, 1.0, 2, "wrak_auto"),
            new VehicleModel("wrak_zuk", "&7Wrak Żuka", 4.5, 1.9, 1.0, 2, "wrak_auto"),
            new VehicleModel("wrak_nysa", "&7Wrak Nysy", 4.6, 1.9, 1.0, 2, "wrak_auto"),
            new VehicleModel("wrak_star_266", "&7Wrak Stara 266", 7.0, 2.5, 1.0, 3, "wrak_wojskowy"),
            new VehicleModel("wrak_autosan", "&7Wrak Autosana", 10.0, 2.5, 1.0, 3, "wrak_auto"),
            new VehicleModel("wrak_ursus", "&7Wrak Ursusa", 3.6, 1.8, 1.0, 1, "wrak_auto"),
            new VehicleModel("wrak_wsk", "&7Wrak WSK", 2.0, 0.7, 1.0, 1, "wrak_auto")
    );

    public VehicleModel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id wraku nie może być puste");
        }
        if (length <= 0 || width <= 0) {
            throw new IllegalArgumentException("wymiary wraku muszą być dodatnie: " + id);
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("skala wraku musi być dodatnia: " + id);
        }
        parts = Math.max(1, Math.min(MAX_PARTS, parts));
        if (lootTable == null || lootTable.isBlank()) {
            lootTable = "wrak_auto";
        }
    }

    /** Wysokość środka modelu nad gruntem (kratki) — {@code scale / 2} z geometrii modelu. */
    public double centerHeight() {
        return scale / 2.0;
    }

    /** Czy wrak jest duży (dzielony na części i stawiany z kilku {@code ItemDisplay}). */
    public boolean large() {
        return parts > 1;
    }

    /** Wrak o danym id (bez względu na wielkość liter) albo {@code null}. */
    public static VehicleModel byId(String id) {
        if (id == null) {
            return null;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (VehicleModel model : ALL) {
            if (model.id.equals(key)) {
                return model;
            }
        }
        return null;
    }

    /** Wszystkie id wraków — do walidacji konfiguracji i podpowiedzi w komendzie. */
    public static List<String> ids() {
        return ALL.stream().map(VehicleModel::id).toList();
    }
}
