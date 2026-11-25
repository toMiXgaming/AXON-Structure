package pl.axon.structure.gui;

import org.bukkit.entity.Player;

import java.util.function.Predicate;

/**
 * Część GUI, która wysyła pakiety (PacketEvents). Bez typów PacketEvents w sygnaturach, żeby {@link ShaderGui}
 * dało się wczytać i bez tego pluginu.
 */
interface GuiBackend {

    /** Pokazuje ekran (albo podmienia otwarty); false, gdy gracza nie da się teraz posadzić przed ekranem. */
    boolean open(Player player, GuiScreen screen);

    boolean isOpen(Player player);

    GuiScreen screen(Player player);

    /** Zamyka ekran, jeśli pasuje do warunku. */
    void closeIf(Player player, Predicate<GuiScreen> which);

    void closeAll();

    void disable();
}
