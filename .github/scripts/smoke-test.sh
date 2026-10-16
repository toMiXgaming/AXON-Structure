#!/usr/bin/env bash
# Uruchamia prawdziwy serwer Paper 1.21.4 z pluginem i sprawdza wklejanie, generowanie i auto-generowanie.
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
      echo "::error::Nie doczekano się: $pattern"; tail -n 80 server.log; return 1
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
cmd "forceload add 1500 1500 1700 1700"
sleep 60
cmd "forceload add 2500 -2500 2700 -2300"
sleep 60
cmd "save-all"
sleep 5
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
if [ "$GOT" -lt 2 ]; then
  echo "::error::Oczekiwano 2 postawień z komend, jest $GOT"; FAIL=1
fi
if grep -q "Auto-generowanie: postawiono" server.log; then
  echo "Auto-generowanie zadziałało."
else
  echo "::warning::Auto-generowanie nie znalazło dobrego terenu w testowanym obszarze"
fi
exit $FAIL
