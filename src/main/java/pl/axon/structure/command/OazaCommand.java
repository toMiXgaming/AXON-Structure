package pl.axon.structure.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.axon.structure.AxonStructurePlugin;

import java.util.List;
import java.util.Locale;

/** Komenda dla graczy: {@code /oaza} (co jest w oazie) i {@code /oaza ranking} (strzelnica). */
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
        sender.sendMessage("§a§lOaza Świt §7– strefa bez przemocy");
        sender.sendMessage("§eStanowiska wymiany §7– kliknij ladę, drugi gracz klika tę samą; wymiana rusza, gdy obaj zatwierdzą");
        sender.sendMessage("§eSklep §7– kliknij ladę albo handlarza (LPM kup, PPM sprzedaj)");
        sender.sendMessage("§eDepozyt §7– prywatny schowek przy ladzie depozytu");
        sender.sendMessage("§eTablica zleceń §7– przynieś towar, dostaniesz zapłatę");
        sender.sendMessage("§eAutomaty §7w barze, §eradiostacja §7– za opłatą zdradza, gdzie spadnie zrzut");
        sender.sendMessage("§eStrzelnica §7– strzelaj w tarcze z linii ognia; §e/" + label + " ranking §7– najlepsi w tygodniu");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && "ranking".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("ranking") : List.of();
    }
}
