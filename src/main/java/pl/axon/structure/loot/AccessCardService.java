package pl.axon.structure.loot;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Karty dostępu (etap 16 #7): niebieska + czerwona → zielona, zużycie przy otwarciu elitarnej skrzyni.
 *
 * <p>Karta to zwykły {@link Material#PAPER} z nazwą, lore i znacznikiem w PDC
 * ({@code axon_access_card = <id>}). Dzięki temu:
 * <ul>
 *   <li>nie da się jej podrobić zwykłym papierem,</li>
 *   <li>przeżywa restart i działa w skrzyniach/ramkach,</li>
 *   <li>można ją rozpoznać po samym przedmiocie, bez trzymania stanu w pamięci.</li>
 * </ul>
 *
 * <p>Łączenie odbywa się w stole rzemieślniczym: niebieska + czerwona dają zieloną.
 * Receptura jest „wirtualna” – przechwytujemy {@link PrepareItemCraftEvent} i podmieniamy wynik,
 * więc nie wymaga rejestracji w Bukkitowym rejestrze receptur (a więc nie zaśmieca księgi receptur).
 */
public final class AccessCardService implements Listener {

    /** Klucz PDC z identyfikatorem karty. */
    public static final String CARD_TAG = "axon_access_card";

    private final JavaPlugin plugin;
    private final NamespacedKey cardKey;

    public AccessCardService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cardKey = new NamespacedKey(plugin, CARD_TAG);
    }

    // ------------------------------------------------------------------
    //  Tworzenie kart
    // ------------------------------------------------------------------

    /** Buduje kartę dostępu o podanym id ({@code niebieska}/{@code czerwona}/{@code zielona}). */
    public ItemStack create(String cardId) {
        String id = cardId == null ? "" : cardId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!AccessCardMath.known(id)) {
            return null;
        }
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(LootConfig.color(AccessCardMath.displayName(id)));
        List<String> lore = new ArrayList<>();
        lore.add(LootConfig.color(AccessCardMath.levelLine(id)));
        lore.add(LootConfig.color("&8Karta dostępu AXON"));
        if (AccessCardMath.GREEN.equals(id)) {
            lore.add(LootConfig.color("&7Otwiera skrzynie elitarne."));
            lore.add(LootConfig.color("&7Zużywana przy użyciu."));
        } else {
            lore.add(LootConfig.color("&7Połącz z drugą kartą w stole,"));
            lore.add(LootConfig.color("&7żeby zrobić zieloną."));
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(cardKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    /** Identyfikator karty z przedmiotu (albo {@code null}, gdy to nie karta). */
    public String cardOf(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(cardKey, PersistentDataType.STRING);
        return AccessCardMath.known(id) ? id : null;
    }

    /** Czy przedmiot jest kartą dostępu. */
    public boolean isCard(ItemStack item) {
        return cardOf(item) != null;
    }

    // ------------------------------------------------------------------
    //  Łączenie: niebieska + czerwona → zielona
    // ------------------------------------------------------------------

    /**
     * Receptura łączenia kart. Przechwytujemy przygotowanie wyniku w stole rzemieślniczym:
     * gdy w siatce jest dokładnie jedna niebieska i jedna czerwona karta (plus puste sloty),
     * wynikiem jest zielona karta.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        String first = null;
        String second = null;
        int cards = 0;
        for (ItemStack slot : matrix) {
            if (slot == null || slot.getType().isAir()) {
                continue;
            }
            String card = cardOf(slot);
            if (card == null) {
                // cokolwiek innego w siatce – to nie nasza receptura
                event.getInventory().setResult(null);
                return;
            }
            cards++;
            if (cards == 1) {
                first = card;
            } else if (cards == 2) {
                second = card;
            } else {
                event.getInventory().setResult(null);
                return;
            }
        }
        if (cards != 2) {
            event.getInventory().setResult(null);
            return;
        }
        String result = AccessCardMath.combine(first, second);
        event.getInventory().setResult(result == null ? null : create(result));
    }

    // ------------------------------------------------------------------
    //  Zużycie przy otwarciu skrzyni
    // ------------------------------------------------------------------

    /**
     * Sprawdza, czy gracz ma kartę o wystarczającym poziomie i zużywa ją.
     *
     * <p>Kolejność: najpierw karta w ręce (najbardziej naturalna), potem reszta ekwipunku.
     * Zużywamy <b>dokładnie jedną</b> kartę – nawet jeśli gracz ma ich kilka.
     *
     * @return {@code true}, gdy karta została znaleziona i zużyta
     */
    public boolean consume(Player player, int requiredLevel) {
        if (player == null || requiredLevel <= 0) {
            return true; // skrzynia nie wymaga karty
        }
        PlayerInventory inventory = player.getInventory();
        int held = inventory.getHeldItemSlot();
        ItemStack inHand = inventory.getItem(held);
        if (matches(inHand, requiredLevel)) {
            consumeOne(inventory, held, inHand);
            return true;
        }
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (i == held) {
                continue;
            }
            if (matches(contents[i], requiredLevel)) {
                consumeOne(inventory, i, contents[i]);
                return true;
            }
        }
        return false;
    }

    /** Czy gracz ma kartę o wystarczającym poziomie (bez zużywania). */
    public boolean has(Player player, int requiredLevel) {
        if (player == null || requiredLevel <= 0) {
            return true;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (matches(item, requiredLevel)) {
                return true;
            }
        }
        return false;
    }

    /** Najwyższy poziom karty, jaki gracz ma przy sobie (0 = brak). */
    public int bestLevel(Player player) {
        if (player == null) {
            return 0;
        }
        int best = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            String card = cardOf(item);
            if (card != null) {
                best = Math.max(best, AccessCardMath.level(card));
            }
        }
        return best;
    }

    private boolean matches(ItemStack item, int requiredLevel) {
        String card = cardOf(item);
        return card != null && AccessCardMath.opens(card, requiredLevel);
    }

    private void consumeOne(PlayerInventory inventory, int slot, ItemStack stack) {
        int amount = stack.getAmount();
        if (amount <= 1) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(amount - 1);
            inventory.setItem(slot, stack);
        }
    }

    /**
     * Blokuje „przypadkowe” zużycie karty przez zwykłe przełączenie slotu – karta ma być
     * zużywana wyłącznie przy otwieraniu skrzyni. Metoda istnieje, żeby jawnie udokumentować,
     * że nie podpinamy się pod {@code PlayerItemHeldEvent}; zostawiamy ją jako no-op hook
     * dla przyszłych rozszerzeń (np. podglądu poziomu na pasku akcji).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent event) {
        // celowo puste – karta nie zużywa się przy zmianie slotu
    }
}
