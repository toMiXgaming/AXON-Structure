package pl.axon.structure.oasis;

import org.bukkit.entity.Player;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiLayout;

import java.util.List;

/**
 * Sklep jako ekran GUI: lista towarów z ceną kupna i skupu, po prawej szczegóły wybranego towaru i przyciski
 * kupna i sprzedaży (×1 i ×4, jak shift w oknie skrzyni). Kupuje i sprzedaje {@link ShopService}.
 */
final class ShopScreen extends OasisScreen {

    private static final int LIST_X = GuiLayout.CONTENT_X;
    private static final int TOP = GuiLayout.CONTENT_Y;
    private static final int H = GuiLayout.CONTENT_H;
    private static final int LIST_W = 404;
    private static final int ROWS_Y = TOP + 38;
    private static final int VISIBLE = (TOP + H - 8 - ROWS_Y) / ROW_STEP;
    private static final int DETAIL_X = LIST_X + LIST_W + 8;
    private static final int DETAIL_W = GuiLayout.CONTENT_X + GuiLayout.CONTENT_W - DETAIL_X;
    /** Kolumny listy, od lewej krawędzi wierszy. */
    private static final int COL_PACK = 184;
    private static final int COL_BUY = 236;
    private static final int COL_SELL = 290;

    private final ShopService shop;
    private final List<ShopService.Offer> offers;
    private int first;
    private int selected;

    ShopScreen(OasisService oasis, ShopService shop, Player player, List<ShopService.Offer> offers) {
        super(oasis, player);
        this.shop = shop;
        this.offers = offers;
    }

    @Override
    public String title() {
        return shop.title();
    }

