#!/usr/bin/env bash
#
# Unified performance benchmark script.
# Collects all configuration up front, builds once, then runs selected
# metric scripts (memory, CPU, battery) back-to-back without user intervention.
#
# Usage: ./measure_all.sh

set -euo pipefail

# --- Wrapper to prevent adb from consuming script stdin ---
adb() { command adb "$@" < /dev/null; }

# --- Java 17 auto-detection ---

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"17\.' ; then
    if command -v /usr/libexec/java_home &>/dev/null; then
        DETECTED=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
    else
        DETECTED=$(update-java-alternatives -l 2>/dev/null | grep '17' | awk '{print $3}' | head -1 || true)
    fi
    if [[ -n "$DETECTED" && -d "$DETECTED" ]]; then
        export JAVA_HOME="$DETECTED"
        echo "Auto-detected Java 17: $JAVA_HOME"
    else
        echo "ERROR: Java 17 is required but not found. Set JAVA_HOME to a JDK 17 installation."
        exit 1
    fi
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PKG="tv.vizbee.demo"

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
BRANCH=$(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')

echo "================================================"
echo "  Unified Performance Benchmark"
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $BRANCH"
echo "================================================"
echo ""

# --- Helper: yes/no prompt ---

ask_yn() {
    local prompt=$1
    local default=$2
    local input
    read -rp "$prompt [$default]: " input
    input="${input:-$default}"
    [[ "$input" =~ ^[Yy] ]]
}

# =============================================
# 1. Select which metrics to run
# =============================================

echo "--- Select metrics to run ---"
echo ""

RUN_MEMORY=false
RUN_CPU=false
RUN_BATTERY=false

ask_yn "Run MEMORY benchmark? (~16 min with defaults)" "y" && RUN_MEMORY=true
ask_yn "Run CPU benchmark? (~35 min with defaults)" "y"     && RUN_CPU=true
ask_yn "Run BATTERY benchmark? (~102 min with defaults)" "y" && RUN_BATTERY=true
echo ""

if [[ "$RUN_MEMORY" == false && "$RUN_CPU" == false && "$RUN_BATTERY" == false ]]; then
    echo "No metrics selected. Exiting."
    exit 0
fi

# =============================================
# 2. Collect shared parameters
# =============================================

echo "--- Shared parameters ---"
echo ""
read -rp "Chromecast device name [CC with Google TV TV 2]: " DEVICE_NAME
DEVICE_NAME="${DEVICE_NAME:-CC with Google TV TV 2}"

read -rp "Initial settling time after launch, seconds [60]: " SETTLE_TIME
SETTLE_TIME="${SETTLE_TIME:-60}"

read -rp "Wait time after cast icon click (discovery), seconds [10]: " DISC_ACTION_WAIT
DISC_ACTION_WAIT="${DISC_ACTION_WAIT:-10}"

read -rp "Wait time after casting starts (connected), seconds [15]: " CONN_ACTION_WAIT
CONN_ACTION_WAIT="${CONN_ACTION_WAIT:-15}"
echo ""

# =============================================
# 3. Collect per-metric parameters
# =============================================

# --- Memory ---
if [[ "$RUN_MEMORY" == true ]]; then
    MEM_IDLE_ITERS=5
    MEM_DISC_ITERS=5
    MEM_CONN_ITERS=5

    echo "--- Memory configuration (defaults: idle=5, disc=5, conn=5) ---"
    if ! ask_yn "  Use defaults?" "y"; then
        read -rp "  Iterations for IDLE scenario [5]: " MEM_IDLE_ITERS
        MEM_IDLE_ITERS="${MEM_IDLE_ITERS:-5}"
        read -rp "  Iterations for DISCOVERY scenario [5]: " MEM_DISC_ITERS
        MEM_DISC_ITERS="${MEM_DISC_ITERS:-5}"
        read -rp "  Iterations for CONNECTED scenario [5]: " MEM_CONN_ITERS
        MEM_CONN_ITERS="${MEM_CONN_ITERS:-5}"
    fi
    echo ""
fi

# --- CPU ---
if [[ "$RUN_CPU" == true ]]; then
    CPU_IDLE_ITERS=5
    CPU_DISC_ITERS=5
    CPU_CONN_ITERS=5
    CPU_TRACE_DURATION=60

    echo "--- CPU configuration (defaults: idle=5, disc=5, conn=5, trace=60s) ---"
    if ! ask_yn "  Use defaults?" "y"; then
        read -rp "  Iterations for IDLE scenario [5]: " CPU_IDLE_ITERS
        CPU_IDLE_ITERS="${CPU_IDLE_ITERS:-5}"
        read -rp "  Iterations for DISCOVERY scenario [5]: " CPU_DISC_ITERS
        CPU_DISC_ITERS="${CPU_DISC_ITERS:-5}"
        read -rp "  Iterations for CONNECTED scenario [5]: " CPU_CONN_ITERS
        CPU_CONN_ITERS="${CPU_CONN_ITERS:-5}"
        read -rp "  Perfetto trace duration, seconds [60]: " CPU_TRACE_DURATION
        CPU_TRACE_DURATION="${CPU_TRACE_DURATION:-60}"
    fi
    echo ""
fi

# --- Battery ---
if [[ "$RUN_BATTERY" == true ]]; then
    BAT_IDLE_ITERS=3
    BAT_DISC_ITERS=3
    BAT_CONN_ITERS=3
    BAT_MEASURE_DURATION=600

    echo "--- Battery configuration (defaults: idle=3, disc=3, conn=3, duration=600s / 10 min) ---"
    if ! ask_yn "  Use defaults?" "y"; then
        read -rp "  Iterations for IDLE scenario [3]: " BAT_IDLE_ITERS
        BAT_IDLE_ITERS="${BAT_IDLE_ITERS:-3}"
        read -rp "  Iterations for DISCOVERY scenario [3]: " BAT_DISC_ITERS
        BAT_DISC_ITERS="${BAT_DISC_ITERS:-3}"
        read -rp "  Iterations for CONNECTED scenario [3]: " BAT_CONN_ITERS
        BAT_CONN_ITERS="${BAT_CONN_ITERS:-3}"
        read -rp "  Measurement duration, seconds [600]: " BAT_MEASURE_DURATION
        BAT_MEASURE_DURATION="${BAT_MEASURE_DURATION:-600}"
    fi
    echo ""
fi

# =============================================
# 4. Estimate total time & confirm
# =============================================

est_total=0

if [[ "$RUN_MEMORY" == true ]]; then
    est_mem=$(( MEM_IDLE_ITERS*(SETTLE_TIME+2) + MEM_DISC_ITERS*(SETTLE_TIME+DISC_ACTION_WAIT+12) + MEM_CONN_ITERS*(SETTLE_TIME+CONN_ACTION_WAIT+12) ))
    est_total=$((est_total + est_mem))
fi

if [[ "$RUN_CPU" == true ]]; then
    est_cpu=$(( CPU_IDLE_ITERS*(SETTLE_TIME+CPU_TRACE_DURATION+4) + CPU_DISC_ITERS*(SETTLE_TIME+DISC_ACTION_WAIT+CPU_TRACE_DURATION+14) + CPU_CONN_ITERS*(SETTLE_TIME+CONN_ACTION_WAIT+CPU_TRACE_DURATION+14) ))
    est_total=$((est_total + est_cpu))
fi

if [[ "$RUN_BATTERY" == true ]]; then
    est_bat=$(( BAT_IDLE_ITERS*(SETTLE_TIME+BAT_MEASURE_DURATION+4) + BAT_DISC_ITERS*(SETTLE_TIME+DISC_ACTION_WAIT+BAT_MEASURE_DURATION+14) + BAT_CONN_ITERS*(SETTLE_TIME+CONN_ACTION_WAIT+BAT_MEASURE_DURATION+14) ))
    est_total=$((est_total + est_bat))
fi

est_min=$((est_total / 60))

echo "================================================"
echo "  Summary"
echo "================================================"
echo "  Device:            $DEVICE (Android $ANDROID_VER)"
echo "  Branch:            $BRANCH"
echo "  Device name:       $DEVICE_NAME"
echo "  Settle time:       ${SETTLE_TIME}s"
echo "  Discovery wait:    ${DISC_ACTION_WAIT}s"
echo "  Cast settle:       ${CONN_ACTION_WAIT}s"
echo ""
if [[ "$RUN_MEMORY" == true ]]; then
    echo "  Memory:  idle=${MEM_IDLE_ITERS} disc=${MEM_DISC_ITERS} conn=${MEM_CONN_ITERS}"
fi
if [[ "$RUN_CPU" == true ]]; then
    echo "  CPU:     idle=${CPU_IDLE_ITERS} disc=${CPU_DISC_ITERS} conn=${CPU_CONN_ITERS}  trace=${CPU_TRACE_DURATION}s"
fi
if [[ "$RUN_BATTERY" == true ]]; then
    echo "  Battery: idle=${BAT_IDLE_ITERS} disc=${BAT_DISC_ITERS} conn=${BAT_CONN_ITERS}  duration=${BAT_MEASURE_DURATION}s"
fi
echo ""
echo "  Estimated total time: ~${est_min} min"
echo "================================================"
echo ""
read -rp "Press ENTER to start (builds & installs the app, then runs all selected benchmarks)..."
echo ""

# =============================================
# 5. Build & install ONCE
# =============================================

echo "Building benchmark APK + test APK..."
cd "$PROJECT_DIR"
./gradlew app:clean app:assembleBenchmark app:assembleDebugAndroidTest -q
echo "Installing app APK..."
adb uninstall "$PKG" > /dev/null 2>&1 || true
adb uninstall "${PKG}.test" > /dev/null 2>&1 || true
adb install app/build/outputs/apk/benchmark/app-benchmark.apk
echo "Installing test APK..."
adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
echo "Installed."
echo ""

# =============================================
# 6. Run each selected metric
# =============================================

FAILED_METRICS=()

if [[ "$RUN_MEMORY" == true ]]; then
    echo ""
    echo "############################################"
    echo "#  Starting: MEMORY benchmark"
    echo "############################################"
    echo ""
    if ! bash "$SCRIPT_DIR/measure_memory.sh" \
        --skip-build \
        --device-name "$DEVICE_NAME" \
        --idle-iters "$MEM_IDLE_ITERS" \
        --disc-iters "$MEM_DISC_ITERS" \
        --conn-iters "$MEM_CONN_ITERS" \
        --settle-time "$SETTLE_TIME" \
        --disc-action-wait "$DISC_ACTION_WAIT" \
        --conn-action-wait "$CONN_ACTION_WAIT"; then
        echo "  WARNING: Memory benchmark exited with errors."
        FAILED_METRICS+=("memory")
    fi
fi

if [[ "$RUN_CPU" == true ]]; then
    echo ""
    echo "############################################"
    echo "#  Starting: CPU benchmark"
    echo "############################################"
    echo ""
    if ! bash "$SCRIPT_DIR/measure_cpu.sh" \
        --skip-build \
        --device-name "$DEVICE_NAME" \
        --idle-iters "$CPU_IDLE_ITERS" \
        --disc-iters "$CPU_DISC_ITERS" \
        --conn-iters "$CPU_CONN_ITERS" \
        --settle-time "$SETTLE_TIME" \
        --trace-duration "$CPU_TRACE_DURATION" \
        --disc-action-wait "$DISC_ACTION_WAIT" \
        --conn-action-wait "$CONN_ACTION_WAIT"; then
        echo "  WARNING: CPU benchmark exited with errors."
        FAILED_METRICS+=("cpu")
    fi
fi

if [[ "$RUN_BATTERY" == true ]]; then
    echo ""
    echo "############################################"
    echo "#  Starting: BATTERY benchmark"
    echo "############################################"
    echo ""
    if ! bash "$SCRIPT_DIR/measure_battery.sh" \
        --skip-build \
        --device-name "$DEVICE_NAME" \
        --idle-iters "$BAT_IDLE_ITERS" \
        --disc-iters "$BAT_DISC_ITERS" \
        --conn-iters "$BAT_CONN_ITERS" \
        --settle-time "$SETTLE_TIME" \
        --measure-duration "$BAT_MEASURE_DURATION" \
        --disc-action-wait "$DISC_ACTION_WAIT" \
        --conn-action-wait "$CONN_ACTION_WAIT"; then
        echo "  WARNING: Battery benchmark exited with errors."
        FAILED_METRICS+=("battery")
    fi
fi

# =============================================
# 7. Final summary
# =============================================

echo ""
echo "================================================"
echo "  All benchmarks complete."
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $BRANCH"
if [[ ${#FAILED_METRICS[@]} -gt 0 ]]; then
    echo "  Warnings: ${FAILED_METRICS[*]}"
fi
echo "================================================"
