package pl.axon.structure.gui;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Zmiany w tym, co widzi klient gracza przy otwartym ekranie (z AxonMap). Stan serwera się nie zmienia: gracz
 * tylko u siebie jest niewidzialny (co chowa rękę w pierwszej osobie), a hotbar i druga ręka są u niego puste
 * (co chowa trzymany przedmiot).
 */
final class ClientView {

    static final byte INVISIBLE = 0x20;
    /** Okno ekwipunku gracza: hotbar to sloty 36-44, druga ręka slot 45. */
    static final int FIRST_HOTBAR_SLOT = 36;
    static final int OFF_HAND_SLOT = 45;

    private ClientView() {
    }

    static boolean isHiddenSlot(int slot) {
        return slot >= FIRST_HOTBAR_SLOT && slot <= OFF_HAND_SLOT;
    }

    /** Chowa rękę i trzymane przedmioty. {@link GuiPackets} pilnuje, żeby zostały schowane, aż do {@link #show}. */
    static void hideHand(Player player) {
        sendFlags(player, (byte) (flags(player) | INVISIBLE));
        for (int slot = FIRST_HOTBAR_SLOT; slot <= OFF_HAND_SLOT; slot++) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerSetSlot(0, 0, slot, ItemStack.EMPTY));
        }
    }

    /** Wysyła z powrotem prawdziwe flagi i ekwipunek. */
    static void show(Player player) {
        sendFlags(player, flags(player));
        player.updateInventory();
    }

    /**
     * Wysyła bieżący czas. {@link GuiPackets} zamienia go graczom z otwartym ekranem na „ekran otwarty”, więc to
     * od razu chowa albo pokazuje HUD.
     */
    static void sendTime(Player player) {
        boolean ticking = !Boolean.FALSE.equals(player.getWorld().getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerTimeUpdate(player.getWorld().getGameTime(), player.getPlayerTime(), ticking));
    }

    /** Czas gry z tym samym dniem, którego reszta mówi shaderom, że ekran jest otwarty. */
    static long openTime(long gameTime) {
        return gameTime - Math.floorMod(gameTime, 24000L) + GuiLayout.HUD_TIME + 5;
    }

    /**
     * Wszystkich innych trzyma poza oknem „ekran otwarty”, także przez ticki, które klient liczy sam do
     * następnej aktualizacji czasu sekundę później.
     */
    static long outsideTime(long gameTime) {
        long rest = Math.floorMod(gameTime, 24000L);
        long from = GuiLayout.HUD_TIME - 60;
        long to = GuiLayout.HUD_TIME + GuiLayout.HUD_TIME_WINDOW;
        return rest >= from && rest < to ? gameTime + (to - rest) : gameTime;
    }

    static void rotate(Player player, float yaw, float pitch) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerRotation(yaw, pitch));
    }

    private static void sendFlags(Player player, byte flags) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(
                player.getEntityId(), List.of(new EntityData(0, EntityDataTypes.BYTE, flags))));
    }

    /** Bajt wspólnych flag bytu, taki jak wysłałby serwer. */
    private static byte flags(Player player) {
        int flags = 0;
        if (player.getFireTicks() > 0) {
            flags |= 0x01;
        }
        if (player.isSneaking()) {
            flags |= 0x02;
        }
        if (player.isSprinting()) {
            flags |= 0x08;
        }
        if (player.isSwimming()) {
            flags |= 0x10;
        }
        if (player.isInvisible()) {
            flags |= INVISIBLE;
        }
        if (player.isGlowing()) {
            flags |= 0x40;
        }
        if (player.isGliding()) {
            flags |= 0x80;
        }
        return (byte) flags;
    }
}
