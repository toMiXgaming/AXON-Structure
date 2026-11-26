package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;

/** Stanowisko wybierane z pustych pól przy wskazanym miejscu, po ustawieniu mebli. */
final class MeliniarzMarkers {
    private MeliniarzMarkers() {}

    static void add(Blueprint blueprint, int x, int y, int z, String facing) {
        for (int distance = 0; distance <= 3; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) != distance || !free(blueprint, x + dx, y, z + dz)) continue;
                    blueprint.crate(x + dx, y, z + dz, facing, "npc-meliniarz");
                    return;
                }
            }
        }
    }

    private static boolean free(Blueprint blueprint, int x, int y, int z) {
        if (!blueprint.inBounds(x, y - 1, z) || !blueprint.inBounds(x, y + 1, z)
                || !blueprint.isAir(x, y, z) || !blueprint.isAir(x, y + 1, z)
                || blueprint.isAir(x, y - 1, z)) {
            return false;
        }
        String floor = blueprint.get(x, y - 1, z);
        if (floor.startsWith("water") || floor.startsWith("lava") || floor.contains("ladder")) return false;
        Blueprint.Pos position = new Blueprint.Pos(x, y, z);
        return blueprint.spawnPoints().stream().noneMatch(point -> point.pos().equals(position));
    }
}
