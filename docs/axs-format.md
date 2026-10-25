# Jak zrobić plik `.axs`

Krótka ściąga dla każdego, kto generuje nową strukturę. Parser to `src/main/java/pl/axon/structure/core/StructureFile.java`, pola nagłówka czyta `core/Traits.java`. Gotowe pliki do podejrzenia leżą w folderze projektu (`kombinat/kombinat.axs`, `kiosk/kiosk.axs`). Nie trzeba czytać całego repo.

## Skąd brać plik

- **Generator w Javie** (`src/generator`): klasa implementuje `StructureDefinition` (`id`, `displayName`, `description`, `build(Random)` zwraca `Blueprint`), dopisana do listy w `StructureExporter`. `./gradlew generujStruktury` zapisuje `build/struktury/*.axs` i postarza je klasą `Weathering`. Meble w kodzie są w `Furniture`, pola nagłówka przez `bp.traits(...)`.
- **Skrypt w Pythonie** (małe struktury, np. kiosk, fastfood): `tools/axs/lib.py` ma klasę `S` z `set/fill/mix/crate/point/sign/save` i render PNG. Wzór: `kiosk/zrodla/kiosk.py` w folderze projektu. Na końcu skryptu `s.save(...)` i `previews(...)`.

## Układ współrzędnych

`x` to szerokość (zachód → wschód), `y` wysokość, `z` głębokość (północ → południe). `z = 0` to front, od którego zwykle wchodzi wjazd z drogi (`north`). Obrót przy wklejaniu plugin liczy sam, plik zawsze opisuje strukturę w jednym ustawieniu.

## Nagłówek

```
# AXON-Structure v1
id: kiosk                     (nazwa w komendach i nazwa pliku)
nazwa: Kiosk U Mietka
opis: jedno zdanie
rozmiar: 26 10 22             (szerokość wysokość głębokość)
grunt: 3                      (opcjonalnie: warstwy 0..2 idą pod ziemię)
wjazdy: 11 21 south; 0 12 west
strefa: rownina
```

Obowiązkowe są `id` i `rozmiar`. Opcjonalne pola:

| Pole | Wartości | Brak pola |
| --- | --- | --- |
| `grunt` | numer warstwy leżącej na poziomie terenu | 0 |
| `wjazdy` | `x z kierunek; ...`, punkt na krawędzi, kierunek na zewnątrz (`north` przy `z = 0`, `south` przy `z = głębokość-1`, `west` przy `x = 0`, `east` przy `x = szerokość-1`) | brak, drogi się nie wpinają |
| `rozmiar-klasa` | `duza`, `srednia`, `mala` | z wymiarów: dłuższy bok ≥ 100 duża, ≥ 50 średnia |
| `mapa` | `tak`, `nie` (struktura testowa) | `tak` |
| `strefa` | `rownina`, `przy-gorach`, `las`, `ustronnie`, `srodek`, `plaza`, `dowolna` (po przecinku) | `dowolna` |
| `klimat` | `snieg`, `umiarkowany`, `suchy` (po przecinku) | wszystkie |
| `bezpieczna` | `tak` (oaza: bez PvP, tylko komendą) | `nie` |

Błędna wartość przerywa wczytywanie pliku. Nieznane klucze przechodzą bez zmian.

## Bloki

```
paleta:
0 minecraft:air               (indeks 0 to zawsze powietrze, indeksy po kolei od 0)
1 minecraft:oak_stairs[facing=north,half=bottom]
warstwy:
0 5: 1*4 0*20 2 2             (y z: bloki wzdłuż x jako "indeks*ile")
```

Każdy wiersz musi mieć dokładnie `szerokość` bloków. Wiersze z samym powietrzem się pomija. Stany bloków piszemy pełne, z właściwościami w nawiasach. Plik jest w UTF-8, polskie znaki są dozwolone.

## Skrzynie, NPC i punkty

Wszystko idzie do sekcji `skrzynie:` w formacie `x y z kierunek pula` (kierunek `north/south/east/west`, w `ramki:` także `up/down`).

