package pl.axon.structure.gui;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pakietowa strona otwartego ekranu (z AxonMap), w wątkach sieci: kliknięcia w niby-ramki zamienia na kliknięcia
 * w ekran, chowa rękę i trzymane przedmioty, choć serwer dalej wysyła ich zmiany, i ustawia czas gry, po którym
 * shadery chowają HUD.
 */
final class GuiPackets extends PacketListenerAbstract {

    /** Machnięcie ręką tak szybko po kliknięciu w ramkę należy do tego kliknięcia. */
    private static final long SWING_AFTER_CLICK_NANOS = 250_000_000L;
    /** Trzymany prawy przycisk powtarza kliknięcie co kilka ticków; to nie może klikać drugi raz. */
    private static final long RIGHT_CLICK_REPEAT_NANOS = 400_000_000L;
    /** Tuż po otwarciu kliknięcia się pomija: to jeszcze ten klik, którym gracz otworzył okno. */
    private static final long OPEN_GRACE_NANOS = 350_000_000L;
    /** Numer drugiej ręki w pakiecie ustawiania ekwipunku gracza. */
    private static final int OFF_HAND_INDEX = 40;

    /** To, co wątki sieci muszą wiedzieć o jednym otwartym ekranie. */
    static final class State {
        final int playerEntityId;
        final long openedNanos = System.nanoTime();
        final Set<Integer> frameIds = ConcurrentHashMap.newKeySet();
        volatile boolean handHidden;
        volatile long lastClickNanos = System.nanoTime();
        volatile long lastRightClickNanos = System.nanoTime();

        State(int playerEntityId) {
            this.playerEntityId = playerEntityId;
        }
    }

    private final JavaPlugin plugin;
    private final PacketGui manager;
    /** AxonMap sam pilnuje czasu gry graczom bez otwartej mapy; wtedy nie ruszamy go drugi raz. */
    private final boolean axonMapPresent;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    GuiPackets(JavaPlugin plugin, PacketGui manager) {
        // po AxonMap (NORMAL), żeby przy otwartym ekranie nasz czas był ostatni
        super(PacketListenerPriority.HIGH);
        this.plugin = plugin;
        this.manager = manager;
        this.axonMapPresent = Bukkit.getPluginManager().getPlugin("AxonMap") != null;
    }

    State open(Player player) {
        State state = new State(player.getEntityId());
        states.put(player.getUniqueId(), state);
        return state;
    }

    void close(UUID playerId) {
        states.remove(playerId);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        UUID id = event.getUser().getUUID();
        State state = id == null ? null : states.get(id);
        if (state == null) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (!state.frameIds.contains(interact.getEntityId())) {
                return;
            }
            event.setCancelled(true);
            long now = System.nanoTime();
            state.lastClickNanos = now;
            if (now - state.openedNanos < OPEN_GRACE_NANOS) {
                return;
            }
            switch (interact.getAction()) {
                case ATTACK -> click(id, false);
                // Jeden prawy klik to INTERACT_AT i INTERACT, a potem to samo dla drugiej ręki, jeśli nic się
                // nie stało: reagujemy tylko na jeden z nich.
                case INTERACT -> {
                    if (interact.getHand() == InteractionHand.MAIN_HAND
                            && now - state.lastRightClickNanos > RIGHT_CLICK_REPEAT_NANOS) {
                        click(id, true);
                    }
                    if (interact.getHand() == InteractionHand.MAIN_HAND) {
                        state.lastRightClickNanos = now;
                    }
                }
                default -> {
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            event.setCancelled(true);
            long now = System.nanoTime();
            // machnięcie, które nie trafiło w ramkę (patrzenie obok), też liczy się jako lewy klik
            if (now - state.lastClickNanos > SWING_AFTER_CLICK_NANOS && now - state.openedNanos >= OPEN_GRACE_NANOS) {
                state.lastClickNanos = now;
                click(id, false);
            }
        }
    }

    private void click(UUID id, boolean right) {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                manager.click(player, right);
            }
        });
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        UUID id = event.getUser().getUUID();
        State state = id == null ? null : states.get(id);
        boolean hidden = state != null && state.handHidden;
        if (event.getPacketType() == PacketType.Play.Server.TIME_UPDATE) {
            if (!hidden && axonMapPresent) {
                return;
            }
            WrapperPlayServerTimeUpdate time = new WrapperPlayServerTimeUpdate(event);
            long age = hidden ? ClientView.openTime(time.getWorldAge()) : ClientView.outsideTime(time.getWorldAge());
            if (age != time.getWorldAge()) {
                time.setWorldAge(age);
                event.markForReEncode(true);
            }
            return;
        }
        if (!hidden) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event);
            if (metadata.getEntityId() != state.playerEntityId) {
                return;
            }
            for (EntityData data : metadata.getEntityMetadata()) {
                if (data.getIndex() == 0 && data.getValue() instanceof Byte flags) {
                    data.setValue((byte) (flags | ClientView.INVISIBLE));
                    event.markForReEncode(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);
            if (slot.getWindowId() == 0 && ClientView.isHiddenSlot(slot.getSlot())) {
                slot.setItem(ItemStack.EMPTY);
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems window = new WrapperPlayServerWindowItems(event);
            List<ItemStack> items = window.getItems();
            if (window.getWindowId() == 0 && items.size() > ClientView.OFF_HAND_SLOT) {
                List<ItemStack> emptied = new ArrayList<>(items);
                for (int slot = ClientView.FIRST_HOTBAR_SLOT; slot <= ClientView.OFF_HAND_SLOT; slot++) {
                    emptied.set(slot, ItemStack.EMPTY);
                }
                window.setItems(emptied);
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory inventory = new WrapperPlayServerSetPlayerInventory(event);
            if (inventory.getSlot() < 9 || inventory.getSlot() == OFF_HAND_INDEX) {
                inventory.setStack(ItemStack.EMPTY);
                event.markForReEncode(true);
            }
        }
    }
}
