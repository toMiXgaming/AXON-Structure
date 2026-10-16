# AXON-Structure

Plugin na serwer **Paper 1.21.4** (Java 21) do stawiania struktur w stylu monumentów z Rusta: z plików, z odświeżanymi skrzyniami i z automatycznym generowaniem w świecie.

## Jak to działa

1. Plugin startuje **pusty** – nie ma w nim żadnej struktury.
2. Struktury to pliki `.axs`. Wrzucasz je do `plugins/AXON-Structure/struktury/` i wpisujesz `/struktury przeladuj`.
3. W pliku struktury są tylko **miejsca na skrzynie** z nazwą puli. Co się w nich pojawi, decyduje `config.yml`.
4. Postawione struktury (ręcznie i automatycznie) są zapisywane w `dane.yml`, a ich skrzynie co jakiś czas losowane od nowa.

## Komendy

| Komenda | Opis |
| --- | --- |
| `/struktury lista` | wczytane struktury |
| `/struktury wklej <nazwa>` | stawia strukturę przed tobą, na twojej wysokości, frontem do ciebie |
| `/struktury generuj <nazwa>` | stawia ją wokół ciebie na wysokości terenu (jak auto-generowanie) |
| `/struktury cofnij` | cofa twoje ostatnie postawienie (pamięta 5) |
| `/struktury odswiez` | od razu losuje skrzynie w najbliższej strukturze |
| `/struktury najblizsza` | pokazuje najbliższą postawioną strukturę |
| `/struktury usun [id]` | zapomina strukturę: zdejmuje ochronę i skrzynie, bloki zostają |
| `/struktury klasy` | klasy skrzyń i pule z szansami |
| `/struktury zrzut [info]` | zrzuca skrzynię z zaopatrzeniem od razu (`info`: za ile minut następny) |
| `/struktury przeladuj` | wczytuje config i pliki struktur od nowa |

Z konsoli: `wklej <nazwa> <świat> <x> <y> <z> [obrót 0-3]` i `generuj <nazwa> <świat> <x> <z> [obrót 0-3]`.
Alias: `/axs`. Uprawnienie: `axonstructure.use` (domyślnie OP).

## Skrzynie

- **Klasa skrzyni** (`klasy:` w configu) – nazwa, blok (`skrzynia`, `beczka` albo `ramka`), opcjonalna dekoracja na wieku i lista lootu z ilościami i szansami.
- **Pula** (`pule:`) – dla danego miejsca losuje klasę według wag. Waga `brak` oznacza, że w tym miejscu nic się nie pojawi.
- **Odświeżanie** – co `odswiezanie.co-ile-minut` minut. Pełne skrzynie zostają (chyba że `zastepuj-pelne: true`), puste i zniszczone losowane są od nowa. Nie odświeża, gdy gracz stoi bliżej niż `blokada-gdy-gracz-blizej-niz`.

## Dopasowanie do terenu

Przy każdym stawianiu plugin:

- dosypuje grunt pod strukturą aż do litej ziemi (bez dziur i jaskiń pod podłogą),
- wycina drzewa i pagórki nad nią,
- wygładza pas terenu wokół (`wygladzanie-krawedzi`) – teren łagodnie przechodzi do poziomu struktury, materiałem z okolicy (trawa, piasek, …).

Auto-generowanie stawia strukturę tylko tam, gdzie pod nią jest mało wody (`maks-woda-procent`) i teren jest w miarę równy (`maks-roznica-wysokosci`), w odstępie co najmniej `min-odleglosc-miedzy` bloków od innych struktur i `min-odleglosc-od-spawnu` od spawnu.

## Ochrona i działki AxonPlot

- Gracze nie mogą niszczyć ani budować w obrębie struktury (obrys + `ochrona.margines`, cała wysokość świata). Nie działają też wybuchy, ogień, tłoki ani wylewanie wody i lawy. Wolno otwierać skrzynie, drzwi i przyciski oraz zabierać karty z ramek. Uprawnienie `axonstructure.bypass` (domyślnie OP) pomija ochronę.
- PvP działa: region WorldGuard `struktura_<id>` ma flagę `pvp: allow` (flagi ustawisz w `ochrona.flagi-worldguard`).
- Struktura nie stanie bliżej niż `ochrona.odstep-od-dzialek` kratek od działki (regiony `dzialka_*`), licząc działkę w jej największym rozmiarze (`ochrona.promien-dzialki`). Dotyczy auto-generowania i komend.
- W drugą stronę pilnuje AxonPlot: nie da się założyć działki bliżej niż `placement.structure-buffer-blocks` od regionu `struktura_*`.
- Odstęp od działek wymaga WorldGuarda. Bez niego ochrona przed niszczeniem dalej działa.

