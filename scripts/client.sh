#!/bin/sh
set -e

N="${1:?usage: scripts/client.sh <instance number> [username] [width] [height]}"
USERNAME="${2:-Agent$N}"
WIDTH="${3:-1600}"
HEIGHT="${4:-900}"
PORT=$((25592 + N))

ROOT="$(cd "$(dirname "$0")/.." && pwd -W)"
LAUNCH_CFG="$ROOT/.gradle/loom-cache/projects/fabric/launch.cfg"
ARG_FILE="$ROOT/fabric/build/loom-cache/argFiles/runClient"
GAME_DIR="$ROOT/fabric/runs/$N"

if [ ! -f "$LAUNCH_CFG" ] || [ ! -f "$ARG_FILE" ]; then
    echo "Missing Loom launch files. Run ./gradlew build and ./gradlew :fabric:runClient once first." >&2
    exit 1
fi

mkdir -p "$GAME_DIR"
cd "$GAME_DIR"
exec "${JAVA:-java}" \
    "-Dcomprehension.port=$PORT" \
    "-Dfabric.dli.config=$LAUNCH_CFG" \
    -Dfabric.dli.env=client \
    -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient \
    "@$ARG_FILE" \
    -XX:StackShadowPages=32 \
    --sun-misc-unsafe-memory-access=allow \
    --enable-native-access=ALL-UNNAMED \
    -Dfile.encoding=UTF-8 \
    net.fabricmc.devlaunchinjector.Main \
    --gameDir "$GAME_DIR" \
    --width "$WIDTH" \
    --height "$HEIGHT" \
    --username "$USERNAME"
