package pl.axon.structure.api;

import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Publiczne API AXON-Structure dla innych pluginów (np. AxonMapGenerator).
 * <p>
 * Pobranie: {@code AxonStructureApi.get()}. Zwraca null, gdy AXON-Structure nie jest włączony.
 */
public interface AxonStructureApi {

    /** Instancja API z ServicesManager albo null, gdy AXON-Structure nie działa. */
    static AxonStructureApi get() {
        return Bukkit.getServicesManager().load(AxonStructureApi.class);
    }

    /** Wszystkie wczytane szablony. Można wywoływać z dowolnego wątku. */
    Collection<StructureTemplate> templates();

    /** Szablon o danym id (bez względu na wielkość liter). Można wywoływać z dowolnego wątku. */
    Optional<StructureTemplate> template(String id);

    /**
     * Rejestruje strukturę, której bloki już stoją w świecie: losuje skrzynie, wpisuje napisy i ramki,
     * zakłada ochronę i region WorldGuard, zapisuje w dane.yml. Nie stawia bloków szablonu.
     * <p>
     * Wątek serwera. Idempotentne po {@code instanceKey}: powtórne wywołanie zwraca istniejącą instancję.
     *
     * @param instanceKey klucz instancji, unikalny w obrębie serwera
     * @param templateId  id szablonu
     * @param world       nazwa świata
     * @param originX     punkt (0, 0, 0) szablonu, x
     * @param originY     y dna szablonu (warstwa 0); grunt wypada na {@code originY + groundLevel()}
     * @param originZ     punkt (0, 0, 0) szablonu, z
     * @param rotation    obrót 0–3 × 90°
     * @throws IllegalArgumentException gdy nie ma szablonu albo świata
     * @throws IllegalStateException    gdy wywołano poza wątkiem serwera
     */
    StructureInstanceInfo register(String instanceKey, String templateId, String world,
                                   int originX, int originY, int originZ, int rotation);

    /** Czy instancja o tym kluczu jest już zarejestrowana. */
    boolean isRegistered(String instanceKey);

    /** Zapomina wszystkie instancje w świecie (zdejmuje ochronę, skrzynie i pojemniki przy drogach, bloki zostają). Wątek serwera. */
    void unregisterWorld(String world);

    /** Wyłącza automatyczne stawianie struktur AXON-Structure w tym świecie. */
    void claimWorld(String world);

    /**
     * Ustawia punkty pojemników przy drogach (beczek) w świecie i zastępuje poprzednie. Pojemniki są modelami bez
     * bloku: stają przy wczytaniu chunka na wysokości terenu, losują się z puli {@code pojemniki.przy-drogach.pula}
     * i odnawiają w tym samym miejscu. Punkty nie są zapisywane, więc trzeba je podać po każdym starcie serwera.
     * Punkt nad wodą jest pomijany. Wątek serwera.
     *
     * @return ile punktów jest teraz w świecie
     */
    int setRoadsideContainers(String world, Collection<RoadsideSpot> spots);
}
