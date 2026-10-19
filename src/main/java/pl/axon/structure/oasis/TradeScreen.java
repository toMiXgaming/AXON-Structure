package pl.axon.structure.oasis;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Wymiana jako ekran GUI: po lewej własny ekwipunek (klik kładzie towar na ladę), w środku własna oferta (klik
 * zabiera), po prawej oferta drugiego gracza, na dole zatwierdzenie i odliczanie. Każdy ruch idzie przez metody
 * {@link TradeService.Trade}, te same zasady co w oknie skrzyni: zmiana kasuje oba zatwierdzenia. Drugi gracz może
 * mieć ekran albo skrzynię.
 */
final class TradeScreen extends OasisScreen {

    private static final int TOP = GuiLayout.CONTENT_Y;
    private static final int PANEL_H = 254;
    private static final int INV_X = GuiLayout.CONTENT_X;
    private static final int INV_W = 200;
    private static final int MINE_X = INV_X + INV_W + 8;
    private static final int MINE_W = 200;
    private static final int OTHER_X = MINE_X + MINE_W + 8;
    private static final int OTHER_W = GuiLayout.CONTENT_X + GuiLayout.CONTENT_W - OTHER_X;
    private static final int ROWS_Y = TOP + 26;
    private static final int VISIBLE = (TOP + PANEL_H - 6 - ROWS_Y) / ROW_STEP;
    private static final int BAR_Y = TOP + PANEL_H + 6;
    private static final int BAR_H = GuiLayout.CONTENT_Y + GuiLayout.CONTENT_H - BAR_Y;

    final TradeService.Trade trade;
    private int invFirst;
    private int mineFirst;
    private int otherFirst;
    private double cursorX;
    private int lastState;

    TradeScreen(OasisService oasis, TradeService.Trade trade, Player player) {
        super(oasis, player);
        this.trade = trade;
    }

    private Player partner() {
        return trade.partner(player);
    }

    private boolean mine() {
        return trade.left.equals(player);
    }

    @Override
    public String title() {
        return "Wymiana z " + partner().getName();
    }

