#!/usr/bin/env bash
# Uruchamia prawdziwy serwer Paper 1.21.4 z pluginem i WorldGuardem i sprawdza wklejanie, generowanie,
# auto-generowanie, regiony struktur, odstęp od działek i ochronę przed wybuchami.
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

# WorldEdit i WorldGuard z Modrinth (wersje dla Paper/Bukkit na 1.21.4).
# WorldGuard 7.0.x działa tylko z WorldEdit 7.3.x, więc wersje są przypięte prefiksem.
modrinth() { # projekt, prefiks wersji
  local project="$1" prefix="$2" url="" gv
  for gv in 1.21.4 1.21.3 1.21; do
    url=$(curl -fsS -G "https://api.modrinth.com/v2/project/$project/version" \
      --data-urlencode "game_versions=[\"$gv\"]" --data-urlencode 'loaders=["paper","bukkit","spigot"]' \
      | jq -r --arg p "$prefix" '[.[] | select(.version_type == "release" and (.version_number | startswith($p)))][0].files
          | (map(select(.primary)) + .)[0].url // empty')
    [ -n "$url" ] && break
  done
  [ -n "$url" ] || { echo "::error::Nie znaleziono $project na Modrinth"; exit 1; }
  echo "$project: $url"
  curl -fsSL -o "plugins/$project.jar" "$url"
}
modrinth worldedit 7.3.
modrinth worldguard 7.0.

# Działka AxonPlot (region dzialka_*) wpisana wprost do pliku regionów WorldGuarda.
mkdir -p plugins/WorldGuard/worlds/world
cat > plugins/WorldGuard/worlds/world/regions.yml <<'REGIONS'
regions:
  dzialka_test:
    type: cuboid
    min: {x: -1007.0, y: -64.0, z: -1007.0}
    max: {x: -993.0, y: 320.0, z: -993.0}
    priority: 0
    flags: {}
    owners: {}
    members: {}
REGIONS

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
  | sed 's/szansa-procent: 0.5/szansa-procent: 100/; s/min-odleglosc-od-spawnu: 300/min-odleglosc-od-spawnu: 0/; s/min-odleglosc-miedzy: 1000/min-odleglosc-miedzy: 200/; s/maks-roznica-wysokosci: 6/maks-roznica-wysokosci: 12/; s/maks-woda-procent: 3/maks-woda-procent: 10/' \
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
cmd "struktury wklej stacja world 0 90 0 0"
wait_for 'Gotowe!' 120
cmd "struktury generuj stacja world 400 400 1"
waited=0
until [ "$(grep -c 'Gotowe!' server.log)" -ge 2 ] || [ "$waited" -ge 180 ]; do sleep 2; waited=$((waited + 2)); done
# Działka ma środek w -1000 -1000 (promień do 25), struktura ma ~52 kratki, odstęp 50: ma odmówić.
cmd "struktury generuj stacja world -1000 -1080 0"
wait_for 'Za blisko działki' 60

# Wybuch TNT nad środkiem stacji nie może nic zniszczyć, ten sam wybuch poza strukturą tak.
CX=$(awk '/srodek-x:/ {print $2; exit}' plugins/AXON-Structure/dane.yml)
CZ=$(awk '/srodek-z:/ {print $2; exit}' plugins/AXON-Structure/dane.yml)
OX=$((CX + 120))
cmd "forceload add $CX $CZ"
cmd "forceload add $OX $CZ"
sleep 3
cmd "setblock $CX 200 $CZ minecraft:gold_block"
cmd "setblock $OX 200 $CZ minecraft:gold_block"
cmd "summon minecraft:tnt $CX 201 $CZ {fuse:5}"
cmd "summon minecraft:tnt $OX 201 $CZ {fuse:5}"
sleep 4
cmd "execute if block $CX 200 $CZ minecraft:gold_block run say OCHRONA-STRUKTURA-OK"
cmd "execute unless block $OX 200 $CZ minecraft:gold_block run say OCHRONA-POZA-OK"
sleep 2

cmd "forceload add 1500 1500 1700 1700"
sleep 60
cmd "forceload add 2500 -2500 2700 -2300"
sleep 60
cmd "save-all"
sleep 5
# Zapominanie struktury zdejmuje jej region.
FORGET=$(grep -oE "^  '?[0-9a-f]{8}'?:$" plugins/AXON-Structure/dane.yml | head -n 1 | tr -d " ':" || true)
cmd "struktury usun $FORGET"
sleep 3
cmd "stop"
wait "$PID" || true

echo "================ log serwera (AXON) ================"
grep -E "AXON|struktur|Gotowe|Auto-generowanie|Teren|skrzyn|Exception|ERROR|WARN.*AXON" server.log || true
echo "================ dane.yml ================"
cat plugins/AXON-Structure/dane.yml || true
echo "================ regiony WorldGuard ================"
grep -E "^ +(struktura|dzialka)_[a-z_0-9]+:|pvp|x:" plugins/WorldGuard/worlds/world/regions.yml || true

FAIL=0
if grep -nE "Exception|Nieprawidłowy stan bloku|nieznany przedmiot|nieznanej klasy|Zła dekoracja|Brak puli|Błąd w pliku" server.log; then
  echo "::error::W logu serwera są błędy"; FAIL=1
fi
GOT=$(grep -c "Gotowe!" server.log || true)
if [ "$GOT" -lt 2 ]; then
  echo "::error::Oczekiwano 2 postawień z komend, jest $GOT"; FAIL=1
fi
grep -q "WorldGuard wykryty" server.log || { echo "::error::Plugin nie podpiął WorldGuarda"; FAIL=1; }
grep -q "Za blisko działki.*dzialka_test" server.log \
  || { echo "::error::Struktura stanęła przy działce"; FAIL=1; }
grep -qF "Server] OCHRONA-STRUKTURA-OK" server.log || { echo "::error::Wybuch zniszczył blok w strukturze"; FAIL=1; }
grep -qF "Server] OCHRONA-POZA-OK" server.log || { echo "::error::Test wybuchu poza strukturą nie zadziałał"; FAIL=1; }
REGIONS=plugins/WorldGuard/worlds/world/regions.yml
STRUCTS=$(grep -cE "^ +struktura_[0-9a-f]+:" "$REGIONS" || true)
INSTANCES=$(grep -cE "^  '?[0-9a-f]{8}'?:$" plugins/AXON-Structure/dane.yml || true)
if [ "$STRUCTS" -ne "$INSTANCES" ] || [ "$STRUCTS" -lt 1 ]; then
  echo "::error::Regionów struktura_: $STRUCTS, struktur w dane.yml: $INSTANCES"; FAIL=1
fi
if [ -n "$FORGET" ] && grep -q "struktura_$FORGET" "$REGIONS"; then
  echo "::error::Region zapomnianej struktury $FORGET został"; FAIL=1
fi
grep -q "Zapomniano" server.log || { echo "::error::Komenda usun nie zadziałała"; FAIL=1; }
if grep -q "Auto-generowanie: postawiono" server.log; then
  echo "Auto-generowanie zadziałało."
else
  echo "::warning::Auto-generowanie nie znalazło dobrego terenu w testowanym obszarze"
fi
exit $FAIL
