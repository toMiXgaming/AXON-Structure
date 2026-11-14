package pl.axon.structure.vehicle;

import java.util.List;
import java.util.Locale;

/**
 * Katalog wraków pojazdów PRL dostępnych w paczce AxonCore.
 *
 * <p>Modele powstają w {@code QualityArmory-texturepack/zrodla/axoncore/vehicles.py}. Model musi się
 * zmieścić w zakresie -16..32 jednostek, więc pojazd jest tam zmniejszony, a plugin rysuje go jako
 * {@code ItemDisplay} ze skalą {@code scale} = {@code 1.6 / f} (te same liczby wypisuje
 * {@code gen_axoncore.py}). Dopiero z tą skalą wrak ma prawdziwe wymiary: 1 kratka = 1 m.
 *
 * <p>Spód kół leży na {@code y = 0} modelu, a wyświetlacz stoi w środku modelu (8 8 8), więc środek
 * wyświetlacza jest {@code scale / 2} kratki nad powierzchnią gruntu (nie nad blokiem gruntu).
 *
 * <p>Wymiary w kratkach ({@code length} × {@code width} × {@code height}) to obrys modelu po skalowaniu.
 * Służą do barier (kolizja) i hitboxów bagażnika, żeby pokrywały się z tym, co widać.
 *
 * <p>Klasa jest czysta (bez Bukkita) — testy jednostkowe liczą na niej całą geometrię.
 */
public record VehicleModel(String id, String displayName, double length, double width, double height,
                           double scale, String lootTable) {

    /**
     * Wszystkie wraki z paczki. Kolejność = kolejność losowania w puli (bez znaczenia dla logiki).
     * Długość i szerokość: {@code Vehicle(length, width)} z {@code vehicles.py} w decymetrach / 10.
     * Wysokość: najwyższy punkt modelu × skala / 16. Skala: {@code 1.6 / f} z {@code vehicles.py}.
     */
    public static final List<VehicleModel> ALL = List.of(
            new VehicleModel("wrak_fiat_126p", "&7Wrak Fiata 126p", 3.1, 1.4, 1.30, 1.078, "wrak_auto"),
            new VehicleModel("wrak_polonez", "&7Wrak Poloneza", 4.3, 1.7, 1.46, 1.496, "wrak_auto"),
            new VehicleModel("wrak_zuk", "&7Wrak Żuka", 4.5, 1.9, 1.95, 1.565, "wrak_auto"),
            new VehicleModel("wrak_nysa", "&7Wrak Nysy", 4.6, 1.9, 1.96, 1.6, "wrak_auto"),
            new VehicleModel("wrak_star_266", "&7Wrak Stara 266", 7.0, 2.5, 2.69, 2.435, "wrak_wojskowy"),
            new VehicleModel("wrak_autosan", "&7Wrak Autosana", 10.0, 2.5, 2.87, 3.478, "wrak_auto"),
            new VehicleModel("wrak_ursus", "&7Wrak Ursusa", 3.6, 1.8, 1.90, 1.252, "wrak_auto"),
            new VehicleModel("wrak_wsk", "&7Wrak WSK", 2.0, 0.7, 1.07, 1.0, "wrak_auto")
    );

    public VehicleModel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id wraku nie może być puste");
        }
        if (length <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("wymiary wraku muszą być dodatnie: " + id);
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("skala wraku musi być dodatnia: " + id);
        }
        if (lootTable == null || lootTable.isBlank()) {
            lootTable = "wrak_auto";
        }
    }

    /**
     * Wysokość środka wyświetlacza nad powierzchnią gruntu (kratki) — {@code scale / 2}, bo środek
     * modelu (y = 8) leży pół modelu nad spodem kół (y = 0).
     */
    public double centerHeight() {
        return scale / 2.0;
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
