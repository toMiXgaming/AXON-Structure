package pl.axon.structure.gui;

import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Obraz ekranu GUI (640 × 384 piksele w kolorach map) i to, czym się po nim rysuje: panele, przyciski, wiersze
 * list i tekst czcionką map. Przyciski rysowane w tym przebiegu są też zapamiętywane jako miejsca do kliknięcia,
 * więc gracz klika dokładnie to, co widzi. Wygląd w stylu Rusta: ciemne, ciepłe szarości, zielone przyciski
 * i pomarańczowy akcent.
 */
public final class GuiCanvas {

    /** Co robi kliknięcie; {@code right} to prawy przycisk myszy. */
    @FunctionalInterface
    public interface Click {
        void run(boolean right);
    }

    /** Miejsce do kliknięcia narysowane w tym przebiegu. */
    public record Hit(String key, int x, int y, int w, int h, Click action) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    public enum Style { PRIMARY, NEUTRAL, DANGER, DISABLED }

    private static final Map<Integer, Byte> COLORS = new ConcurrentHashMap<>();

    public static final byte BG = color(22, 22, 20);
    public static final byte BG_DOT = color(32, 31, 28);
    public static final byte PANEL = color(40, 39, 35);
    public static final byte PANEL_EDGE = color(14, 14, 12);
    public static final byte PANEL_LIGHT = color(64, 62, 56);
    public static final byte PANEL_HEAD = color(54, 52, 46);
    public static final byte INSET = color(28, 28, 25);
    public static final byte ROW = color(50, 49, 44);
    public static final byte ROW_ALT = color(44, 43, 39);
    public static final byte ROW_HOVER = color(70, 68, 60);
    public static final byte ROW_SELECTED = color(66, 86, 36);
    public static final byte TEXT = color(234, 228, 216);
    public static final byte TEXT_DIM = color(156, 150, 138);
    public static final byte TEXT_MUTED = color(106, 103, 96);
    public static final byte SHADOW = color(10, 10, 9);
    public static final byte ACCENT = color(206, 84, 40);
    public static final byte GOLD = color(232, 190, 72);
    public static final byte GREEN = color(140, 200, 80);
    public static final byte RED = color(222, 82, 64);
    public static final byte BLUE = color(96, 160, 214);
    public static final byte WHITE = color(255, 255, 255);

    private static final byte PRIMARY = color(98, 128, 44);
    private static final byte PRIMARY_HOVER = color(124, 160, 56);
    private static final byte PRIMARY_TOP = color(152, 188, 82);
    private static final byte PRIMARY_BOTTOM = color(60, 80, 28);
    private static final byte NEUTRAL = color(76, 73, 66);
    private static final byte NEUTRAL_HOVER = color(100, 96, 86);
    private static final byte NEUTRAL_TOP = color(124, 119, 108);
    private static final byte NEUTRAL_BOTTOM = color(48, 46, 41);
    private static final byte DANGER = color(160, 50, 38);
    private static final byte DANGER_HOVER = color(198, 66, 48);
    private static final byte DANGER_TOP = color(226, 106, 86);
    private static final byte DANGER_BOTTOM = color(100, 30, 22);
    private static final byte DISABLED = color(50, 49, 45);
    private static final byte MARKER_A = color(255, 255, 255);
    private static final byte MARKER_B = color(0, 0, 0);

    private final byte[] pixels = new byte[GuiLayout.WIDTH * GuiLayout.HEIGHT];
    private final List<Hit> hits = new ArrayList<>();
    private final double cursorX;
    private final double cursorY;
    private int clipX0;
    private int clipY0;
    private int clipX1 = GuiLayout.WIDTH;
    private int clipY1 = GuiLayout.HEIGHT;
    private List<String> tooltip;

    GuiCanvas(double cursorX, double cursorY) {
        this.cursorX = cursorX;
        this.cursorY = cursorY;
    }

    /** Najbliższy kolor z palety map (wynik zapamiętany). */
    @SuppressWarnings("deprecation")
    public static byte color(int r, int g, int b) {
        int key = (r << 16) | (g << 8) | b;
        return COLORS.computeIfAbsent(key, k -> MapPalette.matchColor(r, g, b));
    }

    byte[] pixels() {
        return pixels;
    }

    List<Hit> hits() {
        return hits;
    }

    public double cursorX() {
        return cursorX;
    }

    public double cursorY() {
        return cursorY;
    }

    /** Czy kursor jest nad tym prostokątem. */
    public boolean hover(int x, int y, int w, int h) {
        return cursorX >= x && cursorX < x + w && cursorY >= y && cursorY < y + h;
    }

    // ------------------------------------------------------------------ miejsca do kliknięcia

