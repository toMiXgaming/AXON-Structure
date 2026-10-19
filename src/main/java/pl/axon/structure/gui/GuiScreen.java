package pl.axon.structure.gui;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Jeden ekran GUI (sklep, automat, wymiana...). Silnik rysuje pasek tytułu z przyciskami „Tryb skrzyni”
 * i „Zamknij” oraz podpowiedzi na dole, a ekran swoją treść w {@link #draw}. Wszystko woła się w wątku serwera.
 */
public abstract class GuiScreen {

    private boolean dirty = true;

    /** Napis na pasku tytułu (kody kolorów są pomijane). */
    public abstract String title();

    /** Treść ekranu w prostokącie {@link GuiLayout#CONTENT_X}..; przyciski rejestruje {@link GuiCanvas#button}. */
    protected abstract void draw(GuiCanvas canvas);

    /** Krótka informacja na pasku tytułu (np. saldo) albo null. */
    protected String info() {
        return null;
    }

    /** Podpowiedzi na dole ekranu: pary {klawisz, co robi}. */
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "wybierz"}, new String[]{"Scroll", "przewiń"},
                new String[]{"Shift", "zamknij"});
    }

    /** Co 2 ticki, póki ekran jest otwarty; true = narysuj go od nowa. */
    protected boolean tick() {
        return false;
    }

    /** Kółko myszy (zmiana slotu na hotbarze): +1 w dół listy, -1 w górę. */
    protected void scroll(int delta) {
    }

    /**
     * Ekran zniknął (Shift, przycisk, obrażenia, wyjście z serwera, inny ekran...). {@code toChest} = gracz wybrał
     * „Tryb skrzyni” i zaraz dostanie to samo okno jako skrzynię ({@link #openChest}).
     */
    protected void closed(Player player, boolean toChest) {
    }

    /** Otwiera to samo okno w trybie skrzyni; wołane, gdy ekranu już nie ma, a gracz stoi na swoim miejscu. */
    protected abstract void openChest(Player player);

    /** Narysuje ekran od nowa przy najbliższym ticku. */
    public final void redraw() {
        dirty = true;
    }

    final boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    final void render(GuiCanvas c, GuiCanvas.Click close, GuiCanvas.Click chest) {
        c.background();
        int m = GuiLayout.MARGIN;
        c.panel(m, m, GuiLayout.WIDTH - 2 * m, GuiLayout.HEADER_H);
        c.fill(m + 2, m + 2, 3, GuiLayout.HEADER_H - 4, GuiCanvas.ACCENT);
        int x = GuiLayout.WIDTH - m - 6 - 64;
        c.button("gui:zamknij", x, m + 5, 64, 18, "Zamknij", GuiCanvas.Style.DANGER, close);
        x -= 6 + 84;
        c.button("gui:skrzynia", x, m + 5, 84, 18, "Tryb skrzyni", GuiCanvas.Style.NEUTRAL, chest);
        String info = info();
        if (info != null && !info.isEmpty()) {
            int w = GuiCanvas.width(info) + 16;
            x -= 8 + w;
            c.inset(x, m + 5, w, 18);
            c.text(x + 8, m + 10, info, GuiCanvas.GOLD);
        }
        String title = GuiCanvas.plain(title()).toUpperCase(java.util.Locale.ROOT);
        int titleRoom = (x - 8 - (m + 14)) / 2;
        c.text(m + 14, m + 7, GuiCanvas.fit(title, titleRoom), GuiCanvas.TEXT, 2);

        draw(c);

        List<String[]> hints = hints();
        int width = 0;
        for (String[] hint : hints) {
            width += GuiCanvas.width(hint[0]) + 4 + GuiCanvas.width(hint[1]) + 14;
        }
        width += 16 - 14;
        int hx = (GuiLayout.WIDTH - width) / 2;
        c.panel(hx, GuiLayout.FOOTER_Y, width, GuiLayout.FOOTER_H);
        hx += 8;
        for (String[] hint : hints) {
            hx = c.text(hx, GuiLayout.FOOTER_Y + 6, hint[0], GuiCanvas.GOLD) + 4;
            hx = c.text(hx, GuiLayout.FOOTER_Y + 6, hint[1], GuiCanvas.TEXT_DIM) + 14;
        }
        c.drawTooltip();
    }
}
