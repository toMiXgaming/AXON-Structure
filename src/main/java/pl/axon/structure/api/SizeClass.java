package pl.axon.structure.api;

/** Klasa rozmiaru struktury: ile sztuk na mapę i jak duży plac jej przydzielić. */
public enum SizeClass {
    DUZA,
    SREDNIA,
    MALA;

    /** Klasa liczona z wymiarów, gdy nagłówek nie podaje {@code rozmiar-klasa}: dłuższy bok ≥ 100 duża, ≥ 50 średnia. */
    public static SizeClass fromSize(int width, int depth) {
        int longest = Math.max(width, depth);
        if (longest >= 100) {
            return DUZA;
        }
        if (longest >= 50) {
            return SREDNIA;
        }
        return MALA;
    }
}
