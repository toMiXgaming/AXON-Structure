package pl.axon.structure.oasis;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Automat jako ekran GUI: trzy bębny z rysowanymi symbolami, stawka, saldo, wynik i tabela wypłat. Grę losuje
 * i wypłaca {@link SlotMachine#spin}; ekran tylko pokazuje klatki, które dostaje co 2 ticki.
 */
final class SlotScreen extends OasisScreen implements SlotMachine.SpinView {

    private static final int TOP = GuiLayout.CONTENT_Y;
    private static final int H = GuiLayout.CONTENT_H;
    private static final int LEFT_X = GuiLayout.CONTENT_X;
    private static final int LEFT_W = 170;
    private static final int MID_X = LEFT_X + LEFT_W + 8;
    private static final int MID_W = 268;
    private static final int RIGHT_X = MID_X + MID_W + 8;
    private static final int RIGHT_W = GuiLayout.CONTENT_X + GuiLayout.CONTENT_W - RIGHT_X;
    private static final int REEL_W = 72;
    private static final int REEL_H = 150;
    private static final int REEL_GAP = 12;
    private static final int REEL_Y = TOP + 30;
    /** Odstęp między symbolami na taśmie bębna. */
    private static final int STEP = 52;

    private final SlotMachine slots;
    private SlotMachine.Symbol[] shown;
    private boolean[] stopped = {true, true, true};
    private SlotMachine.Symbol[] last;
    private double lastWin;
    private boolean spinning;
    private int frame;
    private String status;

    SlotScreen(OasisService oasis, SlotMachine slots, Player player) {
        super(oasis, player);
        this.slots = slots;
    }

    @Override
    public String title() {
        return "Automat";
    }

    @Override
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "zagraj"}, new String[]{"Shift", "zamknij"});
    }

    @Override
    protected int state() {
        return Objects.hash(info());
    }

    @Override
    protected void openChest(Player player) {
        slots.openChest(player, last, lastWin);
    }

    /** Płaci stawkę i rusza bębnami (z kliknięcia w automat albo przycisku „Zagraj”). */
    void spin() {
        if (spinning) {
            return;
        }
        SlotMachine.Spin spin = slots.spin(player, this);
        if (spin == null) {
            status = slots.active(player) != null ? "Bębny już się kręcą."
                    : "Gra kosztuje " + oasis.vault().format(slots.cost()) + ".";
        } else {
            spinning = true;
            status = null;
            last = null;
            stopped = new boolean[]{false, false, false};
        }
        redraw();
    }

    @Override
    public void frame(SlotMachine.Symbol[] shown, boolean[] stopped) {
        this.shown = shown.clone();
        this.stopped = stopped.clone();
        frame++;
        redraw();
    }

    @Override
    public void done(SlotMachine.Symbol[] result, double win) {
        spinning = false;
        last = result.clone();
        lastWin = win;
        shown = result.clone();
        stopped = new boolean[]{true, true, true};
        redraw();
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    protected void draw(GuiCanvas c) {
        drawGame(c);
        drawReels(c);
        drawPaytable(c);
    }

    private void drawGame(GuiCanvas c) {
        c.panel(LEFT_X, TOP, LEFT_W, H, "Gra");
        int x = LEFT_X + 10;
        int w = LEFT_W - 20;
        c.text(x, TOP + 28, "STAWKA", GuiCanvas.TEXT_MUTED);
        c.text(x, TOP + 40, GuiCanvas.fit(oasis.vault().format(slots.cost()), w / 2), GuiCanvas.GOLD, 2);
        c.text(x, TOP + 66, "SALDO", GuiCanvas.TEXT_MUTED);
        c.text(x, TOP + 78, GuiCanvas.fit(oasis.vault().format(oasis.vault().balance(player)), w), GuiCanvas.TEXT);
        c.fill(x, TOP + 94, w, 1, GuiCanvas.PANEL_LIGHT);
        int y = TOP + 102;
        String text = status != null ? status : spinning ? "Bębny się kręcą..."
                : last == null ? "Trzy takie same symbole płacą stawkę razy mnożnik z tabeli."
                : lastWin > 0 ? "Wygrana: " + oasis.vault().format(lastWin) + "!" : "Nic tym razem.";
        byte color = status != null ? GuiCanvas.RED : last != null && lastWin > 0 ? GuiCanvas.GREEN : GuiCanvas.TEXT_DIM;
        for (String line : GuiCanvas.wrap(text, w)) {
            c.text(x, y, line, color);
            y += 10;
        }
        int by = TOP + H - 44;
        c.button("zagraj", x, by, w, 34, spinning ? "Kręci się..." : "Zagraj za " + oasis.vault().format(slots.cost()),
                spinning ? GuiCanvas.Style.DISABLED : GuiCanvas.Style.PRIMARY, spinning ? null : right -> spin());
    }

    private void drawReels(GuiCanvas c) {
        c.panel(MID_X, TOP, MID_W, H, "Automat");
        List<SlotMachine.Symbol> all = slots.symbols();
        if (all.isEmpty()) {
            return;
        }
        int x0 = MID_X + (MID_W - (3 * REEL_W + 2 * REEL_GAP)) / 2;
        int mid = REEL_Y + REEL_H / 2;
        boolean[] winning = winning();
        for (int r = 0; r < 3; r++) {
            int rx = x0 + r * (REEL_W + REEL_GAP);
            c.inset(rx, REEL_Y, REEL_W, REEL_H);
            c.clip(rx + 1, REEL_Y + 1, REEL_W - 2, REEL_H - 2);
            SlotMachine.Symbol center = shown != null ? shown[r] : all.get(r % all.size());
            int index = Math.max(0, all.indexOf(center));
            if (spinning && !stopped[r]) {
                // taśma w ruchu: symbole przesunięte o kawałek, z poziomymi smugami
                int offset = (frame * 23 + r * 17) % STEP;
                for (int k = -2; k <= 2; k++) {
                    SlotMachine.Symbol s = all.get(Math.floorMod(index + k, all.size()));
                    drawSymbol(c, s.material(), rx + REEL_W / 2, mid + k * STEP + offset, 16);
                }
                for (int py = REEL_Y + 1; py < REEL_Y + REEL_H - 1; py += 3) {
                    c.fill(rx + 1, py, REEL_W - 2, 1, GuiCanvas.INSET);
                }
            } else {
                drawSymbol(c, center.material(), rx + REEL_W / 2, mid, 22);
                for (int k : new int[]{-1, 1}) {
                    SlotMachine.Symbol s = all.get(Math.floorMod(index + k, all.size()));
                    drawSymbol(c, s.material(), rx + REEL_W / 2, mid + k * STEP, 14);
                }
                fade(c, rx + 1, REEL_Y + 1, REEL_W - 2, mid - 30 - REEL_Y);
                fade(c, rx + 1, mid + 30, REEL_W - 2, REEL_Y + REEL_H - 1 - mid - 30);
            }
            c.unclip();
            if (winning[r]) {
                c.outline(rx - 2, REEL_Y - 2, REEL_W + 4, REEL_H + 4, 2, GuiCanvas.GOLD);
                c.outline(rx - 1, REEL_Y - 1, REEL_W + 2, REEL_H + 2, 1, GuiCanvas.GOLD);
            }
        }
        // linia wygranej
        for (int i = 0; i < 5; i++) {
            c.fill(x0 - 10 + i, mid - 4 + i, 1, 9 - 2 * i, GuiCanvas.ACCENT);
            int rx = x0 + 3 * REEL_W + 2 * REEL_GAP + 9 - i;
            c.fill(rx, mid - 4 + i, 1, 9 - 2 * i, GuiCanvas.ACCENT);
        }

        int by = REEL_Y + REEL_H + 12;
        int bw = MID_W - 28;
        c.inset(MID_X + 14, by, bw, 40);
        String banner = spinning ? "..." : last == null ? "POWODZENIA" : lastWin > 0
                ? "WYGRANA " + oasis.vault().format(lastWin) : "NIC TYM RAZEM";
        byte color = spinning ? GuiCanvas.TEXT_DIM : last != null && lastWin > 0 ? GuiCanvas.GOLD : GuiCanvas.TEXT_MUTED;
        c.centered(MID_X + MID_W / 2, by + 13, GuiCanvas.fit(banner, bw / 2 - 8), color, 2);
        int ty = by + 50;
        String rules = slots.pairMultiplier() > 0
                ? "Trzy takie same: stawka razy mnożnik. Dwa takie same: stawka razy " + number(slots.pairMultiplier()) + "."
                : "Trzy takie same: stawka razy mnożnik.";
        for (String line : GuiCanvas.wrap(rules, bw)) {
            c.centered(MID_X + MID_W / 2, ty, line, GuiCanvas.TEXT_DIM);
            ty += 10;
        }
    }

    /** Bębny, które dały wygraną (trójka albo para), żeby je podświetlić. */
    private boolean[] winning() {
        boolean[] out = new boolean[3];
        if (spinning || last == null || lastWin <= 0) {
            return out;
        }
        if (last[0] == last[1] && last[1] == last[2]) {
            return new boolean[]{true, true, true};
        }
        out[0] = last[0] == last[1] || last[0] == last[2];
        out[1] = last[1] == last[0] || last[1] == last[2];
        out[2] = last[2] == last[0] || last[2] == last[1];
        return out;
    }

    private void drawPaytable(GuiCanvas c) {
        c.panel(RIGHT_X, TOP, RIGHT_W, H, "Wypłaty");
        int x = RIGHT_X + 10;
        int w = RIGHT_W - 20;
        List<SlotMachine.Symbol> sorted = new ArrayList<>(slots.symbols());
        sorted.sort(Comparator.comparingDouble(SlotMachine.Symbol::multiplier).reversed());
        int y = TOP + 26;
        c.text(x, y, "3 TAKIE SAME", GuiCanvas.TEXT_MUTED);
        y += 14;
        int bottom = TOP + H - 30;
        for (SlotMachine.Symbol s : sorted) {
            if (y > bottom - 18) {
                break;
            }
            c.fill(x, y, w, 18, GuiCanvas.INSET);
            drawSymbol(c, s.material(), x + 10, y + 9, 6);
            String value = s.multiplier() > 0 ? "x" + number(s.multiplier()) : "pudło";
            c.text(x + 22, y + 6, GuiCanvas.fit(name(new org.bukkit.inventory.ItemStack(s.material())),
                    w - 28 - GuiCanvas.width(value)), GuiCanvas.TEXT);
            c.right(x + w - 4, y + 6, value, s.multiplier() > 0 ? GuiCanvas.GOLD : GuiCanvas.TEXT_MUTED);
            y += 20;
        }
        if (slots.pairMultiplier() > 0) {
            c.fill(x, bottom, w, 1, GuiCanvas.PANEL_LIGHT);
            c.text(x, bottom + 8, "2 takie same", GuiCanvas.TEXT_DIM);
            c.right(x + w - 4, bottom + 8, "x" + number(slots.pairMultiplier()), GuiCanvas.GOLD);
        }
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Przygasza fragment bębna kratką, żeby środkowy symbol się wyróżniał. */
    private static void fade(GuiCanvas c, int x, int y, int w, int h) {
        for (int py = y; py < y + h; py++) {
            for (int px = x + (py % 2); px < x + w; px += 2) {
                c.put(px, py, GuiCanvas.INSET);
            }
        }
    }

    // ------------------------------------------------------------------ symbole

    /**
     * Symbol rysowany z kształtów, bo serwer nie ma tekstur przedmiotów: kamienie szlachetne to romby, sztabki
     * to trapezy, węgiel to bryłka, krzaki to gałązki, a reszta to moneta z literą.
     */
    static void drawSymbol(GuiCanvas c, Material material, int cx, int cy, int size) {
        String key = material.name().toLowerCase(Locale.ROOT);
        int[] rgb = rgb(key);
        byte main = GuiCanvas.color(rgb[0], rgb[1], rgb[2]);
        byte light = GuiCanvas.color(mix(rgb[0], 255, 0.45), mix(rgb[1], 255, 0.45), mix(rgb[2], 255, 0.45));
        byte dark = GuiCanvas.color(mix(rgb[0], 0, 0.5), mix(rgb[1], 0, 0.5), mix(rgb[2], 0, 0.5));
        if (key.contains("diamond") || key.contains("emerald") || key.contains("amethyst") || key.contains("lapis")
                || key.contains("quartz")) {
            c.diamond(cx, cy, size, dark);
            c.diamond(cx, cy, size - Math.max(1, size / 6), main);
            c.diamond(cx - size / 3, cy - size / 3, Math.max(1, size / 4), light);
        } else if (key.endsWith("_ingot")) {
            int h = Math.max(2, size / 2);
            for (int dy = -h; dy <= h; dy++) {
                int half = (int) Math.round(size * 0.55 + (dy + h) * 0.45 * size / (2.0 * h));
                c.fill(cx - half, cy + dy, half * 2 + 1, 1, dy == -h ? light : dy == h ? dark : main);
            }
            c.fill(cx - size / 3, cy - h + 2, size / 2, Math.max(1, size / 8), light);
        } else if (key.contains("coal")) {
            c.circle(cx, cy, size - 1, dark);
            c.circle(cx + size / 6, cy + size / 6, size * 2 / 3, main);
            c.circle(cx - size / 3, cy - size / 3, Math.max(1, size / 5), light);
        } else if (key.contains("bush") || key.contains("stick") || key.contains("sapling")) {
            // pień i trzy suche gałązki
            int thick = Math.max(1, size / 8);
            c.fill(cx - thick / 2, cy - size / 2, thick + 1, size + size / 2, dark);
            for (int i = 0; i < size * 2 / 3; i++) {
                c.fill(cx - i, cy - i / 2, thick + 1, thick, main);
                c.fill(cx + i, cy - size / 3 - i / 2, thick + 1, thick, main);
                if (i < size / 2) {
                    c.fill(cx - i / 2, cy - size / 2 - i, thick, thick, light);
                }
            }
        } else {
            c.circle(cx, cy, size, dark);
            c.circle(cx, cy, size - Math.max(1, size / 6), main);
            if (size >= 10) {
                String letter = key.substring(0, 1).toUpperCase(Locale.ROOT);
                int scale = Math.max(1, size / 8);
                c.text(cx - GuiCanvas.width(letter, scale) / 2, cy - 4 * scale, letter, dark, scale);
            }
        }
    }

    private static int mix(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static int[] rgb(String key) {
        if (key.contains("diamond")) {
            return new int[]{92, 219, 213};
        } else if (key.contains("emerald")) {
            return new int[]{40, 200, 90};
        } else if (key.contains("gold")) {
            return new int[]{250, 210, 60};
        } else if (key.contains("netherite")) {
            return new int[]{86, 72, 72};
        } else if (key.contains("copper")) {
            return new int[]{224, 120, 80};
        } else if (key.contains("iron")) {
            return new int[]{216, 216, 216};
        } else if (key.contains("coal")) {
            return new int[]{56, 56, 56};
        } else if (key.contains("lapis")) {
            return new int[]{40, 70, 190};
        } else if (key.contains("redstone")) {
            return new int[]{210, 36, 36};
        } else if (key.contains("amethyst")) {
            return new int[]{160, 100, 220};
        } else if (key.contains("quartz")) {
            return new int[]{232, 226, 214};
        } else if (key.contains("bush") || key.contains("stick")) {
            return new int[]{148, 104, 52};
        }
        int[][] palette = {{214, 84, 64}, {80, 150, 214}, {120, 190, 80}, {220, 170, 60}, {170, 110, 200}, {80, 190, 180}};
        return palette[Math.floorMod(key.hashCode(), palette.length)];
    }
}
