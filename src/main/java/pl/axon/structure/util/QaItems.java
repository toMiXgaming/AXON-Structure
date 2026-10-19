package pl.axon.structure.util;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Przedmioty z QualityArmory po nazwie, przez refleksję (QualityArmory jest opcjonalny). QualityArmory porównuje
 * nazwy dokładnie (wielkość liter ma znaczenie), więc tu szukamy bez względu na wielkość liter, spacje, myślniki
 * i podkreślenia: {@code ak47}, {@code AK-47} i {@code AK47} to ta sama broń. Gdy takiej nazwy nie ma, bierzemy
 * najkrótszą, która się od niej zaczyna ({@code m4a1} → {@code m4a1s}).
 */
public final class QaItems {

    private static final String API = "me.zombie_striker.qg.api.QualityArmory";

    private QaItems() {
    }

    public static boolean available() {
        try {
            Class.forName(API);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Przedmiot o tej nazwie albo null, gdy QualityArmory go nie zna (albo go nie ma). */
    public static ItemStack find(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            Class<?> qa = Class.forName(API);
            Object exact = qa.getMethod("getCustomItemAsItemStack", String.class).invoke(null, name);
            if (exact instanceof ItemStack stack) {
                return stack.clone();
            }
            String wanted = normalize(name);
            Class<?> base = Class.forName("me.zombie_striker.customitemmanager.CustomBaseObject");
            Method getName = base.getMethod("getName");
            Method asStack = qa.getMethod("getCustomItemAsItemStack", base);
            // dokładnie ta nazwa, a gdy jej nie ma – najkrótsza zaczynająca się od niej (m4a1 → m4a1s)
            Object best = null;
            int bestLength = Integer.MAX_VALUE;
            for (Object item : (List<?>) qa.getMethod("getCustomItemsAsList").invoke(null)) {
                Object itemName = getName.invoke(item);
                if (itemName == null) {
                    continue;
                }
                String n = normalize(itemName.toString());
                if (n.equals(wanted)) {
                    best = item;
                    break;
                }
                if (n.startsWith(wanted) && n.length() < bestLength) {
                    best = item;
                    bestLength = n.length();
                }
            }
            if (best != null) {
                Object stack = asStack.invoke(null, best);
                return stack instanceof ItemStack s ? s.clone() : null;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // brak QualityArmory albo inna wersja API
        }
        return null;
    }

    /** Nazwy wszystkich przedmiotów QualityArmory (do komunikatów, gdy nazwa z configu nie pasuje). */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        try {
            Class<?> qa = Class.forName(API);
            Method getName = Class.forName("me.zombie_striker.customitemmanager.CustomBaseObject").getMethod("getName");
            for (Object item : (List<?>) qa.getMethod("getCustomItemsAsList").invoke(null)) {
                Object n = getName.invoke(item);
                if (n != null) {
                    names.add(n.toString());
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // brak QualityArmory
        }
        return names;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]", "");
    }
}
