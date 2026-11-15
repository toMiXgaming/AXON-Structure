package pl.axon.structure.command;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import pl.axon.structure.AxonStructurePlugin;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Po komendach, które masowo usuwają encje ({@code /killall}, {@code /remove}, {@code /butcher}, {@code /kill}),
 * stawia od nowa modele mebli i pojemników w strukturach. Bez tego w strukturach zostają same niewidzialne
 * bariery, a łóżka i meble znikają, dopóki ktoś nie wpisze {@code /struktury meble wszystkie}.
 */
public final class EntityWipeListener implements Listener {

    private static final Set<String> WIPING = Set.of("killall", "ekillall", "remove", "eremove", "butcher",
            "ebutcher", "mobkill", "emobkill", "kill");

    private final AxonStructurePlugin plugin;
    private boolean scheduled;

    public EntityWipeListener(AxonStructurePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        check(event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        check(event.getCommand());
    }

    /** Czy komenda (z ukośnikiem albo bez, z przestrzenią nazw pluginu albo bez) usuwa encje hurtem. */
    static boolean wipesEntities(String command) {
        String label = command.strip();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        int space = label.indexOf(' ');
        if (space >= 0) {
            label = label.substring(0, space);
        }
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        label = label.toLowerCase(Locale.ROOT);
        if (label.equals("kill")) {
            return command.contains("@e"); // /kill bez @e zabija gracza, nie encje
        }
        return WIPING.contains(label);
    }

    private void check(String command) {
        if (scheduled || !wipesEntities(command)) {
            return;
        }
        scheduled = true;
        // Komenda wykonuje się po tym zdarzeniu, a encje znikają w tym samym ticku. Czekamy chwilę dłużej.
        plugin.getServer().getScheduler().runTaskLater(plugin, this::restore, 5L);
    }

    private void restore() {
        scheduled = false;
        if (plugin.paster().isBusy()) {
            return; // wklejanie lub cofanie samo stawia modele; nie mieszamy się w to
        }
        int models = 0;
        int structures = 0;
        for (StructureInstance instance : List.copyOf(plugin.store().all())) {
            Blueprint bp = plugin.library().get(instance.structureId());
            if (bp == null) {
                continue;
            }
            int placed = plugin.paster().restoreProps(instance, bp);
            if (placed > 0) {
                models += placed;
                structures++;
            }
        }
        int containers = plugin.containers().respawnMissing();
        if (models > 0 || containers > 0) {
            plugin.getLogger().info("Encje usunięte komendą: modele mebli od nowa " + models + " w " + structures
                    + " strukturach, pojemniki " + containers + ".");
        }
    }
}
