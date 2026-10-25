#!/usr/bin/env bash
# Uruchamia prawdziwy serwer Paper 1.21.4 z pluginem i sprawdza wklejanie, generowanie, auto-generowanie,
# ochronę przed wybuchami i zapominanie struktur. Regiony WorldGuard i odstęp od działek sprawdzane są ręcznie.
set -euo pipefail

VERSION=1.21.4
DIR=smoke-server
mkdir -p "$DIR/plugins/AXON-Structure/struktury"
cp build/libs/AXON-Structure-*.jar "$DIR/plugins/"
cp build/struktury/*.axs "$DIR/plugins/AXON-Structure/struktury/"
cd "$DIR"

URL=$(curl -fsS "https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds/latest" \
  | jq -r '.downloads["server:default"].url' 2>/dev/null || true)
if [ -z "$URL" ] || [ "$URL" = "null" ]; then
  BUILD=$(curl -fsS "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds" | jq -r '.builds[-1].build')
  URL="https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/paper-$VERSION-$BUILD.jar"
fi
echo "Paper: $URL"
curl -fsSL -o paper.jar "$URL"

echo "eula=true" > eula.txt
cat > server.properties <<'PROPS'
level-seed=axon-smoke
online-mode=false
spawn-protection=0
view-distance=4
simulation-distance=4
PROPS

# Pierwsze uruchomienie z configem testowym: auto-generowanie przy każdym nowym chunku.
mkdir -p plugins/AXON-Structure
unzip -p ../build/libs/AXON-Structure-*.jar config.yml \
  | sed 's/szansa-procent: 0.5/szansa-procent: 100/; s/min-odleglosc-od-spawnu: 300/min-odleglosc-od-spawnu: 0/; s/min-odleglosc-miedzy: 1000/min-odleglosc-miedzy: 200/; s/maks-roznica-wysokosci: 6/maks-roznica-wysokosci: 12/; s/maks-woda-procent: 3/maks-woda-procent: 10/; s/czas-spadania-sekund: 45/czas-spadania-sekund: 10/' \
  > plugins/AXON-Structure/config.yml

rm -f in && mkfifo in
java -Xms1G -Xmx3G -jar paper.jar --nogui < in > server.log 2>&1 &
PID=$!
exec 3>in

wait_for() { # wzorzec, limit sekund
  local pattern="$1" limit="$2" waited=0
  until grep -q "$pattern" server.log; do
    sleep 2; waited=$((waited + 2))
    if [ "$waited" -ge "$limit" ] || ! kill -0 "$PID" 2>/dev/null; then
      echo "::error::Nie doczekano się: $pattern"
      grep -nE -A3 "ERROR|Could not load|Exception" server.log | head -n 60; tail -n 60 server.log; return 1
    fi
  done
}
cmd() { echo "> $1"; echo "$1" >&3; }

wait_for 'Done (' 600
cmd "struktury lista"
cmd "struktury klasy"
cmd "struktury wklej oilen world 0 90 0 0"
wait_for 'Gotowe!' 120
cmd "struktury generuj oilen world 400 400 1"
wait_gotowe() { # ile postawień, limit sekund
  local waited=0
  until [ "$(grep -c 'Gotowe!' server.log)" -ge "$1" ] || [ "$waited" -ge "$2" ]; do sleep 2; waited=$((waited + 2)); done
}
wait_gotowe 2 180
cmd "struktury wklej gospodarstwo world -300 90 0 2"
wait_gotowe 3 180
cmd "struktury wklej kombinat world 0 90 -300 1"
wait_gotowe 4 240
# Kopalnia ma 12 warstw podziemia: grunt (krata nad szybem) ma wypaść na y 90, drabina w szybie 5 bloków niżej.
cmd "struktury wklej kopalnia world 300 90 -300 0"
wait_gotowe 5 300
cmd "forceload add 294 -300"
sleep 3
cmd "execute if block 295 90 -300 minecraft:iron_trapdoor run say GRUNT-OK"
cmd "execute if block 294 85 -300 minecraft:ladder run say PODZIEMIE-OK"
# Jednostka ma 21 warstw podziemia: posadzka portyku sztabu na y 90, drabina na dnie klatki schronu na y 71.
cmd "struktury wklej jednostka world -300 90 -300 0"
wait_gotowe 6 420
cmd "forceload add -318 -267"
cmd "forceload add -293 -334"
sleep 3
cmd "execute if block -293 90 -334 minecraft:polished_andesite run say JW-GRUNT-OK"
cmd "execute if block -318 71 -267 minecraft:ladder run say JW-SCHRON-OK"
# Szpital ma 9 warstw podziemia: narożnik głównego bloku na y 90, schody do piwnicy na y 86.
cmd "struktury wklej szpital world 300 90 0 0"
wait_gotowe 7 300
cmd "forceload add 275 -5"
cmd "forceload add 277 -1"
sleep 3
cmd "execute if block 275 90 -5 minecraft:smooth_stone run say SZPITAL-GRUNT-OK"
cmd "execute if block 277 86 -1 minecraft:stone_brick_stairs run say SZPITAL-PIWNICA-OK"
# Oaza: strefa bez PvP; lady stanowisk i tarcze strzelnicy muszą stać po wklejeniu (to bloki-znaczniki).
cmd "struktury wklej oaza world 0 90 300 0"
wait_gotowe 8 420
# Kryjówka czarnego rynku: mała struktura z piwnicą; beczka-lada pasera ma stać po wklejeniu.
cmd "struktury wklej czarny-rynek world -300 90 300 0"
wait_gotowe 9 180
cmd "struktury oaza"
# Żyły rudy w kopalni (punkt-ruda), zwierzęta i pusty loot: stan do logu.
cmd "struktury zycie"
sleep 5
# Zrzut zaopatrzenia: spada na lądowisko jednostki albo dach szpitala (czas spadania 10 s).
cmd "struktury zrzut teraz"
sleep 14
# Wybuch TNT nad środkiem stacji Oilen nie może nic zniszczyć, ten sam wybuch poza strukturą tak.
# Środek pierwszej wklejonej stacji (auto-generowanie mogło już coś postawić przy spawnie, więc nie bierzemy pierwszego wpisu).
CX=$(awk '/struktura:/ {s=$2} s=="oilen" && /srodek-x:/ {print $2; exit}' plugins/AXON-Structure/dane.yml)
CZ=$(awk '/struktura:/ {s=$2} s=="oilen" && /srodek-z:/ {print $2; exit}' plugins/AXON-Structure/dane.yml)
OX=$((CX + 120))
cmd "forceload add $CX $CZ"
cmd "forceload add $OX $CZ"
# Chunk poza stacją Oilen ładuje się asynchronicznie i serwer potrafi go długo mielić (wtedy TNT wybucha przed czasem),
# więc dajemy mu czas, zanim cokolwiek postawimy.
sleep 10
cmd "setblock $CX 200 $CZ minecraft:gold_block"
cmd "setblock $OX 200 $CZ minecraft:gold_block"
sleep 2
cmd "execute if block $OX 200 $CZ minecraft:gold_block run say OCHRONA-ZLOTO-OK"
cmd "summon minecraft:tnt $CX 201 $CZ {fuse:5}"
cmd "summon minecraft:tnt $OX 201 $CZ {fuse:5}"
sleep 10
cmd "execute if block $CX 200 $CZ minecraft:gold_block run say OCHRONA-STRUKTURA-OK"
cmd "execute unless block $OX 200 $CZ minecraft:gold_block run say OCHRONA-POZA-OK"
sleep 2

cmd "forceload add 1500 1500 1700 1700"
sleep 60
cmd "forceload add 2500 -2500 2700 -2300"
sleep 60
cmd "save-all"
sleep 5
# Zapominanie struktury.
FORGET=$(grep -oE "^  '?[0-9a-f]{8}'?:$" plugins/AXON-Structure/dane.yml | head -n 1 | tr -d " ':" || true)
cmd "struktury usun $FORGET"
sleep 3
cmd "stop"
wait "$PID" || true

echo "================ log serwera (AXON) ================"
grep -E "AXON|struktur|Gotowe|Auto-generowanie|Teren|skrzyn|Exception|ERROR|WARN.*AXON" server.log || true
echo "================ dane.yml ================"
cat plugins/AXON-Structure/dane.yml || true
FAIL=0
if grep -nE "Exception|Nieprawidłowy stan bloku|nieznany przedmiot|nieznanej klasy|Zła dekoracja|Brak puli|Błąd w pliku" server.log; then
  echo "::error::W logu serwera są błędy"; FAIL=1
fi
GOT=$(grep -c "Gotowe!" server.log || true)
if [ "$GOT" -lt 9 ]; then
  echo "::error::Oczekiwano 9 postawień z komend, jest $GOT"; FAIL=1
fi
grep -qF "Server] GRUNT-OK" server.log || { echo "::error::Kopalnia: grunt nie wypadł na poziomie wklejenia"; FAIL=1; }
grep -qF "Server] PODZIEMIE-OK" server.log || { echo "::error::Kopalnia: brak drabiny w szybie pod ziemią"; FAIL=1; }
grep -qF "Server] JW-GRUNT-OK" server.log || { echo "::error::Jednostka: grunt nie wypadł na poziomie wklejenia"; FAIL=1; }
grep -qF "Server] JW-SCHRON-OK" server.log || { echo "::error::Jednostka: brak drabiny na poziomie 2 schronu"; FAIL=1; }
grep -qF "Server] SZPITAL-GRUNT-OK" server.log || { echo "::error::Szpital: grunt nie wypadł na poziomie wklejenia"; FAIL=1; }
grep -qF "Server] SZPITAL-PIWNICA-OK" server.log || { echo "::error::Szpital: brak schodów do piwnicy"; FAIL=1; }
grep -qF "Server] OCHRONA-ZLOTO-OK" server.log || { echo "::error::Złoty blok poza strukturą nie został postawiony (chunk się nie załadował)"; FAIL=1; }
grep -qF "Server] OCHRONA-STRUKTURA-OK" server.log || { echo "::error::Wybuch zniszczył blok w strukturze"; FAIL=1; }
grep -qF "Server] OCHRONA-POZA-OK" server.log || { echo "::error::Test wybuchu poza strukturą nie zadziałał"; FAIL=1; }
# Wszystkie lady stoją (N/N); auto-generowanie może dołożyć własną kryjówkę czarnego rynku, więc paserów jest co najmniej 1.
grep -qE "Oaza: lady i tarcze na miejscu (3[1-9])/\1, stanowisk wymiany 8, lad paserów [1-9]" server.log || { echo "::error::Oaza/czarny rynek: brak lad, tarcz albo stanowisk"; grep "Oaza:" server.log; FAIL=1; }
grep -q "Kopalnia: żył rudy" server.log || { echo "::error::Komenda zycie nie zadziałała"; FAIL=1; }
if grep -q "Kopalnia: żył rudy 0," server.log; then
  echo "::warning::Postawiona kopalnia nie ma żył rudy (punkt-ruda)"
fi
grep -q "Zrzut wylądował" server.log || { echo "::error::Zrzut zaopatrzenia nie wylądował"; FAIL=1; }
grep -q "Zapomniano" server.log || { echo "::error::Komenda usun nie zadziałała"; FAIL=1; }
if grep -q "Auto-generowanie: postawiono" server.log; then
  echo "Auto-generowanie zadziałało."
else
  echo "::warning::Auto-generowanie nie znalazło dobrego terenu w testowanym obszarze"
fi
exit $FAIL