- **Zwykła skrzynia albo beczka**: dowolna pula z `pule:` w `config.yml` (`plac`, `parter`, `lada`, `zaplecze`, `paliwo`, `magazyn`, ...). Blok pod punktem plugin zeruje, skrzynię stawia odświeżanie. Lepsze skrzynie chowamy za parkourem albo pod ziemią.
- **Ramka z kartą**: sekcja `ramki:`, ten sam format, ramka wisi na bloku po przeciwnej stronie niż `kierunek`.
- **NPC**: pula `npc-<zestaw>`, gdzie zestaw jest w `straznicy.zestawy` (`szabrownik`, `zolnierz`, `ochroniarz`, `osadnik`, `ocalaly`, `spiacy`). Punkt to blok pod stopami NPC, dwie kratki powietrza nad nim.
- **Zrzut zaopatrzenia**: pula `zrzut`, na otwartym placu lub dachu z czystym niebem (skrzynia spada z 60 bloków). Zwykłe odświeżanie go pomija.
- **Stanowiska** (`punkt-*`) leżą w prawdziwym bloku, który zostaje (w Pythonie `s.point`): `punkt-sklep-<nazwa>` (sklep z `stanowiska.sklepy`), `punkt-automat`, `punkt-radio`, `punkt-ruda` (blok rudy w kopalni), `punkt-zwierze-<typ>` (`kura`, `krowa`, `cat`, ...). Oaza ma swoje: `punkt-handel`, `punkt-sklep`, `punkt-depozyt`, `punkt-zlecenia`, `punkt-tarcza`, `punkt-strzelnica`, `punkt-handlarz`, a czarny rynek `punkt-czarny-rynek` i `punkt-czarny-rynek-lada`.

Nowa pula skrzyń wymaga wpisu w `pule:` w `config.yml`, inaczej punkt zostaje pusty.

## Tabliczki

```
napisy:
20 6 20|PALIWO|U MIETKA||     (x y z|linia1|linia2|linia3|linia4)
```

W `warstwy` w tym miejscu musi stać blok tabliczki (`oak_wall_sign[facing=south]` itp.), inaczej napis przepada. Linia ma najwyżej ok. 15 znaków.

## Meble i modele z paczki

- **Meble**: zwykłe bloki, które plugin zamienia na barierę z modelem (lista w `meble.bloki` w `config.yml`). Przód modelu patrzy w `facing` bloku. Najczęstsze: `bookshelf` regał, `crafting_table` warsztat, `smoker` kuchenka, `furnace` koza, `cauldron` umywalka, `lectern` biurko z komputerem, `composter` kosz, `observer` radiostacja, `brewing_stand` kroplówka, `ender_chest` kasa, `jack_o_lantern` lada, `end_portal_frame` kibel, `bee_nest` regał sklepowy, `sticky_piston` rozdzielnia, `vault` frytkownica, `respawn_anchor` kanistry, `lodestone` wózek, `loom` telewizor. `calibrated_sculk_sensor` (lodówka) i `damaged_anvil` (dystrybutor) mają dwie kratki wysokości, więc nad nimi stawiamy `minecraft:barrier`. Łóżka i drzwi z `meble.drzwi` też dostają modele.
- **Duże modele** (wraki, lokomotywy): sekcja `modele:` z linią `x y z model skala obrót`, np. `62.5 5 76 axon:wrak_mi8 8 0`. Auta (`axon:auto_sedan`, `axon:auto_sedan_spalony`, `axon:auto_kombi`) mają 2 × 4 kratki i skalę 2, środek 0,75 kratki nad gruntem w środku obrysu, bariery w dwóch warstwach. `x y z` to środek modelu (mogą być ułamki), obrót w stopniach. Kolizję dają bariery w `warstwy` w obrysie modelu.

## Podgląd i przekrój PNG

Do każdej nowej struktury oddajemy dwa obrazki do folderu projektu `<nazwa>/`:

- `<nazwa>-podglad.png`: izometria całości od południowego wschodu,
- `<nazwa>-przekroj.png`: to samo ucięte nad parterem, z legendą: brązowe skrzynie, niebieskie lepsze skrzynie, czerwone NPC, żółte wjazdy.

Robi je `previews(s, katalog, nazwa, cut=Y)` z `tools/axs/preview.py`, gdzie `cut` to najwyższa pokazywana warstwa. Dla struktur z podziemiem warto dodać trzeci obrazek z mniejszym `cut`. Fioletowe kratki na obrazku to bloki, których `lib.py` nie zna: dopisz je do `COL`.

## Sprawdzenie bez serwera

```bash
python3 tools/axs/sprawdz_axs.py plik.axs                  # te same reguły co StructureFile.read
python3 tools/axs/sprawdz_axs.py plik.axs --png out/ 6     # plus podgląd i przekrój do out/
```

Skrypt kończy się błędem z numerem linii, gdy coś nie przejdzie u pluginu (zły wiersz, indeks spoza palety, punkt poza rozmiarem, zła liczba pól, zła wartość w nagłówku, wjazd poza krawędzią), ostrzega o napisach bez tabliczki i wypisuje nagłówek oraz liczbę punktów w każdej puli. Wystarczy Python 3, a do `--png` także Pillow. Pełny test na serwerze Paper robi GitHub Actions (`.github/scripts/smoke-test.sh`) dla struktur z `src/generator`.
