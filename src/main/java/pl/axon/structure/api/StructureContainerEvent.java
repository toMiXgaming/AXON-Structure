package pl.axon.structure.api;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Otwarcie albo dwie fazy jednej operacji pobrania z pojemnika struktury. */
public final class StructureContainerEvent extends Event {
    public enum Phase { OPEN, BEFORE_TAKE, HELD, AFTER_TAKE }

    private static final HandlerList HANDLERS = new HandlerList();
    private final String structureId;
    private final Player player;
    private final Location location;
    private final Phase phase;
    private final UUID transaction;
    private final int removed;

    public StructureContainerEvent(String structureId, Player player, Location location, Phase phase,
                                   UUID transaction, int removed) {
        this.structureId = structureId;
        this.player = player;
        this.location = location.clone();
        this.phase = phase;
        this.transaction = transaction;
        this.removed = removed;
    }

    public String getStructureId() { return structureId; }
    public Player getPlayer() { return player; }
    public Location getLocation() { return location.clone(); }
    public Phase getPhase() { return phase; }
    public UUID getTransaction() { return transaction; }
    public int getRemoved() { return removed; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
