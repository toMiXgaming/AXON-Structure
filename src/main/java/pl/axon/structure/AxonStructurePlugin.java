package pl.axon.structure;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.axon.structure.command.StrukturyCommand;
import pl.axon.structure.core.PasteService;
import pl.axon.structure.core.StructureRegistry;
import pl.axon.structure.structures.GasStationStructure;

public final class AxonStructurePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        StructureRegistry registry = new StructureRegistry();
        registry.register(new GasStationStructure());

        StrukturyCommand command = new StrukturyCommand(registry, new PasteService(this));
        PluginCommand pluginCommand = getCommand("struktury");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getLogger().info("Załadowano " + registry.all().size() + " struktur(y).");
    }
}
