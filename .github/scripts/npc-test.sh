#!/usr/bin/env bash
# Strażnicy NPC: serwer Paper z Citizens i Sentinelem (Jenkins Citizens albo ich Maven; na Modrinth ich nie ma), wklejenie jednostki
# i szpitala, postawienie strażników komendą i sprawdzenie, że stoją. Gdy nie da się pobrać Citizens/Sentinela,
# test jest pomijany (ostrzeżenie), bo to zewnętrzne pluginy.
set -euo pipefail

DIR=smoke-npc
mkdir -p "$DIR/plugins/AXON-Structure/struktury"
cp build/libs/AXON-Structure-*.jar "$DIR/plugins/"
cp build/struktury/jednostka.axs build/struktury/szpital.axs "$DIR/plugins/AXON-Structure/struktury/"
cd "$DIR"

UA="Mozilla/5.0 (X11; Linux x86_64) AXON-Structure-CI"
fetch_jenkins() { # plik docelowy, nazwy zadań Jenkinsa do wypróbowania
  local out="$1" job json path
  shift
  for job in "$@"; do
    json=$(curl -fsSL -A "$UA" "https://ci.citizensnpcs.co/job/$job/lastSuccessfulBuild/api/json") || { echo "jenkins $job: brak"; continue; }
    path=$(echo "$json" | jq -r '[.artifacts[].relativePath | select(endswith(".jar")) | select(test("sources|javadoc") | not)][0]')
    if [ -n "$path" ] && [ "$path" != "null" ]; then
      echo "jenkins $job: $path"
      curl -fsSL -A "$UA" -o "$out" "https://ci.citizensnpcs.co/job/$job/lastSuccessfulBuild/artifact/$path" && return 0
    fi
  done
  return 1
}
fetch_sentinel_maven() { # Sentinel jest też w repozytorium Mavena Citizens (sam plugin, bez cieniowania)
  local base="https://maven.citizensnpcs.co/repo/org/mcmonkey/sentinel" ver snap file
  ver=$(curl -fsSL -A "$UA" "$base/maven-metadata.xml" | grep -oP '(?<=<latest>)[^<]+') || { echo "maven sentinel: brak"; return 1; }
  if [[ "$ver" == *SNAPSHOT ]]; then
    snap=$(curl -fsSL -A "$UA" "$base/$ver/maven-metadata.xml" | grep -oP '(?<=<value>)[^<]+' | head -n 1) || return 1
    file="sentinel-$snap.jar"
  else
    file="sentinel-$ver.jar"
  fi
  echo "maven sentinel: $ver/$file"
  curl -fsSL -A "$UA" -o "$1" "$base/$ver/$file"
}
get_citizens() { fetch_jenkins "$1" citizens2 Citizens2 Citizens; }
get_sentinel() { fetch_jenkins "$1" sentinel Sentinel || fetch_sentinel_maven "$1"; }
if ! get_citizens plugins/Citizens.jar || ! get_sentinel plugins/Sentinel.jar; then
  echo "::warning::Nie udało się pobrać Citizens/Sentinela – test strażników pominięty"
  exit 0
fi

cp ../smoke-server/paper.jar . 2>/dev/null || {
  BUILD=$(curl -fsS "https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds" | jq -r '.builds[-1].build')
  curl -fsSL -o paper.jar "https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/$BUILD/downloads/paper-1.21.4-$BUILD.jar"
}
echo "eula=true" > eula.txt
cat > server.properties <<'PROPS'
level-seed=axon-npc
online-mode=false
spawn-protection=0
view-distance=4
simulation-distance=4
PROPS
unzip -p ../build/libs/AXON-Structure-*.jar config.yml | sed 's/^  wlaczone: true$/  wlaczone: false/' \
  > plugins/AXON-Structure/config.yml

rm -f in && mkfifo in
java -Xms1G -Xmx3G -jar paper.jar --nogui < in > server.log 2>&1 &
PID=$!
exec 3>in
cmd() { echo "> $1"; echo "$1" >&3; }
wait_for() {
  local waited=0
  until grep -q "$1" server.log; do
    sleep 2; waited=$((waited + 2))
    if [ "$waited" -ge "$2" ] || ! kill -0 "$PID" 2>/dev/null; then
      echo "::error::Nie doczekano się: $1"; tail -n 80 server.log; return 1
    fi
  done
}

wait_for 'Done (' 600
if ! grep -q "Enabling Citizens" server.log || ! grep -q "Enabling Sentinel" server.log; then
  echo "::warning::Citizens albo Sentinel nie wystartowały na Paper 1.21.4 – test strażników pominięty"
  grep -nE "Citizens|Sentinel" server.log | head -n 20 || true
  cmd "stop"; wait "$PID" || true; exit 0
fi
cmd "struktury wklej jednostka world -300 90 -300 0"
wait_for 'Gotowe!' 420
cmd "struktury wklej szpital world 300 90 0 0"
until [ "$(grep -c 'Gotowe!' server.log)" -ge 2 ]; do sleep 2; done
cmd "forceload add -340 -345 -300 -250"
cmd "forceload add 280 -10 310 10"
sleep 5
cmd "struktury straznicy postaw"
sleep 5
cmd "struktury straznicy"
cmd "npc list"
sleep 3
cmd "stop"
wait "$PID" || true

echo "================ log serwera (strażnicy) ================"
grep -nE "AXON|Strażni|strażni|Citizens|Sentinel|Exception|ERROR" server.log | head -n 80 || true
FAIL=0
if grep -nE "pl\.axon|Nie udało się postawić strażnika|Nieprawidłowy stan bloku|Brak puli" server.log; then
  echo "::error::Błędy pluginu przy strażnikach"; FAIL=1
fi
PLACED=$(grep -oE "Strażnicy: postawiono [0-9-]+" server.log | tail -n 1 | grep -oE "[0-9-]+$" || echo 0)
if [ "${PLACED:-0}" -lt 7 ]; then
  echo "::error::Oczekiwano 7 strażników (4 w jednostce, 3 w szpitalu), postawiono ${PLACED:-0}"; FAIL=1
fi
exit $FAIL
