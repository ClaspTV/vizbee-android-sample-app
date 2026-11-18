#!/usr/bin/env bash
#
# Measures memory footprint (TOTAL PSS, Java Heap, Native Heap) of the app
# across idle, discovery, and connected scenarios.
#
# The script prompts for all configuration up front, then runs all three
# scenarios back-to-back. Discovery and connected states are automated via
# a UIAutomator instrumentation test that clicks the cast icon and selects
# the target device.
#
# Usage: ./measure_memory.sh

set -euo pipefail

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

PKG="tv.vizbee.demo"
ACTIVITY=".activity.MainActivity"
TEST_CLASS="tv.vizbee.demo.MemoryBenchmarkHelper"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "================================================"
echo "  Memory Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Configuration prompts ---

read -rp "Chromecast device name (as shown in Vizbee device list): " DEVICE_NAME
echo ""

read -rp "Iterations for IDLE scenario [30]: " IDLE_ITERS
IDLE_ITERS="${IDLE_ITERS:-30}"
read -rp "Iterations for DISCOVERY scenario [10]: " DISC_ITERS
DISC_ITERS="${DISC_ITERS:-10}"
read -rp "Iterations for CONNECTED scenario [10]: " CONN_ITERS
CONN_ITERS="${CONN_ITERS:-10}"
echo ""

read -rp "Initial settling time after launch, seconds [60]: " SETTLE_TIME
SETTLE_TIME="${SETTLE_TIME:-60}"
read -rp "Wait time after cast icon click (discovery), seconds [10]: " DISC_ACTION_WAIT
DISC_ACTION_WAIT="${DISC_ACTION_WAIT:-10}"
read -rp "Wait time after device connection (connected), seconds [15]: " CONN_ACTION_WAIT
CONN_ACTION_WAIT="${CONN_ACTION_WAIT:-15}"
echo ""

echo "--- Configuration ---"
echo "  Device name:       $DEVICE_NAME"
echo "  Idle iterations:   $IDLE_ITERS"
echo "  Disc iterations:   $DISC_ITERS"
echo "  Conn iterations:   $CONN_ITERS"
echo "  Settle time:       ${SETTLE_TIME}s"
echo "  Discovery wait:    ${DISC_ACTION_WAIT}s"
echo "  Connected wait:    ${CONN_ACTION_WAIT}s"
echo "---------------------"
echo ""
read -rp "Press ENTER to start (builds & installs the app)..."
echo ""

# --- Build & Install ---

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

# --- Helpers ---

countdown() {
    local secs=$1
    local label="${2:-Waiting}"
    for ((i = secs; i > 0; i--)); do
        printf "\r  %s: %3ds remaining..." "$label" "$i"
        sleep 1
    done
    printf "\r  %s: done.              \n" "$label"
}

parse_meminfo() {
    local total_pss="" java_heap="" native_heap=""
    while IFS= read -r line; do
        if [[ "$line" =~ ^[[:space:]]*TOTAL\ PSS: ]]; then
            total_pss=$(echo "$line" | awk '{print $3}')
        elif [[ "$line" =~ ^[[:space:]]*TOTAL\ SWAP\ PSS: ]]; then
            : # skip
        elif [[ "$line" =~ Java\ Heap: ]]; then
            java_heap=$(echo "$line" | awk '{print $3}')
        elif [[ "$line" =~ Native\ Heap: ]]; then
            native_heap=$(echo "$line" | awk '{print $3}')
        fi
    done
    echo "$total_pss $java_heap $native_heap"
}

