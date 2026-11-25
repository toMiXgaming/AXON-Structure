package pl.axon.structure.oasis;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Towar pasera jako ekran GUI: kilka drogich ofert z ceną i stanem, szczegóły wybranej i przycisk kupna.
 * Kupuje {@link BlackMarketService#buy}, więc limity sztuk i ceny są te same co w oknie skrzyni.
 */
final class BlackMarketScreen extends OasisScreen {

    private static final int LIST_X = GuiLayout.CONTENT_X;
    private static final int TOP = GuiLayout.CONTENT_Y;
    private static final int H = GuiLayout.CONTENT_H;
    private static final int LIST_W = 404;
    private static final int ROWS_Y = TOP + 38;
    private static final int DETAIL_X = LIST_X + LIST_W + 8;
    private static final int DETAIL_W = GuiLayout.CONTENT_X + GuiLayout.CONTENT_W - DETAIL_X;
    private static final int COL_AMOUNT = 184;
    private static final int COL_PRICE = 228;
    private static final int COL_STOCK = 290;

    private final BlackMarketService market;
    private final List<Integer> offers;
    /** Przedmioty ofert (null: towar nie dojechał, np. brak broni w QualityArmory). */
    private final List<ItemStack> items = new ArrayList<>();
    private int selected;

    BlackMarketScreen(OasisService oasis, BlackMarketService market, Player player, List<Integer> offers) {
        super(oasis, player);
        this.market = market;
        this.offers = offers;
        for (int index : offers) {
            BlackMarketService.Offer offer = market.offer(index);
            items.add(Items.create(offer.spec(), offer.amount()));
        }
    }

    @Override
    public String title() {
        return market.title();
    }

    @Override
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "wybierz / kup"}, new String[]{"Shift", "zamknij"});
    }

    @Override
    protected int state() {
        int sold = 0;
        for (int index : offers) {
            sold = sold * 31 + market.sold(index);
        }
        return Objects.hash(super.state(), sold, market.untilRefresh() / 60_000L);
    }

    @Override
    protected void openChest(Player player) {
        market.openChest(player, offers);
    }

    private String stock(int index) {
        BlackMarketService.Offer offer = market.offer(index);
        return offer.stock() <= 0 ? "bez limitu" : Math.max(0, offer.stock() - market.sold(index)) + " szt.";
    }

    private boolean soldOut(int index) {
        BlackMarketService.Offer offer = market.offer(index);
        return offer.stock() > 0 && market.sold(index) >= offer.stock();
    }

    private String refresh() {
        long left = market.untilRefresh();
        return (left / 3_600_000L) + " h " + (left / 60_000L % 60) + " min";
    }

    @Override
    protected void draw(GuiCanvas c) {
        c.panel(LIST_X, TOP, LIST_W, H, "Towar spod lady");
        int x = LIST_X + 8;
        int w = LIST_W - 16;
        c.text(x + 6, TOP + 25, "TOWAR", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_AMOUNT, TOP + 25, "ILOŚĆ", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_PRICE, TOP + 25, "CENA", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_STOCK, TOP + 25, "STAN", GuiCanvas.TEXT_MUTED);
        c.fill(x, TOP + 34, w, 1, GuiCanvas.PANEL_LIGHT);
        for (int i = 0; i < offers.size(); i++) {
            int index = offers.get(i);
            int pos = i;
            BlackMarketService.Offer offer = market.offer(index);
            ItemStack item = items.get(i);
            int y = ROWS_Y + i * ROW_STEP;
            boolean hover = row(c, x, y, w, i, i == selected);
            c.area("towar" + i, x, y, w, ROW_H, right -> selected = pos);
            if (item == null) {
                c.text(x + 6, y + 6, "Towar nie dojechał", GuiCanvas.RED);
                continue;
            }
            c.text(x + 6, y + 6, GuiCanvas.fit(name(item), COL_AMOUNT - 12), GuiCanvas.TEXT);
            c.text(x + COL_AMOUNT, y + 6, offer.amount() + " szt.", GuiCanvas.TEXT_DIM);
            c.text(x + COL_PRICE, y + 6, GuiCanvas.fit(oasis.vault().format(offer.price()), 58), GuiCanvas.GOLD);
            boolean out = soldOut(index);
            c.text(x + COL_STOCK, y + 6, out ? "brak" : GuiCanvas.fit(stock(index), 46),
                    out ? GuiCanvas.RED : GuiCanvas.TEXT_DIM);
            c.button("kup" + i, x + w - 46, y + 2, 42, 16, "Kup", out ? GuiCanvas.Style.DISABLED : GuiCanvas.Style.PRIMARY,
                    out ? null : right -> market.buy(player, index));
            if (hover) {
                c.tooltip(describe(item));
            }
        }
        int y = ROWS_Y + Math.max(offers.size(), 1) * ROW_STEP + 8;
        c.text(x + 6, y, "Nowy towar za " + refresh() + ".", GuiCanvas.TEXT_DIM);
        c.text(x + 6, y + 12, "Limit sztuk jest wspólny dla całego serwera.", GuiCanvas.TEXT_MUTED);
        drawDetails(c);
    }

    private void drawDetails(GuiCanvas c) {
        c.panel(DETAIL_X, TOP, DETAIL_W, H, "Szczegóły");
        if (selected < 0 || selected >= offers.size() || items.get(selected) == null) {
            return;
        }
        int index = offers.get(selected);
        BlackMarketService.Offer offer = market.offer(index);
        ItemStack item = items.get(selected);
        int x = DETAIL_X + 10;
        int w = DETAIL_W - 20;
        int y = TOP + 26;
        List<String> title = GuiCanvas.wrap(name(item), w);
        for (int i = 0; i < Math.min(2, title.size()); i++) {
            c.text(x, y, title.get(i), GuiCanvas.GOLD);
            y += 11;
        }
        y += 4;
        y = ShopScreen.stat(c, x, y, w, "Ilość", offer.amount() + " szt.", GuiCanvas.TEXT);
        y = ShopScreen.stat(c, x, y, w, "Cena", oasis.vault().format(offer.price()), GuiCanvas.GOLD);
        boolean out = soldOut(index);
        y = ShopScreen.stat(c, x, y, w, "Na stanie", out ? "wyprzedane" : stock(index),
                out ? GuiCanvas.RED : GuiCanvas.TEXT);
        y = ShopScreen.stat(c, x, y, w, "Nowy towar", "za " + refresh(), GuiCanvas.TEXT_DIM);
        c.fill(x, y + 2, w, 1, GuiCanvas.PANEL_LIGHT);
        y += 8;
        int button = TOP + H - 32;
        for (String line : wrapAll(details(item), w)) {
            if (y > button - 14) {
                break;
            }
            c.text(x, y, line, GuiCanvas.TEXT_DIM);
            y += 10;
        }
        c.button("kup", x, button, w, 22, out ? "Wyprzedane" : "Kup za " + oasis.vault().format(offer.price()),
                out ? GuiCanvas.Style.DISABLED : GuiCanvas.Style.PRIMARY, out ? null : right -> market.buy(player, index));
    }
}
