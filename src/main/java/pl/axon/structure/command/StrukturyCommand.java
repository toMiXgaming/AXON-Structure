package pl.axon.structure.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.axon.structure.AxonStructurePlugin;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.Rotation;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.loot.CrateClass;
import pl.axon.structure.loot.CratePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StrukturyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("lista", "wklej", "generuj", "cofnij", "odswiez", "najblizsza", "usun", "klasy", "zrzut", "straznicy", "przeladuj", "pomoc");
    /** Odległość (w blokach) między graczem a frontem struktury przy {@code wklej}. */
    private static final int FRONT_DISTANCE = 3;

    private final AxonStructurePlugin plugin;

    public StrukturyCommand(AxonStructurePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "pomoc" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(sender);
            case "wklej" -> paste(sender, label, args);
            case "generuj" -> generate(sender, label, args);
            case "cofnij" -> undo(sender);
            case "odswiez" -> refresh(sender);
            case "najblizsza" -> nearest(sender);
            case "usun" -> forget(sender, args);
            case "klasy" -> classes(sender);
            case "straznicy" -> guards(sender, args);
            case "zrzut" -> sender.sendMessage(plugin.airdrops().isActive() || args.length < 2 || !args[1].equalsIgnoreCase("info")
                    ? plugin.airdrops().trigger(sender)
                    : "§eNastępny zrzut: §f" + plugin.airdrops().nextInfo() + "§e.");
            case "przeladuj" -> {
                plugin.reload();
                sender.sendMessage("§aPrzeładowano: " + plugin.library().all().size() + " struktur, "
                        + plugin.crates().loot().classes().size() + " klas skrzyń, "
                        + plugin.crates().loot().pools().size() + " pul.");
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage("§6§lAXON Struktury");
        sender.sendMessage("§e/" + label + " lista §7– wczytane struktury");
        sender.sendMessage("§e/" + label + " wklej <nazwa> §7– stawia strukturę przed tobą, na twojej wysokości");
        sender.sendMessage("§e/" + label + " generuj <nazwa> §7– stawia ją wokół ciebie, dopasowaną do terenu");
        sender.sendMessage("§e/" + label + " cofnij §7– cofa twoje ostatnie postawienie");
        sender.sendMessage("§e/" + label + " odswiez §7– od razu losuje skrzynie w najbliższej strukturze");
        sender.sendMessage("§e/" + label + " najblizsza §7– gdzie jest najbliższa postawiona struktura");
        sender.sendMessage("§e/" + label + " usun [id] §7– zapomina najbliższą strukturę: zdejmuje ochronę i skrzynie, bloki zostają");
        sender.sendMessage("§e/" + label + " klasy §7– klasy skrzyń i pule z configu");
        sender.sendMessage("§e/" + label + " zrzut [info] §7– zrzuca skrzynię z zaopatrzeniem teraz (info: o której następny)");
        sender.sendMessage("§e/" + label + " straznicy [postaw] §7– strażnicy NPC (postaw: od razu we wczytanych strukturach)");
        sender.sendMessage("§e/" + label + " przeladuj §7– wczytuje config i pliki struktur od nowa");
        sender.sendMessage("§7Z konsoli: §ewklej <nazwa> <świat> <x> <y> <z> [obrót 0-3]§7, "
                + "§egeneruj <nazwa> <świat> <x> <z> [obrót 0-3]");
    }

    private void list(CommandSender sender) {
        if (plugin.library().all().isEmpty()) {
            sender.sendMessage("§eBrak struktur. Wrzuć pliki .axs do §f"
                    + plugin.library().folder().getPath() + "§e i wpisz §f/struktury przeladuj§e.");
            return;
        }
        sender.sendMessage("§6Struktury:");
        for (Blueprint bp : plugin.library().all()) {
            sender.sendMessage("§e" + bp.id() + " §f– " + bp.name() + " §7(" + bp.width() + "×" + bp.depth() + "×"
                    + bp.height() + ", " + bp.spawnPoints().size() + " miejsc na skrzynie)");
        }
    }

    private Blueprint structure(CommandSender sender, String label, String[] args, String usage) {
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /" + label + " " + usage);
            return null;
        }
        Blueprint bp = plugin.library().get(args[1]);
        if (bp == null) {
            sender.sendMessage("§cNie ma struktury §e" + args[1] + "§c. Zobacz §e/" + label + " lista§c.");
            return null;
        }
        if (plugin.paster().isBusy()) {
            sender.sendMessage("§cTrwa już stawianie albo cofanie, poczekaj chwilę.");
            return null;
        }
        return bp;
    }

    private void paste(CommandSender sender, String label, String[] args) {
        Blueprint bp = structure(sender, label, args, "wklej <nazwa> [świat x y z [obrót]]");
        if (bp == null) {
            return;
        }
        Placement placement;
        if (args.length >= 6) {
            try {
                int steps = args.length >= 7 ? Math.floorMod(Integer.parseInt(args[6]), 4) : 0;
                World world = world(sender, args[2]);
                if (world == null) {
                    return;
                }
                int x = Integer.parseInt(args[3]);
                int y = Integer.parseInt(args[4]);
                int z = Integer.parseInt(args[5]);
                placement = Placement.anchored(world.getName(), x, y - bp.ground(), z, bp.width() / 2, bp.depth() / 2, steps);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cWspółrzędne i obrót muszą być liczbami.");
                return;
            }
        } else if (sender instanceof Player player) {
            Location l = player.getLocation();
            int steps = Rotation.stepsFromYaw(l.getYaw());
            placement = Placement.anchored(l.getWorld().getName(), l.getBlockX(), l.getBlockY() - 1 - bp.ground(), l.getBlockZ(),
                    bp.width() / 2, -FRONT_DISTANCE, steps);
        } else {
            sender.sendMessage("§cZ konsoli podaj: wklej <nazwa> <świat> <x> <y> <z> [obrót]");
            return;
        }
        plugin.paster().paste(bp, placement, sender, owner(sender), false, null);
    }

    private void generate(CommandSender sender, String label, String[] args) {
        Blueprint bp = structure(sender, label, args, "generuj <nazwa> [świat x z [obrót]]");
        if (bp == null) {
            return;
        }
        World world;
        int x;
        int z;
        int steps;
        if (args.length >= 5) {
            world = world(sender, args[2]);
            if (world == null) {
                return;
            }
            try {
                x = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
                steps = args.length >= 6 ? Math.floorMod(Integer.parseInt(args[5]), 4) : 0;
            } catch (NumberFormatException e) {
                sender.sendMessage("§cWspółrzędne i obrót muszą być liczbami.");
                return;
            }
        } else if (sender instanceof Player player) {
            Location l = player.getLocation();
            world = l.getWorld();
            x = l.getBlockX();
            z = l.getBlockZ();
            steps = Rotation.stepsFromYaw(l.getYaw());
        } else {
            sender.sendMessage("§cZ konsoli podaj: generuj <nazwa> <świat> <x> <z> [obrót]");
            return;
        }
        sender.sendMessage("§7Sprawdzam teren...");
        plugin.autoGenerator().tryPlace(world, bp, x, z, steps, false, sender, owner(sender), ok -> {
            if (!ok) {
                sender.sendMessage("§cNie udało się postawić struktury.");
            }
        });
    }

    private void undo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTę komendę może wykonać tylko gracz.");
            return;
        }
        if (plugin.paster().isBusy()) {
            sender.sendMessage("§cTrwa już stawianie albo cofanie, poczekaj chwilę.");
            return;
        }
        if (!plugin.paster().undo(player.getUniqueId(), sender)) {
            sender.sendMessage("§cNie masz nic do cofnięcia.");
        }
    }

    private void refresh(CommandSender sender) {
        StructureInstance instance = nearestTo(sender);
        if (instance == null) {
            return;
        }
        int placed = plugin.crates().refresh(instance, true);
        plugin.store().save();
        if (placed < 0) {
            sender.sendMessage("§cStruktura §e" + instance.structureId() + "§c nie jest wczytana.");
        } else {
            sender.sendMessage("§aWylosowano skrzynie w §e" + instance.structureId() + "§a: " + placed + " z "
                    + plugin.library().get(instance.structureId()).spawnPoints().size() + " miejsc.");
        }
    }

    private void nearest(CommandSender sender) {
        StructureInstance instance = nearestTo(sender);
        if (instance == null) {
            return;
        }
        Location l = ((Player) sender).getLocation();
        int distance = (int) Math.sqrt(instance.distanceSquared(l.getX(), l.getZ()));
        Blueprint shown = plugin.library().get(instance.structureId());
        int groundY = instance.placement().originY() + (shown == null ? 0 : shown.ground());
        sender.sendMessage("§aNajbliższa: §e" + instance.structureId() + " §7(id " + instance.id() + ")§a na §f" + instance.centerX() + " "
                + groundY + " " + instance.centerZ() + "§a (" + distance + " bloków"
                + (instance.automatic() ? ", wygenerowana automatycznie" : "") + ")");
    }

    private void forget(CommandSender sender, String[] args) {
        StructureInstance instance;
        if (args.length >= 2) {
            instance = plugin.store().get(args[1]);
            if (instance == null) {
                sender.sendMessage("§cNie ma struktury o id §e" + args[1] + "§c.");
                return;
            }
        } else {
            instance = nearestTo(sender);
            if (instance == null) {
                return;
            }
        }
        plugin.crates().clear(instance);
        plugin.guards().forget(instance.id());
        plugin.protection().onRemoved(instance);
        plugin.store().remove(instance.id());
        sender.sendMessage("§aZapomniano §e" + instance.structureId() + "§a (id " + instance.id()
                + "). Bloki zostały, ale nie są już chronione, a skrzynie się nie odnawiają.");
    }

    private void guards(CommandSender sender, String[] args) {
        if (!plugin.guards().available()) {
            sender.sendMessage("§cStrażnicy wymagają pluginów Citizens i Sentinel (ci.citizensnpcs.co). "
                    + "Bez nich miejsca strażników zostają puste.");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("postaw")) {
            int placed = plugin.guards().spawnAllLoaded();
            sender.sendMessage("§aStrażnicy: postawiono " + placed + ".");
            plugin.getLogger().info("Strażnicy: postawiono " + placed);
            return;
        }
        sender.sendMessage("§eStrażnicy: §f" + plugin.guards().activeCount() + " §estoi, miejsc w postawionych strukturach: §f"
                + plugin.guards().guardPoints() + "§e. Pojawiają się, gdy gracz podejdzie do struktury.");
    }

    private StructureInstance nearestTo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTę komendę może wykonać tylko gracz.");
            return null;
        }
        Location l = player.getLocation();
        StructureInstance instance = plugin.store().nearest(l.getWorld().getName(), l.getX(), l.getZ());
        if (instance == null) {
            sender.sendMessage("§cW tym świecie nie ma jeszcze żadnej struktury.");
        }
        return instance;
    }

    private void classes(CommandSender sender) {
        sender.sendMessage("§6Klasy skrzyń:");
        for (CrateClass crate : plugin.crates().loot().classes().values()) {
            sender.sendMessage("§e" + crate.id() + " §f– " + crate.displayName() + " §7(" + crate.kind().name()
                    .toLowerCase(Locale.ROOT) + ", " + crate.loot().size() + " pozycji lootu)");
        }
        sender.sendMessage("§6Pule:");
        for (CratePool pool : plugin.crates().loot().pools().values()) {
            StringBuilder line = new StringBuilder("§e" + pool.id() + "§7: ");
            int total = pool.weights().values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> e : pool.weights().entrySet()) {
                line.append(e.getKey()).append(' ')
                        .append(total == 0 ? 0 : Math.round(e.getValue() * 100.0 / total)).append("%, ");
            }
            sender.sendMessage(line.substring(0, line.length() - 2));
        }
    }

    private static World world(CommandSender sender, String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            sender.sendMessage("§cNie ma świata §e" + name + "§c.");
        }
        return world;
    }

    private static java.util.UUID owner(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("wklej") || args[0].equalsIgnoreCase("generuj"))) {
            for (Blueprint bp : plugin.library().all()) {
                options.add(bp.id());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("usun")) {
            for (StructureInstance instance : plugin.store().all()) {
                options.add(instance.id());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("wklej") || args[0].equalsIgnoreCase("generuj"))) {
            for (World world : Bukkit.getWorlds()) {
                options.add(world.getName());
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}
