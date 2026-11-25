package pl.axon.structure.gui;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Otwarty ekran jednego gracza. */
final class GuiSession {

    final UUID playerId;
    /** Ten sam obiekt gracza także po wyjściu z serwera (żeby ekran mógł po nim posprzątać). */
    final Player player;
    final String world;
    final ArmorStand seat;
    final Location origin;
    /** Po dwie ramki na kafelek i cztery, które zamykają pudełko wokół kamery. */
    final int[] entityIds = new int[GuiLayout.TILES * 2 + 4];
    GuiPackets.State packets;
    boolean framesSpawned;

    GuiScreen screen;
    String hover;
    List<GuiCanvas.Hit> hits = List.of();
    volatile byte[] pixels;

    GuiSession(Player player, ArmorStand seat, Location origin, GuiScreen screen) {
        this.playerId = player.getUniqueId();
        this.player = player;
        this.world = origin.getWorld() == null ? "" : origin.getWorld().getName();
        this.seat = seat;
        this.origin = origin.clone();
        this.screen = screen;
    }
}
