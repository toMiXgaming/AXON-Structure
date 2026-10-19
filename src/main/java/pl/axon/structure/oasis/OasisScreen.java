package pl.axon.structure.oasis;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Wspólne dla ekranów GUI oazy: saldo z Vaulta na pasku tytułu, odświeżanie, gdy coś się zmieni poza ekranem
 * (saldo, ekwipunek), i rysowanie list. Przedmiotów nie da się narysować jako ikon (serwer nie ma tekstur),
 * więc listy pokazują nazwy, ilości i ceny, a opis jest w dymku.
 */
abstract class OasisScreen extends GuiScreen {

    /** Wysokość wiersza listy i odstęp między wierszami. */
    static final int ROW_H = 20;
    static final int ROW_STEP = 22;

    final OasisService oasis;
    final Player player;
    private int ticks;
    private int lastState;

    OasisScreen(OasisService oasis, Player player) {
        this.oasis = oasis;
        this.player = player;
    }

    @Override
    protected String info() {
        return oasis.vault().available() ? "Saldo: " + oasis.vault().format(oasis.vault().balance(player)) : null;
    }

    /** Co sekundę sprawdza, czy zmieniło się coś widocznego na ekranie ({@link #state}). */
    @Override
    protected boolean tick() {
        if (++ticks % 10 != 0) {
            return false;
        }
        int now = state();
        boolean changed = now != lastState;
        lastState = now;
        return changed;
    }

    /** Skrót tego, co ekran pokazuje, a co może się zmienić samo (saldo, ekwipunek, czas do odświeżenia). */
    protected int state() {
        return Objects.hash(info(), java.util.Arrays.hashCode(player.getInventory().getStorageContents()));
    }

    // ------------------------------------------------------------------ rysowanie

    /** Tło wiersza listy: na zmianę dwa odcienie, jaśniejszy pod kursorem, zielony, gdy wybrany. */
    static boolean row(GuiCanvas c, int x, int y, int w, int index, boolean selected) {
        boolean hover = c.hover(x, y, w, ROW_H);
        c.fill(x, y, w, ROW_H, selected ? GuiCanvas.ROW_SELECTED : hover ? GuiCanvas.ROW_HOVER
                : index % 2 == 0 ? GuiCanvas.ROW : GuiCanvas.ROW_ALT);
        if (selected) {
            c.fill(x, y, 2, ROW_H, GuiCanvas.GREEN);
        } else if (hover) {
            c.fill(x, y, 2, ROW_H, GuiCanvas.ACCENT);
        }
        return hover;
    }

    /** Pasek przewijania przy liście (tylko gdy lista się nie mieści). */
    static void scrollbar(GuiCanvas c, int x, int y, int h, int first, int visible, int total) {
        if (total <= visible) {
            return;
        }
        c.inset(x, y, 4, h);
        int thumb = Math.max(10, h * visible / total);
        int top = y + (h - thumb) * first / Math.max(1, total - visible);
        c.fill(x + 1, top, 2, thumb, GuiCanvas.TEXT_DIM);
    }

    /** Pierwszy widoczny wiersz po przewinięciu, w granicach listy. */
    static int clampFirst(int first, int visible, int total) {
        return Math.max(0, Math.min(first, total - visible));
    }

    /** Linie opisu do panelu albo dymku, w podanej szerokości. */
    static List<String> wrapAll(List<String> lines, int width) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.addAll(GuiCanvas.wrap(line, width));
        }
        return out;
    }

    // ------------------------------------------------------------------ opisy przedmiotów

    static String name(ItemStack item) {
        return GuiCanvas.plain(Items.name(item));
    }

    /** Opis przedmiotu do dymku: nazwa, ilość, zaklęcia, wytrzymałość i opis z przedmiotu. */
    @SuppressWarnings("deprecation")
    static List<String> describe(ItemStack item) {
        List<String> lines = new ArrayList<>();
        lines.add(name(item) + (item.getAmount() > 1 ? "  x" + item.getAmount() : ""));
        lines.addAll(details(item));
        return lines;
    }

    /** Zaklęcia, wytrzymałość i opis przedmiotu (bez nazwy). */
    @SuppressWarnings("deprecation")
    static List<String> details(ItemStack item) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            String key = e.getKey().getKey().getKey().replace('_', ' ');
            lines.add("Zaklęcie: " + key + " " + e.getValue());
        }
        ItemMeta meta = item.getItemMeta();
        int max = item.getType().getMaxDurability();
        if (meta instanceof Damageable damageable && max > 0 && damageable.hasDamage()) {
            lines.add("Wytrzymałość: " + (max - damageable.getDamage()) + "/" + max);
        }
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                String plain = GuiCanvas.plain(line).trim();
                if (!plain.isEmpty()) {
                    lines.add(plain);
                }
            }
        }
        return lines;
    }

    static String upper(String text) {
        return GuiCanvas.plain(text).toUpperCase(Locale.ROOT);
    }
}
