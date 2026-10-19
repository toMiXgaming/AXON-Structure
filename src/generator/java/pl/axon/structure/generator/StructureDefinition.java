package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Traits;

import java.util.Random;

public interface StructureDefinition {

    /** Nazwa używana w komendzie, np. {@code stacja}. */
    String id();

    String displayName();

    String description();

    /** Buduje nową, lekko losową wersję struktury. */
    Blueprint build(Random random);

    /** Pola nagłówka dla AxonMapGenerator (mapa, strefa, klimat, …). Domyślnie wartości standardowe. */
    default Traits traits() {
        return Traits.defaults();
    }
}