compute_stats() {
    local label=$1
    shift
    local values=("$@")
    local sorted=($(printf '%s\n' "${values[@]}" | sort -n))
    local count=${#sorted[@]}

    local sum=0
    for v in "${values[@]}"; do sum=$((sum + v)); done
    local avg=$((sum / count))

    local mid=$((count / 2))
    local p50
    if ((count % 2 == 0)); then
        p50=$(( (sorted[mid - 1] + sorted[mid]) / 2 ))
    else
        p50=${sorted[$mid]}
    fi

    local p90_idx=$(echo "$count * 9 / 10" | bc)
    if ((p90_idx >= count)); then p90_idx=$((count - 1)); fi
    local p90=${sorted[$p90_idx]}

    local avg_mb=$(echo "scale=1; $avg / 1024" | bc)
    local p50_mb=$(echo "scale=1; $p50 / 1024" | bc)
    local p90_mb=$(echo "scale=1; $p90 / 1024" | bc)

    echo "  $label:"
    echo "    Raw (KB): ${values[*]}"
    echo "    Avg:  ${avg_mb} MB"
    echo "    P50:  ${p50_mb} MB"
    echo "    P90:  ${p90_mb} MB"
}

# Runs the instrumentation test to put the app in discovery/connected state.
# The test stays alive (sleeping) so the state is maintained. We capture
# meminfo, then force-stop the app which also kills the test.
run_ui_automation() {
    local scenario=$1
    local action_wait_ms=$2

    local settle_ms=$((SETTLE_TIME * 1000))
    local device_name_arg=""
    if [[ "$scenario" == "connected" ]]; then
        device_name_arg="-e deviceName '$DEVICE_NAME'"
    fi

    # Launch instrumentation in background; it will sleep after reaching state
    # Use eval to preserve quoting of device name with spaces inside adb shell
    adb shell "am instrument -w \
        -e scenario $scenario \
        -e actionWait $action_wait_ms \
        -e settleTime $settle_ms \
        $device_name_arg \
        -e class ${TEST_CLASS}#putInState \
        ${PKG}.test/${TEST_RUNNER}" > /tmp/memtest_instrument.log 2>&1 &
    local INST_PID=$!

    # Wait for the test to signal "ready" or fail
    local waited=0
    local max_wait=$(( (SETTLE_TIME + action_wait_ms / 1000 + 30) ))
    while ((waited < max_wait)); do
        if grep -q "status=ready" /tmp/memtest_instrument.log 2>/dev/null; then
            return 0
        fi
        if grep -q "FAILURES\!\!\!" /tmp/memtest_instrument.log 2>/dev/null; then
            echo ""
            echo "  ERROR: Instrumentation test failed:"
            cat /tmp/memtest_instrument.log
            kill "$INST_PID" 2>/dev/null || true
            return 1
        fi
        sleep 1
        ((waited++))
    done

    echo "  ERROR: Timed out waiting for instrumentation test (${max_wait}s)"
    kill "$INST_PID" 2>/dev/null || true
    return 1
}

# --- Run a scenario ---

run_scenario() {
    local scenario=$1
    local iterations=$2
    local settle=$3
    local action_wait=$4   # seconds, only for discovery/connected

    if ((iterations == 0)); then
        echo ""
        echo "  Skipping $scenario (0 iterations)."
        return
    fi

    echo ""
    echo "========================================"
    echo "  Scenario: $scenario ($iterations iterations)"
    echo "========================================"
    echo ""

    local PSS_VALUES=()
    local JAVA_VALUES=()
    local NATIVE_VALUES=()

    for i in $(seq 1 "$iterations"); do
        echo "--- Iteration $i/$iterations ---"

        # Force-stop any previous run
        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        if [[ "$scenario" == "idle" ]]; then
            # Simple cold launch + wait
            adb shell am start-activity -W -S "$PKG/$ACTIVITY" > /dev/null 2>&1
            countdown "$settle" "Stabilizing"
        else
            # Instrumentation restarts the app, handles settle time internally,
            # then clicks cast icon / selects device
            local action_wait_ms=$((action_wait * 1000))
            echo "  Running UI automation ($scenario, settle=${settle}s, action wait=${action_wait}s)..."
            if ! run_ui_automation "$scenario" "$action_wait_ms"; then
                echo "  SKIPPING iteration $i due to automation failure."
                echo ""
                continue
            fi
            echo "  State reached. Measuring..."
        fi

        # Capture meminfo
        MEMINFO=$(adb shell dumpsys meminfo "$PKG" 2>&1)
        read -r pss java native <<< "$(echo "$MEMINFO" | parse_meminfo)"

        if [[ -z "$pss" || -z "$java" || -z "$native" ]]; then
            echo "  SKIPPING iteration $i — failed to parse meminfo."
            echo ""
            continue
        fi

        local pss_mb=$(echo "scale=1; $pss / 1024" | bc)
        local java_mb=$(echo "scale=1; $java / 1024" | bc)
        local native_mb=$(echo "scale=1; $native / 1024" | bc)

        echo "  TOTAL PSS: ${pss_mb} MB | Java Heap: ${java_mb} MB | Native Heap: ${native_mb} MB"
        echo ""

        PSS_VALUES+=("$pss")
        JAVA_VALUES+=("$java")
        NATIVE_VALUES+=("$native")
    done

    # Force-stop after last iteration
    adb shell am force-stop "$PKG" 2>/dev/null || true

    local count=${#PSS_VALUES[@]}
    if ((count == 0)); then
        echo "  No successful measurements for $scenario."
        return
    fi

    echo ""
    echo "=== Results: $scenario ($count successful iterations) ==="
    compute_stats "TOTAL PSS" "${PSS_VALUES[@]}"
    echo ""
    compute_stats "Java Heap" "${JAVA_VALUES[@]}"
    echo ""
    compute_stats "Native Heap" "${NATIVE_VALUES[@]}"
}

# --- Run all scenarios ---

run_scenario "idle"      "$IDLE_ITERS" "$SETTLE_TIME" 0
run_scenario "discovery" "$DISC_ITERS" "$SETTLE_TIME" "$DISC_ACTION_WAIT"
run_scenario "connected" "$CONN_ITERS" "$SETTLE_TIME" "$CONN_ACTION_WAIT"

echo ""
echo "================================================"
echo "  All scenarios complete."
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
echo "================================================"