## Format pliku `.axs`

Tekstowy: nagłówek (`id`, `nazwa`, `opis`, `rozmiar`, opcjonalnie `grunt`), paleta bloków, warstwy (`y z: indeks*ile ...`), a na końcu sekcje `skrzynie:` i `ramki:` (`x y z kierunek pula`) oraz `napisy:`. Szczegóły w `StructureFile.java`.

`grunt: N` oznacza, że warstwa `N` leży na poziomie terenu, a warstwy `0 .. N-1` to podziemie wklejane pod powierzchnię (bez tego pola grunt to warstwa 0, jak dotąd). Wysokość podawana w `/struktury wklej` i wyliczana przy auto-generowaniu to zawsze poziom gruntu.

Struktury opisane w kodzie (`src/generator`):

| Plik | Struktura | Pule skrzyń |
| --- | --- | --- |
| `stacja.axs` | Opuszczona stacja benzynowa (Oxum's Gas) | `paliwo`, `plac`, `lada`, `karta`, `sklep`, `lodowki`, `toalety`, `zaplecze`, `magazyn-regal`, `magazyn`, `dach` |
| `gospodarstwo.axs` | Opuszczone gospodarstwo Agro-Pol (PGR) | `plac`, `paliwo`, `parter`, `klepisko`, `strych`, `pietro`, `silos` |
| `kombinat.axs` | Kombinat rolny Przyszłość (spółdzielnia) | `plac`, `paliwo`, `parter`, `klepisko`, `strych`, `magazyn-nawozow`, `suwnica`, `wieza`, `elewator` |
| `kopalnia.axs` | Zamknięta kopalnia węgla Jedność (z podziemiem, `grunt: 12`) | `plac`, `paliwo`, `parter`, `strych`, `sztolnia`, `podszybie`, `podszybie-sklad`, `maszynownia`, `wieza-szybowa` |
| `jednostka.axs` | Opuszczona jednostka wojskowa JW 2137, 150 × 130 (dwupoziomowy schron, `grunt: 21`) | `plac`, `paliwo`, `parter`, `lada`, `klepisko`, `strych`, `wieza`, `izba-chorych`, `sztab`, `wieze-wartownicze`, `wrak`, `dzwig`, `schron-1`, `magazyn-broni`, `zbrojownia`, `komin`, `wieza-lotow`, `skarbiec` |
| `szpital.axs` | Opuszczony Szpital Powiatowy Pod Lipami (piwnice, `grunt: 9`) | `plac`, `parter`, `lada`, `izba-chorych`, `apteka`, `blok-operacyjny`, `dyrekcja`, `kostnica`, `ladowisko` |

W gospodarstwie lepsze skrzynie wymagają parkouru: strych stodoły – heder kombajnu, maska, kabina, sterta siana, krawędź strychu; piętro domu – stos drewna, dach szopy na węgiel, balkon; rzadka skrzynia leży na szczycie silosu (drabina w klatce).

W kombinacie: piętro paszarni – przyczepa, bela, poprzeczka silosu paszowego, daszek, okno; pomost suwnicy w hali – podniesiona wywrotka, dach kabiny ciągnika, regał; wieża elewatora – drabina na półpiętro, drzwi serwisowe na zewnętrzny pomost, drabina na ścianie i z powrotem do środka; wieża ciśnień – długa drabina bez klatki. Z kanału w hali i ze zbiornika na gnojowicę wychodzi się tylko po drabince.

W kopalni: wieża szybowa – zygzakowate schody, w trzecim biegu brakuje stopnia, więc z ostatniego skacze się na belkę kratownicy i z niej na wyższy podest; pulpit maszynisty – kratownica przy bębnie, bęben, belka suwnicy, pomost; najwyższe piętro sortowni – rampa i wąski taśmociąg z zewnątrz (schody w środku zawalone). Pod nadszybiem jest podszybie 12 bloków pod ziemią: schodzi się drabiną w szybie albo przez dziurę w kracie na dach klatki, a wychodzi tylko drabiną. Skład materiałów wybuchowych za kratą ma jedną dziurę.

W jednostce: strych garaży – zderzak Stara (albo palety przy wiacie i dach wiaty), maska, kabina, plandeka, kadłub BTR-a, wyrwana krata wentylacyjna; kosz dźwigu – palety, dach wiaty, kabina dźwigu, wysięgnik nad placem; komin kotłowni – drabina w kotłowni na dach, rura ciepłownicza, pomost w połowie komina i klamry na szczyt (dolne klamry urwane); magazyn sprzętu – stopnie z palet na regał, przeskok nad przewróconą sekcją, kładka na drugi rząd; wieża desantowa – stopień, ścianka, równoważnia nad rowem z wodą, słupki coraz wyżej; wieża kontroli lotów – schody do połowy, balkon, drabina na zewnątrz do kabiny; wieża wartownicza NW – tylko skokiem z dachu koszar A, SE – z wału ziemnego (w obu drabinach brakuje szczebli); kancelaria tajna w sztabie – tylko przez zawalony strop archiwum. Magazyn uzbrojenia ma jedną wyrwę w siatce, trzeci bunkier wejście przez dach. Schron pod lądowiskiem: grzybek i śluza na poziom 1 (9 bloków pod ziemią), drugie wejście drabiną z szybu w hangarze; poziom 2 (19 bloków) przez klatkę z urwanym biegiem; skarbiec tylko szybem wentylacyjnym z maszynowni (drzwi od korytarza zamknięte); szyb awaryjny z zalanego tunelu wyprowadza do lasu, ale wejść nim się nie da. Nowa klasa `skrzynia-broni` ma tylko zwykłe przedmioty (łuki, kusze, strzały, miecz, tarcza, proch, race).

W szpitalu: 4 piętro (dyrekcja) – schody kończą się na 3 piętrze, dalej szybem windy: z 3 piętra na dach utkniętej kabiny i drabiną serwisową do wyważonych drzwi, albo włazem z dachu; dach z lądowiskiem – właz z 4 piętra albo rusztowanie przy zachodnim szczycie (pomosty co 4 bloki, drabiny na zmianę, dziury w deskach); apteka – kosz na śmieci, kabina i skrzynia przewróconej karetki, daszek wejścia do pawilonu, sześć klimatyzatorów na ścianie, dach pawilonu, kanał wentylacyjny z drabinką; blok operacyjny – tylko łącznikiem z 1 piętra (schody pawilonu zasypane), z sali przy wschodniej ścianie można wyskoczyć oknem. Z daszku podjazdu karetek wchodzi się na dach izby przyjęć i oknami do sal 1 piętra. Piwnice mają dwa wejścia (schody w klatce zachodniej i zjazd dla karawanów do kostnicy) i tunel do kotłowni z zalanym odcinkiem.

**Zrzuty zaopatrzenia** (`zrzuty:` w configu): co losowe 45–90 minut, gdy online są co najmniej 2 osoby, nad lądowiskiem jednostki albo dachem szpitala spada na spadochronie skrzynia z pulą `zrzut` (rzadka skrzynia albo skrzynia broni). Serwer dostaje komunikat ze współrzędnymi, nad skrzynią unosi się dym, a nieopróżniona znika po 15 minutach. Miejsca zrzutów to punkty z pulą `zrzut` w plikach `.axs`; zwykłe odświeżanie skrzyń je pomija.

Wszystkie struktury mają rozpoznawalne wyposażenie (prawdziwe łóżka, krzesła ze schodków, stoliki, biurka z lampkami, kuchenki, umywalki, toalety, regały, szafy – wspólna klasa `Furniture`), dzikie obozowiska, szkielety, graffiti na tabliczkach i zapalone lampy w ciemnych wnętrzach i pod ziemią. Przy eksporcie klasa `Weathering` postarza każdą strukturę: część cegieł i bruku staje się omszała lub popękana, przy ścianach rośnie mech, na ścianach porosty. Słabszych skrzynek po pokojach jest ok. 1,5 raza więcej niż wcześniej (stacja 48, gospodarstwo 40, kombinat 51, kopalnia 38, jednostka 134, szpital 79).

Zapisuje je do plików:

```bash
./gradlew generujStruktury   # -> build/struktury/*.axs
```

## Budowanie

```bash
./gradlew build              # -> build/libs/AXON-Structure-<wersja>.jar
```

GitHub Actions buduje plugin, generuje pliki struktur i uruchamia prawdziwy serwer Paper 1.21.4, na którym sprawdza wklejanie, generowanie i auto-generowanie.