    /** Zapamiętuje miejsce do kliknięcia bez rysowania (np. cały wiersz listy). Późniejsze leżą wyżej. */
    public void area(String key, int x, int y, int w, int h, Click action) {
        if (action != null) {
            hits.add(new Hit(key, x, y, w, h, action));
        }
    }

    /** Przycisk z napisem; wyłączony (albo bez akcji) nie reaguje. Zwraca, czy kursor jest nad nim. */
    public boolean button(String key, int x, int y, int w, int h, String label, Style style, Click action) {
        boolean disabled = style == Style.DISABLED || action == null;
        boolean hover = !disabled && hover(x, y, w, h);
        byte fill;
        byte top;
        byte bottom;
        switch (disabled ? Style.DISABLED : style) {
            case PRIMARY -> {
                fill = hover ? PRIMARY_HOVER : PRIMARY;
                top = PRIMARY_TOP;
                bottom = PRIMARY_BOTTOM;
            }
            case DANGER -> {
                fill = hover ? DANGER_HOVER : DANGER;
                top = DANGER_TOP;
                bottom = DANGER_BOTTOM;
            }
            case NEUTRAL -> {
                fill = hover ? NEUTRAL_HOVER : NEUTRAL;
                top = NEUTRAL_TOP;
                bottom = NEUTRAL_BOTTOM;
            }
            default -> {
                fill = DISABLED;
                top = DISABLED;
                bottom = PANEL_EDGE;
            }
        }
        rounded(x, y, w, h, 2, PANEL_EDGE);
        fill(x + 1, y + 1, w - 2, h - 2, bottom);
        fill(x + 1, y + 1, w - 2, h - 3, top);
        fill(x + 2, y + 2, w - 4, h - 5, fill);
        if (hover) {
            outline(x, y, w, h, 2, TEXT);
        }
        String text = fit(label, w - 6);
        centered(x + w / 2, y + (h - 7) / 2, text, disabled ? TEXT_MUTED : TEXT);
        if (!disabled) {
            area(key, x, y, w, h, action);
        }
        return hover;
    }

    // ------------------------------------------------------------------ kształty

    /** Tło całego ekranu: prawie czarne, z rzadką kratką kropek. */
    public void background() {
        java.util.Arrays.fill(pixels, BG);
        for (int y = 2; y < GuiLayout.HEIGHT; y += 6) {
            for (int x = (y / 6) % 2 == 0 ? 2 : 5; x < GuiLayout.WIDTH; x += 6) {
                pixels[y * GuiLayout.WIDTH + x] = BG_DOT;
            }
        }
    }

    /** Ciemny panel z cieniem, obwódką i jaśniejszą krawędzią. */
    public void panel(int x, int y, int w, int h) {
        shadow(x, y, w, h);
        rounded(x, y, w, h, 3, PANEL_EDGE);
        rounded(x + 1, y + 1, w - 2, h - 2, 2, PANEL_LIGHT);
        fill(x + 2, y + 2, w - 4, h - 4, PANEL);
    }

    /** Panel z paskiem nagłówka i napisem (wielkimi literami, jak w Ruście). */
    public void panel(int x, int y, int w, int h, String title) {
        panel(x, y, w, h);
        fill(x + 2, y + 2, w - 4, 16, PANEL_HEAD);
        fill(x + 2, y + 18, w - 4, 1, PANEL_EDGE);
        fill(x + 2, y + 2, 2, 16, ACCENT);
        text(x + 9, y + 6, fit(title.toUpperCase(java.util.Locale.ROOT), w - 16), TEXT_DIM);
    }

    /** Wgłębione pole (np. okienko bębna albo pole liczby). */
    public void inset(int x, int y, int w, int h) {
        fill(x, y, w, h, INSET);
        fill(x, y, w, 1, PANEL_EDGE);
        fill(x, y, 1, h, PANEL_EDGE);
        fill(x, y + h - 1, w, 1, PANEL_LIGHT);
        fill(x + w - 1, y, 1, h, PANEL_LIGHT);
    }

    /** Pasek postępu: ułamek 0-1 wypełniony kolorem. */
    public void bar(int x, int y, int w, int h, double fraction, byte color) {
        inset(x, y, w, h);
        int filled = (int) Math.round((w - 2) * Math.max(0, Math.min(1, fraction)));
        fill(x + 1, y + 1, filled, h - 2, color);
    }

    /** Przyciemnia piksele pod i tuż wokół prostokąta, żeby panele wisiały nad tłem. */
    public void shadow(int x, int y, int w, int h) {
        for (int py = y + 2; py < y + h + 2; py++) {
            for (int px = x + 2; px < x + w + 2; px++) {
                if (px >= x + w || py >= y + h) {
                    darken(px, py);
                }
            }
        }
    }

