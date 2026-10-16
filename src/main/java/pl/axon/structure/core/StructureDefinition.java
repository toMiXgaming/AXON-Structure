package pl.axon.structure.core;

import java.util.Random;

public interface StructureDefinition {

    /** Nazwa używana w komendzie, np. {@code stacja}. */
    String id();

    String displayName();

    String description();

    /** Buduje nową, lekko losową wersję struktury. */
    Blueprint build(Random random);
}
