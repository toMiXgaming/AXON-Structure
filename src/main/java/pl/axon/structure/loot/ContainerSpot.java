package pl.axon.structure.loot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Miejsce na pojemnik-model: w strukturze (punkt skrzyni z pliku .axs), przy drodze (z AxonMapGenerator) albo
 * postawione komendą. Stan trzymany jest tylko w pamięci: encje nie zapisują się w świecie i stają przy wczytaniu
 * chunka, a po restarcie każdy pojemnik losuje się od nowa.
 */
final class ContainerSpot implements InventoryHolder {

    /** Wysokość nieznana: pojemnik staje na terenie, liczona przy pierwszym postawieniu. */
    static final int ON_TERRAIN = Integer.MIN_VALUE;

    enum State {
        /** Nic nie stoi, czeka na odnowienie ({@code respawnAt}). */
        WAITING,
        /** Cała beczka, czeka na rozbicie. */
        INTACT,
        /** Skrzynia, która się nie rozbija: stoi zawsze, loot losuje się przy pierwszym otwarciu. */
        CLOSED,
        /** Rozbita beczka z lootem w środku. */
        BROKEN
    }

    final String key;
    final String world;
    final int x;
    int y;
    final int z;
    /** Kierunek, w który patrzy przód pojemnika (yaw Minecrafta: 0 południe, 90 zachód). */
    final float yaw;
    /** Pula losowania albo null, gdy pojemnik ma stałą klasę ({@code fixedClass}). */
    final String pool;
    final String fixedClass;
    /** Id instancji struktury albo null (droga, komenda). */
    final String owner;
    final boolean roadside;

    State state = State.WAITING;
    CrateClass crate;
    Inventory inventory;
    /** Postęp rozbijania: 1.0 = rozbita. */
    double damage;
    long lastHit;
    long brokenAt;
    long emptiedAt;
    long respawnAt;
    /** Pojemnik nie może tu stanąć (woda pod spodem, poza światem); nie próbujemy więcej. */
    boolean dead;
    final List<UUID> entities = new ArrayList<>();
    UUID display;
    /** Do kiedy (epoch millis) pojemnik jest zablokowany; 0 = otwarty. */
    long lockedUntil;
    /** Napis z odliczaniem nad zablokowanym pojemnikiem. */
    UUID lockText;

    ContainerSpot(String key, String world, int x, int y, int z, float yaw, String pool, String fixedClass,
                  String owner, boolean roadside) {
        this.key = key;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pool = pool;
        this.fixedClass = fixedClass;
        this.owner = owner;
        this.roadside = roadside;
    }

    boolean visible() {
        return state != State.WAITING && !dead;
    }

    int chunkX() {
        return x >> 4;
    }

    int chunkZ() {
        return z >> 4;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
