package pl.axon.structure.core;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.api.SizeClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opcjonalne pola nagłówka {@code .axs} opisujące, jak struktura ma zachowywać się przy automatycznym
 * stawianiu (AxonMapGenerator). Brak pola = wartość domyślna, więc stare pliki działają bez zmian.
 *
 * @param groundLevel poziom terenu wewnątrz szablonu ({@code grunt: N})
 * @param entrances   wjazdy na krawędzi, w które wpinają się drogi ({@code wjazdy: x z kierunek; ...})
 * @param mapAllowed  czy generator może stawiać tę strukturę ({@code mapa: tak|nie})
 * @param sizeClass   klasa rozmiaru, null = liczona z wymiarów ({@code rozmiar-klasa: duza|srednia|mala})
 * @param zones       preferowane strefy ({@code strefa: ...}), {@code dowolna} = wszystkie
 * @param climates    preferowane klimaty ({@code klimat: snieg,umiarkowany,suchy})
 * @param safe        strefa bez PvP ({@code bezpieczna: tak})
 * @param tags        pozostałe pola nagłówka, których AS nie zna (przechodzą przez zapis bez zmian)
 */
public record Traits(int groundLevel, List<Entrance> entrances, boolean mapAllowed, SizeClass sizeClass,
                     Set<String> zones, Set<String> climates, boolean safe, Map<String, String> tags) {

    private static final Set<String> ZONES = Set.of("rownina", "przy-gorach", "las", "ustronnie", "srodek", "plaza", "dowolna");
    private static final Set<String> CLIMATES = Set.of("snieg", "umiarkowany", "suchy");
    private static final Set<String> FACINGS = Set.of("north", "east", "south", "west");
    private static final Set<String> ANY_ZONE = Set.of("dowolna");
    private static final Set<String> KNOWN_KEYS = Set.of("id", "nazwa", "opis", "rozmiar", "grunt", "wjazdy",
            "rozmiar-klasa", "mapa", "strefa", "klimat", "bezpieczna");

    public Traits {
        entrances = List.copyOf(entrances);
        zones = Collections.unmodifiableSet(new LinkedHashSet<>(zones));
        climates = Collections.unmodifiableSet(new LinkedHashSet<>(climates));
        tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    /** Wartości domyślne: brak wjazdów, mapa dozwolona, dowolna strefa i klimat, bez PvP wyłączonego. */
    public static Traits defaults() {
        return new Traits(0, List.of(), true, null, ANY_ZONE, CLIMATES, false, Map.of());
    }

    public Traits withMapAllowed(boolean value) {
        return new Traits(groundLevel, entrances, value, sizeClass, zones, climates, safe, tags);
    }

    /**
     * Czyta pola nagłówka. Błędna wartość rzuca {@link IOException} z nazwą pola.
     *
     * @param meta   wszystkie pary {@code klucz: wartość} z nagłówka
     * @param width  szerokość szablonu (do sprawdzenia wjazdów)
     * @param depth  głębokość szablonu (do sprawdzenia wjazdów)
     */
    public static Traits parse(Map<String, String> meta, int width, int depth) throws IOException {
        int groundLevel = 0;
        String grunt = meta.get("grunt");
        if (grunt != null && !grunt.isBlank()) {
            groundLevel = parseInt(grunt, "grunt");
            if (groundLevel < 0) {
                throw new IOException("'grunt' nie może być ujemne");
            }
        }
        List<Entrance> entrances = parseEntrances(meta.get("wjazdy"), width, depth);
        boolean mapAllowed = parseYesNo(meta.get("mapa"), "mapa", true);
        SizeClass sizeClass = parseSizeClass(meta.get("rozmiar-klasa"));
        // Stare pliki pisały "strefa: bezpieczna" zamiast osobnego pola: to strefa srodek + bezpieczna: tak.
        String zoneValue = meta.get("strefa");
        boolean legacySafe = false;
        if (zoneValue != null) {
            String[] items = zoneValue.split(",");
            legacySafe = Arrays.stream(items).anyMatch(item -> item.trim().equalsIgnoreCase("bezpieczna"));
            zoneValue = Arrays.stream(items)
                    .map(item -> item.trim().equalsIgnoreCase("bezpieczna") ? "srodek" : item.trim())
                    .collect(Collectors.joining(","));
        }
        Set<String> zones = parseSet(zoneValue, "strefa", ZONES, ANY_ZONE);
        Set<String> climates = parseSet(meta.get("klimat"), "klimat", CLIMATES, CLIMATES);
        boolean safe = legacySafe || parseYesNo(meta.get("bezpieczna"), "bezpieczna", false);
        Map<String, String> tags = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : meta.entrySet()) {
            if (!KNOWN_KEYS.contains(entry.getKey())) {
                tags.put(entry.getKey(), entry.getValue());
            }
        }
        return new Traits(groundLevel, entrances, mapAllowed, sizeClass, zones, climates, safe, tags);
    }

    /** Linie nagłówka do zapisania; pola na wartościach domyślnych są pomijane. */
    public List<String> toHeaderLines() {
        List<String> lines = new ArrayList<>();
        if (groundLevel != 0) {
            lines.add("grunt: " + groundLevel);
        }
        if (!entrances.isEmpty()) {
            lines.add("wjazdy: " + entrances.stream()
                    .map(e -> e.x() + " " + e.z() + " " + e.facing())
                    .collect(Collectors.joining("; ")));
        }
        if (!mapAllowed) {
            lines.add("mapa: nie");
        }
        if (sizeClass != null) {
            lines.add("rozmiar-klasa: " + sizeClass.name().toLowerCase(Locale.ROOT));
        }
        if (!zones.equals(ANY_ZONE)) {
            lines.add("strefa: " + String.join(",", zones));
        }
        if (!climates.equals(CLIMATES)) {
            lines.add("klimat: " + String.join(",", climates));
        }
        if (safe) {
            lines.add("bezpieczna: tak");
        }
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            lines.add(entry.getKey() + ": " + entry.getValue());
        }
        return lines;
    }

    private static int parseInt(String value, String key) throws IOException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IOException("'" + key + "' musi być liczbą całkowitą, a jest '" + value + "'");
        }
    }

    private static List<Entrance> parseEntrances(String value, int width, int depth) throws IOException {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Entrance> result = new ArrayList<>();
        for (String part : value.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length != 3) {
                throw new IOException("'wjazdy' oczekuje 'x z kierunek', a jest '" + part.trim() + "'");
            }
            int x = parseInt(tokens[0], "wjazdy");
            int z = parseInt(tokens[1], "wjazdy");
            if (x < 0 || x >= width || z < 0 || z >= depth) {
                throw new IOException("wjazd " + x + " " + z + " leży poza szablonem");
            }
            String facing = tokens[2].toLowerCase(Locale.ROOT);
            if (!FACINGS.contains(facing)) {
                throw new IOException("zły kierunek wjazdu: '" + tokens[2] + "'");
            }
            result.add(new Entrance(x, z, facing));
        }
        return result;
    }

    private static boolean parseYesNo(String value, String key, boolean fallback) throws IOException {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "tak" -> true;
            case "nie" -> false;
            default -> throw new IOException("'" + key + "' może być tylko 'tak' albo 'nie', a jest '" + value + "'");
        };
    }

    private static SizeClass parseSizeClass(String value) throws IOException {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "duza" -> SizeClass.DUZA;
            case "srednia" -> SizeClass.SREDNIA;
            case "mala" -> SizeClass.MALA;
            default -> throw new IOException("'rozmiar-klasa' może być duza, srednia albo mala, a jest '" + value + "'");
        };
    }

    private static Set<String> parseSet(String value, String key, Set<String> allowed, Set<String> fallback)
            throws IOException {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String item = part.trim().toLowerCase(Locale.ROOT);
            if (item.isEmpty()) {
                continue;
            }
            if (!allowed.contains(item)) {
                throw new IOException("nieznana wartość '" + item + "' w '" + key + "' (dozwolone: "
                        + String.join(", ", allowed) + ")");
            }
            result.add(item);
        }
        return result.isEmpty() ? fallback : result;
    }
}
