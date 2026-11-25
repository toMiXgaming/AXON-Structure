package pl.axon.structure.gui;

/**
 * Rozmiar ekranu GUI: 5 × 3 mapy po 128 pikseli. Ten sam układ i te same znaczniki co w AxonMap, więc shadery
 * z paczki zasobów (rendertype_text, position_tex_color) rysują go na cały ekran bez żadnych zmian.
 * Musi się zgadzać z AXON_COLS / AXON_ROWS w shaderze.
 */
public final class GuiLayout {

    public static final int COLS = 5;
    public static final int ROWS = 3;
    public static final int TILES = COLS * ROWS;
    public static final int TILE = 128;
    public static final int WIDTH = COLS * TILE;
    public static final int HEIGHT = ROWS * TILE;

    /** Piksele 0-23 pierwszego wiersza każdej mapy to znacznik, którego szuka shader. */
    public static final int MARKER_WIDTH = 24;

    /**
     * O ile stopni trzeba obrócić głowę, żeby kursor przejechał cały ekran. Przy otwarciu gracz patrzy na
     * yaw 0, pitch 0. Musi się zgadzać z AXON_YAW_RANGE / AXON_PITCH_RANGE w shaderze.
     */
    public static final double CURSOR_YAW_RANGE = 180.0;
    public static final double CURSOR_PITCH_RANGE = 100.0;

    /**
     * Przy otwartym ekranie klient gracza dostaje czas gry, którego reszta z 24000 leży w
     * [HUD_TIME, HUD_TIME + HUD_TIME_WINDOW). Shadery GUI widzą go w GameTime i chowają hotbar, serduszka
     * i celownik. Musi się zgadzać z AXON_HUD_TIME / AXON_HUD_WINDOW w shaderach.
     */
    public static final long HUD_TIME = 12000;
    public static final long HUD_TIME_WINDOW = 60;

    /** Pasek tytułu u góry, podpowiedzi na dole, a między nimi miejsce na treść ekranu. */
    public static final int MARGIN = 8;
    public static final int HEADER_H = 28;
    public static final int CONTENT_X = MARGIN;
    public static final int CONTENT_Y = MARGIN + HEADER_H + 8;
    public static final int CONTENT_W = WIDTH - 2 * MARGIN;
    public static final int FOOTER_H = 18;
    public static final int FOOTER_Y = HEIGHT - MARGIN - FOOTER_H;
    public static final int CONTENT_H = FOOTER_Y - 6 - CONTENT_Y;

    private GuiLayout() {
    }
}
