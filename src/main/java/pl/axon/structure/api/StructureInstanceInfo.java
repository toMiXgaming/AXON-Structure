package pl.axon.structure.api;

/**
 * Wynik rejestracji struktury.
 *
 * @param key          klucz podany przez wywołującego (np. {@code amg:<seed>:<nr>})
 * @param instanceId   id instancji w AXON-Structure
 * @param templateId   id szablonu
 * @param world        nazwa świata
 * @param centerX      środek struktury, x
 * @param centerZ      środek struktury, z
 * @param cratesPlaced ile skrzyń i ramek zostało wylosowanych; 0 przy ponownej rejestracji tego samego klucza
 */
public record StructureInstanceInfo(String key, String instanceId, String templateId, String world,
                                    int centerX, int centerZ, int cratesPlaced) {
}