    @Override
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "cały stos"}, new String[]{"PPM", "1 sztuka"}, new String[]{"Scroll", "lista"},
                new String[]{"Shift", "przerwij"});
    }

    /** Wymiana zmienia się też z drugiej strony, więc sprawdzamy co 2 ticki. */
    @Override
    protected boolean tick() {
        int now = Objects.hash(Arrays.hashCode(trade.inv.getContents()),
                Arrays.hashCode(player.getInventory().getStorageContents()),
                trade.leftOk, trade.rightOk, trade.secondsLeft, trade.finished);
        boolean changed = now != lastState;
        lastState = now;
        return changed;
    }

    @Override
    protected void scroll(int delta) {
        if (cursorX < MINE_X) {
            invFirst = Math.max(0, invFirst + delta * 2);
        } else if (cursorX < OTHER_X) {
            mineFirst = Math.max(0, mineFirst + delta * 2);
        } else {
            otherFirst = Math.max(0, otherFirst + delta * 2);
        }
    }

    @Override
    protected void closed(Player player, boolean toChest) {
        if (!toChest) {
            // tak samo jak zamknięcie okna skrzyni: towar wraca do właścicieli
            trade.cancel("§cWymiana przerwana: §f" + player.getName() + " §czamknął okno.");
        }
    }

    @Override
    protected void openChest(Player player) {
        if (!trade.finished) {
            player.openInventory(trade.inv);
        }
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    protected void draw(GuiCanvas c) {
        cursorX = c.cursorX();
        boolean finished = trade.finished;

        List<Integer> own = new ArrayList<>();
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length && slot < 36; slot++) {
            if (storage[slot] != null && !storage[slot].getType().isAir()) {
                own.add(slot);
            }
        }
        c.panel(INV_X, TOP, INV_W, PANEL_H, "Twój ekwipunek");
        invFirst = clampFirst(invFirst, VISIBLE, own.size());
        for (int i = 0; i < VISIBLE && invFirst + i < own.size(); i++) {
            int slot = own.get(invFirst + i);
            ItemStack item = storage[slot];
            int y = ROWS_Y + i * ROW_STEP;
            if (itemRow(c, INV_X + 6, y, INV_W - 18, invFirst + i, item)) {
                List<String> lines = describe(item);
                lines.add("LPM: na ladę cały stos, PPM: 1 szt.");
                c.tooltip(lines);
            }
            if (!finished) {
                c.area("inv" + slot, INV_X + 6, y, INV_W - 18, ROW_H, right -> trade.offer(player, slot, right));
            }
        }
        empty(c, INV_X, own.isEmpty(), "Pusty ekwipunek.");
        scrollbar(c, INV_X + INV_W - 10, ROWS_Y, VISIBLE * ROW_STEP - 2, invFirst, VISIBLE, own.size());

        List<Integer> mine = trade.slots(mine());
        c.panel(MINE_X, TOP, MINE_W, PANEL_H, "Twoja oferta (" + mine.size() + ")");
        mineFirst = clampFirst(mineFirst, VISIBLE, mine.size());
        for (int i = 0; i < VISIBLE && mineFirst + i < mine.size(); i++) {
            int slot = mine.get(mineFirst + i);
            ItemStack item = trade.inv.getItem(slot);
            if (item == null) {
                continue;
            }
            int y = ROWS_Y + i * ROW_STEP;
            if (itemRow(c, MINE_X + 6, y, MINE_W - 18, mineFirst + i, item)) {
                List<String> lines = describe(item);
                lines.add("LPM: zabierz z lady.");
                c.tooltip(lines);
            }
            if (!finished) {
                c.area("lada" + slot, MINE_X + 6, y, MINE_W - 18, ROW_H, right -> trade.withdraw(player, slot));
            }
        }
        empty(c, MINE_X, mine.isEmpty(), "Kliknij przedmiot po lewej.");
        scrollbar(c, MINE_X + MINE_W - 10, ROWS_Y, VISIBLE * ROW_STEP - 2, mineFirst, VISIBLE, mine.size());

        List<Integer> theirs = trade.slots(!mine());
        c.panel(OTHER_X, TOP, OTHER_W, PANEL_H, "Oferta: " + partner().getName() + " (" + theirs.size() + ")");
        otherFirst = clampFirst(otherFirst, VISIBLE, theirs.size());
        for (int i = 0; i < VISIBLE && otherFirst + i < theirs.size(); i++) {
            int slot = theirs.get(otherFirst + i);
            ItemStack item = trade.inv.getItem(slot);
            if (item == null) {
                continue;
            }
            int y = ROWS_Y + i * ROW_STEP;
            if (itemRow(c, OTHER_X + 6, y, OTHER_W - 18, otherFirst + i, item)) {
                c.tooltip(describe(item));
            }
            // bez akcji, tylko po to, żeby dymek pojawiał się pod kursorem
            c.area("ich" + slot, OTHER_X + 6, y, OTHER_W - 18, ROW_H, right -> {
            });
        }
        empty(c, OTHER_X, theirs.isEmpty(), partner().getName() + " nic jeszcze nie dał.");
        scrollbar(c, OTHER_X + OTHER_W - 10, ROWS_Y, VISIBLE * ROW_STEP - 2, otherFirst, VISIBLE, theirs.size());

        drawBar(c, finished);
    }

    /** Wiersz z przedmiotem: nazwa i ilość; zwraca, czy kursor jest nad nim. */
    private static boolean itemRow(GuiCanvas c, int x, int y, int w, int index, ItemStack item) {
        boolean hover = row(c, x, y, w, index, false);
        String amount = "x" + item.getAmount();
        boolean enchanted = !item.getEnchantments().isEmpty();
        c.text(x + 6, y + 6, GuiCanvas.fit(name(item), w - 16 - GuiCanvas.width(amount)),
                enchanted ? GuiCanvas.BLUE : GuiCanvas.TEXT);
        c.right(x + w - 5, y + 6, amount, GuiCanvas.TEXT_DIM);
        return hover;
    }

    private static void empty(GuiCanvas c, int panelX, boolean empty, String text) {
        if (empty) {
            c.text(panelX + 12, ROWS_Y + 6, text, GuiCanvas.TEXT_MUTED);
        }
    }

    private void drawBar(GuiCanvas c, boolean finished) {
        int x = GuiLayout.CONTENT_X;
        int w = GuiLayout.CONTENT_W;
        c.panel(x, BAR_Y, w, BAR_H);
        boolean meOk = trade.confirmed(player);
        boolean themOk = trade.confirmed(partner());
        int by = BAR_Y + (BAR_H - 28) / 2;
        c.button("zatwierdz", x + 10, by, 170, 28, finished ? "Wymiana zamknięta" : meOk ? "Cofnij zatwierdzenie" : "Zatwierdź",
                finished ? GuiCanvas.Style.DISABLED : meOk ? GuiCanvas.Style.NEUTRAL : GuiCanvas.Style.PRIMARY,
                finished ? null : right -> trade.toggleConfirm(player));

        int sx = x + w - 190;
        light(c, sx, BAR_Y + 10, "Ty", meOk);
        light(c, sx, BAR_Y + 24, partner().getName(), themOk);

        int cx = x + 180 + (sx - x - 180) / 2;
        if (trade.secondsLeft >= 0 && !finished) {
            c.centered(cx, BAR_Y + 8, "WYMIANA ZA " + trade.secondsLeft + " S", GuiCanvas.GOLD, 2);
            c.centered(cx, BAR_Y + 28, "Zmień coś, żeby przerwać.", GuiCanvas.TEXT_DIM);
        } else {
            c.centered(cx, BAR_Y + 10, "Wymiana ruszy, gdy obaj zatwierdzicie.", GuiCanvas.TEXT);
            c.centered(cx, BAR_Y + 24, "Każda zmiana kasuje zatwierdzenia.", GuiCanvas.TEXT_DIM);
        }
    }

    private static void light(GuiCanvas c, int x, int y, String who, boolean ok) {
        c.fill(x, y, 8, 8, GuiCanvas.PANEL_EDGE);
        c.fill(x + 1, y + 1, 6, 6, ok ? GuiCanvas.GREEN : GuiCanvas.RED);
        c.text(x + 14, y, GuiCanvas.fit(who, 64) + (ok ? ": zatwierdzone" : ": nie zatwierdzone"),
                ok ? GuiCanvas.GREEN : GuiCanvas.TEXT_DIM);
    }
}
