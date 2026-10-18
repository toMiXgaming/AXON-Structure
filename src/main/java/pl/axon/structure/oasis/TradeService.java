package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bezpieczna wymiana na stanowisku w oazie. Pierwszy gracz klika ladę i czeka, drugi klika tę samą ladę: obaj
 * widzą jedno okno, każdy kładzie towar po swojej stronie. Wymiana rusza dopiero, gdy obaj zatwierdzą, a każda
 * zmiana w ofercie kasuje oba zatwierdzenia i odliczanie, więc nie da się podmienić towaru w ostatniej chwili.
 */
final class TradeService {

    private static final long WAIT_MS = 60_000;
    private static final int COUNTDOWN_TICKS = 3 * 20;
    private static final double MAX_DISTANCE = 8;
    private static final int LEFT_CONFIRM = 45;
    private static final int RIGHT_CONFIRM = 53;
    private static final int STATUS = 49;

    private record Waiting(UUID player, long since) {
    }

    private final OasisService oasis;
    /** Stanowisko (klucz punktu) → gracz, który czeka na drugą stronę. */
    private final Map<String, Waiting> waiting = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    TradeService(OasisService oasis) {
        this.oasis = oasis;
    }

    void use(Player player, OasisService.Point station) {
        if (busy(player)) {
            return;
        }
        String key = station.key();
        Waiting w = waiting.get(key);
        long now = System.currentTimeMillis();
        Player other = w == null ? null : Bukkit.getPlayer(w.player());
        if (other == null || now - w.since() > WAIT_MS || other.equals(player)
                || !other.getWorld().equals(player.getWorld())
                || other.getLocation().distance(station.center()) > MAX_DISTANCE) {
            waiting.put(key, new Waiting(player.getUniqueId(), now));
            player.sendMessage("§aStanowisko wymiany zajęte. §7Drugi gracz musi kliknąć tę samą ladę (masz minutę).");
            return;
        }
        waiting.remove(key);
        if (busy(other)) {
            player.sendMessage("§cTen gracz jest teraz zajęty.");
            return;
        }
        Trade trade = new Trade(other, player);
        trades.add(trade);
        trade.open();
    }

    private boolean busy(Player player) {
        for (Trade trade : trades) {
            if (trade.left.equals(player) || trade.right.equals(player)) {
                return true;
            }
        }
        return false;
    }

    /** Przy wyłączaniu pluginu: oddaje towar z otwartych wymian. */
    void cancelAll() {
        for (Trade trade : new ArrayList<>(trades)) {
            trade.cancel("§cWymiana przerwana.");
        }
        waiting.clear();
    }

    private static boolean leftSlot(int slot) {
        return slot < 45 && slot % 9 < 4;
    }

    private static boolean rightSlot(int slot) {
        return slot < 45 && slot % 9 > 4;
    }

    private final class Trade {
        final Player left;
        final Player right;
        final Menu menu;
        final Inventory inv;
        boolean leftOk;
        boolean rightOk;
        boolean finished;
        BukkitTask countdown;

        Trade(Player left, Player right) {
            this.left = left;
            this.right = right;
            menu = new Menu(6, "&8Wymiana: " + left.getName() + " ⇄ " + right.getName());
            inv = menu.getInventory();
            for (int slot = 0; slot < 54; slot++) {
                if (slot % 9 == 4 || slot >= 45) {
                    inv.setItem(slot, Menu.filler());
                }
            }
            menu.onClick(this::click).onDrag(this::drag).onClose(e -> cancel("§cWymiana przerwana: §f"
                    + e.getPlayer().getName() + " §czamknął okno."));
            refresh();
        }

        void open() {
            left.openInventory(inv);
            right.openInventory(inv);
            for (Player p : List.of(left, right)) {
                p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
            }
        }

        boolean isLeft(Object who) {
            return left.equals(who);
        }

