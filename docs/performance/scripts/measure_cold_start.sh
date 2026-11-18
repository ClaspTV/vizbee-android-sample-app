#!/usr/bin/env bash
#
# Measures cold-start time of the app over multiple iterations.
# Builds the app, installs it on the connected device, and runs cold starts.
#
# Usage: ./measure_cold_start.sh [iterations]
#   iterations  Number of cold starts to measure (default: 5)

set -euo pipefail

ITERATIONS="${1:-5}"
PKG="tv.vizbee.demo"
ACTIVITY=".activity.MainActivity"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "Device: $DEVICE (Android $ANDROID_VER)"
echo ""

# --- Build & Install ---

echo "Building benchmark APK..."
cd "$PROJECT_DIR"
./gradlew app:assembleBenchmark -q
echo "Installing..."
adb uninstall "$PKG" > /dev/null 2>&1 || true
adb install app/build/outputs/apk/benchmark/app-benchmark.apk > /dev/null 2>&1
echo "Installed."
echo ""

# --- Measure ---

echo "Running $ITERATIONS cold start iterations..."
echo ""

TIMES=()
for i in $(seq 1 "$ITERATIONS"); do
    adb shell am force-stop "$PKG"
    sleep 1
    OUTPUT=$(adb shell am start-activity -W -S "$PKG/$ACTIVITY" 2>&1)
    TOTAL_TIME=$(echo "$OUTPUT" | grep "TotalTime" | awk '{print $2}' | tr -d '\r')
    LAUNCH_STATE=$(echo "$OUTPUT" | grep "LaunchState" | awk '{print $2}' | tr -d '\r')
    echo "  Iteration $i: ${TOTAL_TIME} ms ($LAUNCH_STATE)"
    TIMES+=("$TOTAL_TIME")
done

# --- Results ---

echo ""
echo "=== Results ==="

# Sort for median/p90
SORTED=($(printf '%s\n' "${TIMES[@]}" | sort -n))
COUNT=${#SORTED[@]}
SUM=0
for t in "${TIMES[@]}"; do SUM=$((SUM + t)); done
AVG=$((SUM / COUNT))

# Median (p50)
MID=$((COUNT / 2))
if ((COUNT % 2 == 0)); then
    P50=$(( (SORTED[MID - 1] + SORTED[MID]) / 2 ))
else
    P50=${SORTED[$MID]}
fi

# P90
P90_IDX=$(echo "$COUNT * 9 / 10" | bc)
if ((P90_IDX >= COUNT)); then P90_IDX=$((COUNT - 1)); fi
P90=${SORTED[$P90_IDX]}

echo "  Iterations: $COUNT"
echo "  Values: ${TIMES[*]} ms"
echo "  Avg:  ${AVG} ms"
echo "  P50:  ${P50} ms"
echo "  P90:  ${P90} ms"
echo ""
echo "Device: $DEVICE (Android $ANDROID_VER)"
