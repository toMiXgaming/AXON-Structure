package pl.axon.structure.gui;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Niewidzialne świecące ramki z mapami, które istnieją tylko u jednego gracza (z AxonMap). */
final class FakeFrames {

    /** Kierunki w pakiecie stawiania bytów wiszących. */
    static final int FACING_DOWN = 0;
    static final int FACING_UP = 1;
    static final int FACING_NORTH = 2;
    static final int FACING_SOUTH = 3;
    static final int FACING_WEST = 4;
    static final int FACING_EAST = 5;

    private static final byte INVISIBLE = 0x20;
    private static final int INDEX_FLAGS = 0;
    private static final int INDEX_ITEM = 8;

    private FakeFrames() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void spawn(Player player, int entityId, int blockX, int blockY, int blockZ, int facing, ItemStack item) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(entityId, Optional.of(UUID.randomUUID()),
                EntityTypes.GLOW_ITEM_FRAME, new Vector3d(blockX + 0.5, blockY + 0.5, blockZ + 0.5),
                0f, 0f, 0f, facing, Optional.empty());
        List<EntityData> data = List.of(
                new EntityData(INDEX_FLAGS, EntityDataTypes.BYTE, INVISIBLE),
                new EntityData(INDEX_ITEM, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(entityId, data);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    static void destroy(Player player, int... entityIds) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerDestroyEntities(entityIds));
    }
}