    @Override
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "wybierz / kup"}, new String[]{"Scroll", "lista"},
                new String[]{"Shift", "zamknij"});
    }

    @Override
    protected void scroll(int delta) {
        first = clampFirst(first + delta * 2, VISIBLE, offers.size());
    }

    @Override
    protected void openChest(Player player) {
        shop.openChest(player, offers);
    }

    private String money(double amount) {
        return oasis.vault().format(amount);
    }

    @Override
    protected void draw(GuiCanvas c) {
        c.panel(LIST_X, TOP, LIST_W, H, "Towary (" + offers.size() + ")");
        int x = LIST_X + 8;
        int w = LIST_W - 22;
        c.text(x + 6, TOP + 25, "TOWAR", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_PACK, TOP + 25, "PACZKA", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_BUY, TOP + 25, "KUPNO", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_SELL, TOP + 25, "SKUP", GuiCanvas.TEXT_MUTED);
        c.fill(x, TOP + 34, w, 1, GuiCanvas.PANEL_LIGHT);
        first = clampFirst(first, VISIBLE, offers.size());
        for (int i = 0; i < VISIBLE && first + i < offers.size(); i++) {
            int index = first + i;
            ShopService.Offer o = offers.get(index);
            int y = ROWS_Y + i * ROW_STEP;
            boolean hover = row(c, x, y, w, index, index == selected);
            c.area("towar" + index, x, y, w, ROW_H, right -> selected = index);
            c.text(x + 6, y + 6, GuiCanvas.fit(name(o.item()), COL_PACK - 12), GuiCanvas.TEXT);
            c.text(x + COL_PACK, y + 6, o.amount() + " szt.", GuiCanvas.TEXT_DIM);
            c.text(x + COL_BUY, y + 6, o.buy() > 0 ? GuiCanvas.fit(money(o.buy()), 50) : "-",
                    o.buy() > 0 ? GuiCanvas.GOLD : GuiCanvas.TEXT_MUTED);
            c.text(x + COL_SELL, y + 6, o.sell() > 0 ? GuiCanvas.fit(money(o.sell()), 40) : "-",
                    o.sell() > 0 ? GuiCanvas.TEXT_DIM : GuiCanvas.TEXT_MUTED);
            boolean buyable = o.buy() > 0;
            c.button("kup" + index, x + w - 46, y + 2, 42, 16, "Kup",
                    buyable ? GuiCanvas.Style.PRIMARY : GuiCanvas.Style.DISABLED,
                    buyable ? right -> shop.buy(player, o, 1) : null);
            if (hover) {
                c.tooltip(describe(o.item()));
            }
        }
        scrollbar(c, LIST_X + LIST_W - 10, ROWS_Y, VISIBLE * ROW_STEP - 2, first, VISIBLE, offers.size());
        drawDetails(c);
    }

    private void drawDetails(GuiCanvas c) {
        c.panel(DETAIL_X, TOP, DETAIL_W, H, "Szczegóły");
        if (selected < 0 || selected >= offers.size()) {
            return;
        }
        ShopService.Offer o = offers.get(selected);
        int x = DETAIL_X + 10;
        int w = DETAIL_W - 20;
        int y = TOP + 26;
        List<String> title = GuiCanvas.wrap(name(o.item()), w);
        for (int i = 0; i < Math.min(2, title.size()); i++) {
            c.text(x, y, title.get(i), GuiCanvas.GOLD);
            y += 11;
        }
        y += 4;
        int have = Items.count(player, o.item());
        y = stat(c, x, y, w, "Paczka", o.amount() + " szt.", GuiCanvas.TEXT);
        y = stat(c, x, y, w, "Kupno", o.buy() > 0 ? money(o.buy()) : "nie na sprzedaż",
                o.buy() > 0 ? GuiCanvas.GOLD : GuiCanvas.TEXT_MUTED);
        y = stat(c, x, y, w, "Skup", o.sell() > 0 ? money(o.sell()) : "nie skupuje",
                o.sell() > 0 ? GuiCanvas.TEXT : GuiCanvas.TEXT_MUTED);
        y = stat(c, x, y, w, "Masz", have + " szt.", have >= o.amount() ? GuiCanvas.GREEN : GuiCanvas.TEXT_DIM);
        c.fill(x, y + 2, w, 1, GuiCanvas.PANEL_LIGHT);
        y += 8;

        int buttons = TOP + H - 56;
        for (String line : wrapAll(details(o.item()), w)) {
            if (y > buttons - 14) {
                break;
            }
            c.text(x, y, line, GuiCanvas.TEXT_DIM);
            y += 10;
        }

        int bw = (w - 6) / 2;
        boolean buy = o.buy() > 0;
        boolean sell = o.sell() > 0 && have >= o.amount();
        c.button("kup1", x, buttons, bw, 20, "Kup x1", buy ? GuiCanvas.Style.PRIMARY : GuiCanvas.Style.DISABLED,
                buy ? right -> shop.buy(player, o, 1) : null);
        c.button("kup4", x + bw + 6, buttons, bw, 20, "Kup x4", buy ? GuiCanvas.Style.PRIMARY : GuiCanvas.Style.DISABLED,
                buy ? right -> shop.buy(player, o, 4) : null);
        c.button("sprzedaj1", x, buttons + 24, bw, 20, "Sprzedaj x1",
                sell ? GuiCanvas.Style.NEUTRAL : GuiCanvas.Style.DISABLED, sell ? right -> shop.sell(player, o, 1) : null);
        c.button("sprzedaj4", x + bw + 6, buttons + 24, bw, 20, "Sprzedaj x4",
                sell ? GuiCanvas.Style.NEUTRAL : GuiCanvas.Style.DISABLED, sell ? right -> shop.sell(player, o, 4) : null);
    }

    /** Wiersz „etykieta ..... wartość”; zwraca y następnego. */
    static int stat(GuiCanvas c, int x, int y, int w, String label, String value, byte color) {
        c.text(x, y, label, GuiCanvas.TEXT_DIM);
        c.right(x + w, y, GuiCanvas.fit(value, w - GuiCanvas.width(label) - 8), color);
        return y + 12;
    }
}
