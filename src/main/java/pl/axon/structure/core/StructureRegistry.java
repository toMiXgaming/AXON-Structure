package pl.axon.structure.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class StructureRegistry {

    private final Map<String, StructureDefinition> structures = new LinkedHashMap<>();

    public void register(StructureDefinition definition) {
        structures.put(definition.id().toLowerCase(Locale.ROOT), definition);
    }

    public StructureDefinition get(String id) {
        return structures.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<StructureDefinition> all() {
        return structures.values();
    }
}
