package pl.axon.structure.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.axon.structure.core.PasteService;
import pl.axon.structure.core.StructureDefinition;
import pl.axon.structure.core.StructureRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class StrukturyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("lista", "wklej", "cofnij", "pomoc");

    private final StructureRegistry registry;
    private final PasteService pasteService;

    public StrukturyCommand(StructureRegistry registry, PasteService pasteService) {
        this.registry = registry;
        this.pasteService = pasteService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "pomoc" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(sender);
            case "wklej" -> paste(sender, label, args);
            case "cofnij" -> undo(sender);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage("§6§lAXON Struktury");
        sender.sendMessage("§e/" + label + " lista §7– dostępne struktury");
        sender.sendMessage("§e/" + label + " wklej <nazwa> [ziarno] §7– wkleja strukturę przed tobą");
        sender.sendMessage("§e/" + label + " cofnij §7– cofa ostatnie wklejenie");
        sender.sendMessage("§7Struktura stanie przed tobą, frontem w twoją stronę. Ustaw się tam, gdzie ma być wjazd.");
    }

    private void list(CommandSender sender) {
        sender.sendMessage("§6Dostępne struktury:");
        for (StructureDefinition definition : registry.all()) {
            sender.sendMessage("§e" + definition.id() + " §f– " + definition.displayName()
                    + " §7(" + definition.description() + ")");
        }
    }

    private void paste(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTę komendę może wykonać tylko gracz.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /" + label + " wklej <nazwa> [ziarno]");
            return;
        }
        StructureDefinition definition = registry.get(args[1]);
        if (definition == null) {
            sender.sendMessage("§cNie ma struktury §e" + args[1] + "§c. Zobacz §e/" + label + " lista§c.");
            return;
        }
        if (pasteService.isBusy()) {
            sender.sendMessage("§cTrwa już wklejanie albo cofanie, poczekaj chwilę.");
            return;
        }
        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cZiarno musi być liczbą.");
                return;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }
        pasteService.paste(player, definition, seed);
    }

    private void undo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTę komendę może wykonać tylko gracz.");
            return;
        }
        if (pasteService.isBusy()) {
            sender.sendMessage("§cTrwa już wklejanie albo cofanie, poczekaj chwilę.");
            return;
        }
        if (!pasteService.undo(player)) {
            sender.sendMessage("§cNie masz nic do cofnięcia.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("wklej")) {
            for (StructureDefinition definition : registry.all()) {
                options.add(definition.id());
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.startsWith(prefix));
        return options;
    }
}
