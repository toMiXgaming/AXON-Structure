package pl.axon.structure.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.axon.structure.AxonStructurePlugin;
import pl.axon.structure.gui.ShaderGui;

import java.util.List;
import java.util.Locale;

/**
 * Komenda dla graczy: {@code /oaza} (co jest w oazie), {@code /oaza ranking} (strzelnica) i {@code /oaza gui}
 * (okna oazy na pełnym ekranie albo jako skrzynia).
 */
public final class OazaCommand implements CommandExecutor, TabCompleter {

    private final AxonStructurePlugin plugin;

    public OazaCommand(AxonStructurePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].toLowerCase(Locale.ROOT).equals("ranking")) {
            plugin.oasis().showRanking(sender);
            return true;
        }
        if (args.length > 0 && args[0].toLowerCase(Locale.ROOT).equals("gui")) {
            toggleGui(sender, label);
            return true;
        }
        sender.sendMessage("§a§lOaza Świt §7– strefa bez przemocy");
        sender.sendMessage("§eStanowiska wymiany §7– kliknij ladę, drugi gracz klika tę samą; wymiana rusza, gdy obaj zatwierdzą");
        sender.sendMessage("§eSklep §7– kliknij ladę albo handlarza (LPM kup, PPM sprzedaj)");
        sender.sendMessage("§eDepozyt §7– prywatny schowek przy ladzie depozytu");
        sender.sendMessage("§eTablica zleceń §7– przynieś towar, dostaniesz zapłatę");
        sender.sendMessage("§eAutomaty §7w barze, §eradiostacja §7– za opłatą zdradza, gdzie spadnie zrzut");
        sender.sendMessage("§eStrzelnica §7– strzelaj w tarcze z linii ognia; §e/" + label + " ranking §7– najlepsi w tygodniu");
        sender.sendMessage("§e/" + label + " gui §7– okna oazy na pełnym ekranie albo jako zwykła skrzynia");
        return true;
    }

    /** Gracz sam wybiera: ekran z shaderem albo skrzynia (wybór zapisany w gui.yml). */
    private void toggleGui(CommandSender sender, String label) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTylko dla graczy.");
            return;
        }
        ShaderGui gui = plugin.gui();
        if (gui.toggleChestMode(player)) {
            gui.close(player);
            sender.sendMessage("§eOkna oazy otworzą się teraz jako skrzynia. §7Wpisz §f/" + label + " gui§7, żeby wrócić do ekranu.");
            return;
        }
        String reason = gui.fallbackReason(player);
        if (reason == null) {
            sender.sendMessage("§aOkna oazy otworzą się na pełnym ekranie. §7Kursor to mysz, Shift zamyka, a przycisk"
                    + " §fTryb skrzyni §7otwiera to samo okno jako skrzynię.");
        } else {
            sender.sendMessage("§eWybrałeś ekran, ale na razie i tak będzie skrzynia: §7" + reason + ".");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        return List.of("ranking", "gui").stream().filter(s -> s.startsWith(typed)).toList();
    }
}