    public void fill(int x0, int y0, int w, int h, byte color) {
        int fromY = Math.max(clipY0, y0);
        int toY = Math.min(clipY1, y0 + h);
        int fromX = Math.max(clipX0, x0);
        int toX = Math.min(clipX1, x0 + w);
        for (int y = fromY; y < toY; y++) {
            int row = y * GuiLayout.WIDTH;
            for (int x = fromX; x < toX; x++) {
                pixels[row + x] = color;
            }
        }
    }

    public void rounded(int x, int y, int w, int h, int r, byte color) {
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                if (inRounded(x, y, w, h, r, px, py)) {
                    put(px, py, color);
                }
            }
        }
    }

    public void outline(int x, int y, int w, int h, int r, byte color) {
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                if (inRounded(x, y, w, h, r, px, py) && !inRounded(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), px, py)) {
                    put(px, py, color);
                }
            }
        }
    }

    private static boolean inRounded(int x, int y, int w, int h, int r, int px, int py) {
        if (px < x || py < y || px >= x + w || py >= y + h) {
            return false;
        }
        if (r <= 0) {
            return true;
        }
        double cx = Math.max(x + r, Math.min(x + w - r, px + 0.5));
        double cy = Math.max(y + r, Math.min(y + h - r, py + 0.5));
        double dx = px + 0.5 - cx;
        double dy = py + 0.5 - cy;
        return dx * dx + dy * dy <= r * r + 0.5;
    }

    /** Romb (np. kamień szlachetny na bębnie automatu). */
    public void diamond(int cx, int cy, int radius, byte color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            fill(cx - half, cy + dy, half * 2 + 1, 1, color);
        }
    }

    public void circle(int cx, int cy, int radius, byte color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt(radius * radius + 0.5 - dy * dy));
            fill(cx - half, cy + dy, half * 2 + 1, 1, color);
        }
    }

    /**
     * Kolory map mają po cztery odcienie (mnożniki 180, 220, 255 i 135 dla odcienia 0-3); następny ciemniejszy
     * odcień przyciemnia piksel bez zmiany barwy.
     */
    private void darken(int x, int y) {
        if (x < clipX0 || y < clipY0 || x >= clipX1 || y >= clipY1) {
            return;
        }
        int i = y * GuiLayout.WIDTH + x;
        int value = pixels[i] & 0xFF;
        if (value < 4) {
            return;
        }
        int shade = switch (value & 3) {
            case 2 -> 1;
            case 1 -> 0;
            default -> 3;
        };
        pixels[i] = (byte) ((value & ~3) | shade);
    }

    public void put(int x, int y, byte color) {
        if (x >= clipX0 && y >= clipY0 && x < clipX1 && y < clipY1) {
            pixels[y * GuiLayout.WIDTH + x] = color;
        }
    }

    /** Rysowanie tylko w tym prostokącie (np. bęben automatu), aż do {@link #unclip()}. */
    public void clip(int x, int y, int w, int h) {
        clipX0 = Math.max(0, x);
        clipY0 = Math.max(0, y);
        clipX1 = Math.min(GuiLayout.WIDTH, x + w);
        clipY1 = Math.min(GuiLayout.HEIGHT, y + h);
    }

    public void unclip() {
        clipX0 = 0;
        clipY0 = 0;
        clipX1 = GuiLayout.WIDTH;
        clipY1 = GuiLayout.HEIGHT;
    }

    // ------------------------------------------------------------------ tekst

    /** Tekst z cieniem jak w Minecrafcie; zwraca x za tekstem. Kody kolorów są pomijane. */
    public int text(int x, int y, String text, byte color) {
        String plain = plain(text);
        draw(x + 1, y + 1, plain, SHADOW, 1);
        return draw(x, y, plain, color, 1);
    }

    /** Tekst powiększony {@code scale} razy (tytuły, wyniki). */
    public int text(int x, int y, String text, byte color, int scale) {
        String plain = plain(text);
        draw(x + scale, y + scale, plain, SHADOW, scale);
        return draw(x, y, plain, color, scale);
    }

    public void centered(int centerX, int y, String text, byte color) {
        text(centerX - width(text) / 2, y, text, color);
    }

    public void centered(int centerX, int y, String text, byte color, int scale) {
        text(centerX - width(text, scale) / 2, y, text, color, scale);
    }

    /** Tekst dosunięty prawą krawędzią do {@code right}. */
    public void right(int right, int y, String text, byte color) {
        text(right - width(text), y, text, color);
    }

    private int draw(int x, int y, String text, byte color, int scale) {
        for (char c : text.toCharArray()) {
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
            if (sprite == null) {
                x += 4 * scale;
                continue;
            }
            for (int row = 0; row < sprite.getHeight(); row++) {
                for (int col = 0; col < sprite.getWidth(); col++) {
                    if (sprite.get(row, col)) {
                        if (scale == 1) {
                            put(x + col, y + row, color);
                        } else {
                            fill(x + col * scale, y + row * scale, scale, scale, color);
                        }
                    }
                }
            }
            x += (sprite.getWidth() + 1) * scale;
        }
        return x;
    }

    public static int width(String text) {
        return width(text, 1);
    }

    public static int width(String text, int scale) {
        int width = 0;
        for (char c : plain(text).toCharArray()) {
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
            width += (sprite == null ? 3 : sprite.getWidth()) + 1;
        }
        return Math.max(0, width - 1) * scale;
    }

    /** Skraca tekst z „..” na końcu, żeby zmieścił się w {@code maxWidth} pikselach. */
    public static String fit(String text, int maxWidth) {
        String plain = plain(text);
        if (width(plain) <= maxWidth) {
            return plain;
        }
        int end = plain.length();
        while (end > 0 && width(plain.substring(0, end) + "..") > maxWidth) {
            end--;
        }
        return plain.substring(0, end) + "..";
    }

    /** Dzieli tekst na linie nie szersze niż {@code maxWidth} (po słowach). */
    public static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : plain(text).split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (width(next) <= maxWidth || line.isEmpty()) {
                line = new StringBuilder(next);
            } else {
                lines.add(fit(line.toString(), maxWidth));
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(fit(line.toString(), maxWidth));
        }
        return lines;
    }

    /**
     * Tekst bez kodów kolorów ({@code &a}, {@code §l}) i bez polskich ogonków, których czcionka map nie ma;
     * pozostałe nieznane znaki to „?”.
     */
    public static String plain(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < text.length()
                    && "0123456789abcdefklmnorxABCDEFKLMNORX".indexOf(text.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            if (c == '§') {
                continue;
            }
            int pl = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ".indexOf(c);
            char plainChar = pl >= 0 ? "acelnoszzACELNOSZZ".charAt(pl) : c;
            out.append(MinecraftFont.Font.getChar(plainChar) != null ? plainChar : '?');
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ dymek

    /** Dymek z opisem przy kursorze, rysowany na samym końcu (nad wszystkim). Pierwsza linia to tytuł. */
    public void tooltip(List<String> lines) {
        if (lines != null && !lines.isEmpty()) {
            tooltip = lines;
        }
    }

    void drawTooltip() {
        if (tooltip == null) {
            return;
        }
        int w = 0;
        for (String line : tooltip) {
            w = Math.max(w, width(line));
        }
        w = Math.min(260, w) + 12;
        int h = tooltip.size() * 10 + 8;
        int x = (int) cursorX + 12;
        int y = (int) cursorY + 8;
        if (x + w > GuiLayout.WIDTH - 4) {
            x = (int) cursorX - 12 - w;
        }
        if (y + h > GuiLayout.HEIGHT - 4) {
            y = GuiLayout.HEIGHT - 4 - h;
        }
        x = Math.max(4, x);
        y = Math.max(4, y);
        shadow(x, y, w, h);
        rounded(x, y, w, h, 2, PANEL_EDGE);
        fill(x + 1, y + 1, w - 2, h - 2, INSET);
        fill(x + 1, y + 1, 2, h - 2, ACCENT);
        for (int i = 0; i < tooltip.size(); i++) {
            text(x + 7, y + 5 + i * 10, fit(tooltip.get(i), w - 12), i == 0 ? GOLD : TEXT_DIM);
        }
    }

    // ------------------------------------------------------------------ znacznik dla shadera

    /** Znacznik w pierwszym i ostatnim wierszu każdej mapy; shader go wykrywa i zasłania sąsiednim wierszem. */
    void writeMarkers() {
        for (int tile = 0; tile < GuiLayout.TILES; tile++) {
            int row = (tile / GuiLayout.COLS) * GuiLayout.TILE * GuiLayout.WIDTH
                    + (tile % GuiLayout.COLS) * GuiLayout.TILE;
            for (int x = 0; x < 8; x++) {
                pixels[row + x] = x % 2 == 0 ? MARKER_A : MARKER_B;
            }
            for (int x = 0; x < 16; x++) {
                pixels[row + 8 + x] = x < tile ? MARKER_A : MARKER_B;
            }
            int lastRow = row + (GuiLayout.TILE - 1) * GuiLayout.WIDTH;
            for (int x = 0; x < 8; x++) {
                pixels[lastRow + x] = x % 2 == 0 ? MARKER_A : MARKER_B;
            }
        }
    }
}