        void click(InventoryClickEvent event) {
            if (finished) {
                event.setCancelled(true);
                return;
            }
            boolean me = isLeft(event.getWhoClicked());
            InventoryAction action = event.getAction();
            if (action == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true); // podwójne kliknięcie zbierałoby też z cudzej strony
                return;
            }
            boolean top = event.getClickedInventory() != null && event.getClickedInventory().equals(inv);
            if (!top) {
                if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    // shift+klik z własnego ekwipunku: tylko na swoją stronę
                    event.setCancelled(true);
                    ItemStack moving = event.getCurrentItem();
                    if (moving != null && !moving.getType().isAir()) {
                        ItemStack rest = putOnSide(me, moving.clone());
                        event.setCurrentItem(rest);
                        changed();
                    }
                }
                return;
            }
            int slot = event.getRawSlot();
            if (slot == (me ? LEFT_CONFIRM : RIGHT_CONFIRM)) {
                event.setCancelled(true);
                if (me) {
                    leftOk = !leftOk;
                } else {
                    rightOk = !rightOk;
                }
                refresh();
                return;
            }
            if (!(me ? leftSlot(slot) : rightSlot(slot))) {
                event.setCancelled(true);
                return;
            }
            String name = action.name();
            if (name.startsWith("PICKUP") || name.startsWith("PLACE") || name.startsWith("DROP")
                    || name.startsWith("HOTBAR") || name.equals("SWAP_WITH_CURSOR") || name.equals("MOVE_TO_OTHER_INVENTORY")) {
                changed();
            } else if (action != InventoryAction.NOTHING) {
                event.setCancelled(true);
            }
        }

        void drag(InventoryDragEvent event) {
            if (finished) {
                event.setCancelled(true);
                return;
            }
            boolean me = isLeft(event.getWhoClicked());
            for (int slot : event.getRawSlots()) {
                if (slot < 54 && !(me ? leftSlot(slot) : rightSlot(slot))) {
                    event.setCancelled(true);
                    return;
                }
            }
            changed();
        }

        /** Kładzie przedmiot na stronie gracza; zwraca to, co się nie zmieściło (albo null). */
        ItemStack putOnSide(boolean me, ItemStack item) {
            for (int slot = 0; slot < 45 && item != null; slot++) {
                if (!(me ? leftSlot(slot) : rightSlot(slot))) {
                    continue;
                }
                ItemStack there = inv.getItem(slot);
                if (there == null || there.getType().isAir()) {
                    inv.setItem(slot, item);
                    return null;
                }
                if (there.isSimilar(item) && there.getAmount() < there.getMaxStackSize()) {
                    int moved = Math.min(item.getAmount(), there.getMaxStackSize() - there.getAmount());
                    there.setAmount(there.getAmount() + moved);
                    item.setAmount(item.getAmount() - moved);
                    if (item.getAmount() <= 0) {
                        return null;
                    }
                }
            }
            return item;
        }

        /** Ktoś ruszył towar: zatwierdzenia i odliczanie od nowa (po ticku, gdy zmiana już się wykona). */
        void changed() {
            leftOk = false;
            rightOk = false;
            Bukkit.getScheduler().runTask(oasis.plugin(), this::refresh);
        }

        void refresh() {
            if (finished) {
                return;
            }
            inv.setItem(LEFT_CONFIRM, confirm(left, leftOk));
            inv.setItem(RIGHT_CONFIRM, confirm(right, rightOk));
            if (leftOk && rightOk) {
                if (countdown == null) {
                    startCountdown();
                }
            } else {
                if (countdown != null) {
                    countdown.cancel();
                    countdown = null;
                }
                inv.setItem(STATUS, Menu.icon(Material.PAPER, "&eUłóż towar po swojej stronie",
                        "&7Lewa strona: &f" + left.getName(), "&7Prawa strona: &f" + right.getName(),
                        "&7Wymiana ruszy, gdy obaj zatwierdzicie.", "&7Każda zmiana kasuje zatwierdzenia."));
            }
        }

        ItemStack confirm(Player who, boolean ok) {
            return ok
                    ? Menu.icon(Material.LIME_WOOL, "&a" + who.getName() + ": zatwierdzone", "&7Kliknij, żeby cofnąć.")
                    : Menu.icon(Material.RED_WOOL, "&c" + who.getName() + ": nie zatwierdzone", "&7Kliknij, gdy oferta pasuje.");
        }

        void startCountdown() {
            final int[] left3 = {COUNTDOWN_TICKS / 20};
            countdown = Bukkit.getScheduler().runTaskTimer(oasis.plugin(), () -> {
                if (!leftOk || !rightOk) {
                    // zmiana w ofercie w tym samym ticku – odświeżenie zaraz skasuje odliczanie
                    return;
                }
                if (left3[0] <= 0) {
                    countdown.cancel();
                    countdown = null;
                    complete();
                    return;
                }
                inv.setItem(STATUS, Menu.icon(Material.CLOCK, "&aWymiana za " + left3[0] + " s",
                        "&7Zmień coś, żeby przerwać."));
                for (Player p : List.of(this.left, right)) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
                }
                left3[0]--;
            }, 0L, 20L);
        }

        List<ItemStack> side(boolean me) {
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < 45; slot++) {
                if (me ? leftSlot(slot) : rightSlot(slot)) {
                    ItemStack item = inv.getItem(slot);
                    if (item != null && !item.getType().isAir()) {
                        items.add(item);
                    }
                }
            }
            return items;
        }

        void complete() {
            if (finished) {
                return;
            }
            List<ItemStack> fromLeft = side(true);
            List<ItemStack> fromRight = side(false);
            finish();
            Items.give(right, fromLeft.toArray(new ItemStack[0]));
            Items.give(left, fromRight.toArray(new ItemStack[0]));
            for (Player p : List.of(left, right)) {
                p.sendMessage("§aWymiana zakończona.");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
            }
            oasis.plugin().getLogger().info("Wymiana w oazie: " + left.getName() + " " + describe(fromLeft) + " ⇄ "
                    + right.getName() + " " + describe(fromRight));
        }

        String describe(List<ItemStack> items) {
            List<String> parts = new ArrayList<>();
            for (ItemStack item : items) {
                parts.add(item.getAmount() + "x " + item.getType().name().toLowerCase(java.util.Locale.ROOT));
            }
            return parts.isEmpty() ? "(nic)" : parts.toString();
        }

        void cancel(String message) {
            if (finished) {
                return;
            }
            List<ItemStack> fromLeft = side(true);
            List<ItemStack> fromRight = side(false);
            finish();
            Items.give(left, fromLeft.toArray(new ItemStack[0]));
            Items.give(right, fromRight.toArray(new ItemStack[0]));
            left.sendMessage(message);
            right.sendMessage(message);
        }

        /** Zamyka wymianę: czyści okno (przedmioty są już skopiowane) i zamyka je obu graczom. */
        void finish() {
            finished = true;
            trades.remove(this);
            if (countdown != null) {
                countdown.cancel();
                countdown = null;
            }
            inv.clear();
            if (!oasis.plugin().isEnabled()) {
                // wyłączanie pluginu: zadań już nie da się zaplanować, zamykamy od razu
                closeFor();
                return;
            }
            Bukkit.getScheduler().runTask(oasis.plugin(), this::closeFor);
        }

        private void closeFor() {
            for (Player p : List.of(left, right)) {
                if (p.getOpenInventory().getTopInventory().equals(inv)) {
                    p.closeInventory();
                }
            }
        }
    }
}
