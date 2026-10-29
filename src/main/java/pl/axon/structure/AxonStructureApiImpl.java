package pl.axon.structure;

import org.bukkit.Bukkit;
import pl.axon.structure.api.AxonStructureApi;
import pl.axon.structure.api.RoadsideSpot;
import pl.axon.structure.api.RotationUtil;
import pl.axon.structure.api.StructureInstanceInfo;
import pl.axon.structure.api.StructureTemplate;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.PasteService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Implementacja {@link AxonStructureApi} rejestrowana w ServicesManager. */
public final class AxonStructureApiImpl implements AxonStructureApi {

    private final AxonStructurePlugin plugin;

    public AxonStructureApiImpl(AxonStructurePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<StructureTemplate> templates() {
        return plugin.library().templates();
    }

    @Override
    public Optional<StructureTemplate> template(String id) {
        return Optional.ofNullable(plugin.library().template(Objects.requireNonNull(id)));
    }

    @Override
    public StructureInstanceInfo register(String instanceKey, String templateId, String world,
                                          int originX, int originY, int originZ, int rotation) {
        requireServerThread();
        Objects.requireNonNull(instanceKey, "instanceKey");
        StructureInstance existing = plugin.store().findByKey(instanceKey);
        if (existing != null) {
            return info(instanceKey, existing, 0);
        }
        Blueprint bp = plugin.library().get(Objects.requireNonNull(templateId));
        if (bp == null) {
            throw new IllegalArgumentException("Nie ma szablonu '" + templateId + "'");
        }
        if (Bukkit.getWorld(Objects.requireNonNull(world)) == null) {
            throw new IllegalArgumentException("Nie ma świata '" + world + "'");
        }
        Placement placement = new Placement(world, originX, originY, originZ, RotationUtil.normalize(rotation));
        PasteService.Registered registered = plugin.paster().registerPlaced(bp, placement, instanceKey, true);
        return info(instanceKey, registered.instance(), registered.crates());
    }

    @Override
    public boolean isRegistered(String instanceKey) {
        return plugin.store().findByKey(instanceKey) != null;
    }

    @Override
    public void unregisterWorld(String world) {
        requireServerThread();
        for (StructureInstance instance : List.copyOf(plugin.store().inWorld(world))) {
            plugin.crates().clear(instance);
            plugin.protection().onRemoved(instance);
            plugin.store().remove(instance.id());
        }
        plugin.containers().removeWorld(world);
    }

    @Override
    public int setRoadsideContainers(String world, Collection<RoadsideSpot> spots) {
        requireServerThread();
        return plugin.containers().setRoadside(Objects.requireNonNull(world), List.copyOf(spots));
    }

    @Override
    public void claimWorld(String world) {
        plugin.autoGenerator().claimWorld(Objects.requireNonNull(world));
    }

    private static StructureInstanceInfo info(String key, StructureInstance instance, int crates) {
        return new StructureInstanceInfo(key, instance.id(), instance.structureId(), instance.world(),
                instance.centerX(), instance.centerZ(), crates);
    }

    private static void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Tę metodę API wolno wywołać tylko z wątku serwera");
        }
    }
}
