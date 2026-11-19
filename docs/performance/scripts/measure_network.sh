#!/usr/bin/env bash
#
# Measures network traffic (KB/min) of the app across idle, discovery, and
# connected scenarios using TrafficStats via a UIAutomator instrumentation test.
#
# Usage: ./measure_network.sh
#
# Prerequisites:
#   - Android device connected via USB with USB debugging enabled
#   - Java 17 for Gradle build

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
TEST_CLASS="tv.vizbee.demo.NetworkBenchmarkHelper"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "================================================"
echo "  Network Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Configuration prompts ---

read -rp "Chromecast device name (as shown in Vizbee device list): " DEVICE_NAME
echo ""

read -rp "Iterations for IDLE scenario [3]: " IDLE_ITERS
IDLE_ITERS="${IDLE_ITERS:-3}"
read -rp "Iterations for DISCOVERY scenario [3]: " DISC_ITERS
DISC_ITERS="${DISC_ITERS:-3}"
read -rp "Iterations for CONNECTED scenario [3]: " CONN_ITERS
CONN_ITERS="${CONN_ITERS:-3}"
echo ""

read -rp "Initial settling time after launch, seconds [60]: " SETTLE_TIME
SETTLE_TIME="${SETTLE_TIME:-60}"
read -rp "Network measurement duration, seconds [180]: " MEASURE_DURATION
MEASURE_DURATION="${MEASURE_DURATION:-180}"
read -rp "Wait time after cast icon click (discovery), seconds [10]: " DISC_ACTION_WAIT
DISC_ACTION_WAIT="${DISC_ACTION_WAIT:-10}"
read -rp "Wait time after casting starts (connected), seconds [15]: " CONN_ACTION_WAIT
CONN_ACTION_WAIT="${CONN_ACTION_WAIT:-15}"
echo ""

echo "--- Configuration ---"
echo "  Device name:         $DEVICE_NAME"
echo "  Idle iterations:     $IDLE_ITERS"
echo "  Disc iterations:     $DISC_ITERS"
echo "  Conn iterations:     $CONN_ITERS"
echo "  Settle time:         ${SETTLE_TIME}s"
echo "  Measure duration:    ${MEASURE_DURATION}s"
echo "  Discovery wait:      ${DISC_ACTION_WAIT}s"
echo "  Cast settle:        ${CONN_ACTION_WAIT}s"
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

# Parse instrumentation output for network stats.
# Looks for status bundle keys: rxBytes, txBytes, totalBytes, durationMs
# Returns: "rxBytes txBytes totalBytes durationMs" or empty on failure.
parse_network_result() {
    local log_file=$1
    local rx="" tx="" total="" duration="" done_found=""

    while IFS= read -r line; do
        case "$line" in
            *status=done*) done_found=1 ;;
            *rxBytes=*) rx=$(echo "$line" | sed 's/.*rxBytes=//' | tr -d '[:space:]') ;;
            *txBytes=*) tx=$(echo "$line" | sed 's/.*txBytes=//' | tr -d '[:space:]') ;;
            *totalBytes=*) total=$(echo "$line" | sed 's/.*totalBytes=//' | tr -d '[:space:]') ;;
            *durationMs=*) duration=$(echo "$line" | sed 's/.*durationMs=//' | tr -d '[:space:]') ;;
        esac
    done < "$log_file"

    if [[ -n "$done_found" && -n "$total" && -n "$duration" && "$total" != "-1" ]]; then
        echo "$rx $tx $total $duration"
    fi
}

compute_net_stats() {
    local label=$1
    shift
    local values=("$@")
    local count=${#values[@]}

    local sorted=($(printf '%s\n' "${values[@]}" | sort -g))

    local sum
    sum=$(printf '%s\n' "${values[@]}" | awk '{s+=$1} END {print s}')
    local avg
    avg=$(echo "scale=2; $sum / $count" | bc)

    local mid=$((count / 2))
    local p50
    if ((count % 2 == 0)); then
        p50=$(echo "scale=2; (${sorted[mid - 1]} + ${sorted[mid]}) / 2" | bc)
    else
        p50=${sorted[$mid]}
    fi

    local p90_idx
    p90_idx=$(echo "$count * 9 / 10" | bc)
    if ((p90_idx >= count)); then p90_idx=$((count - 1)); fi
    local p90=${sorted[$p90_idx]}

    echo "  $label:"
    echo "    Raw (KB/min): ${values[*]}"
    echo "    Avg:  ${avg} KB/min"
    echo "    P50:  ${p50} KB/min"
    echo "    P90:  ${p90} KB/min"
}

# --- Run a scenario ---

run_scenario() {
    local scenario=$1
    local iterations=$2
    local action_wait=$3   # seconds, only for discovery/connected

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

    local TOTAL_KBMIN_VALUES=()
    local RX_KBMIN_VALUES=()
    local TX_KBMIN_VALUES=()

    local settle_ms=$((SETTLE_TIME * 1000))
    local measure_ms=$((MEASURE_DURATION * 1000))
    local action_wait_ms=$((action_wait * 1000))

    # Total time per iteration: settle + action_wait + measure + buffer
    local total_wait=$((SETTLE_TIME + action_wait + MEASURE_DURATION + 90))

    for i in $(seq 1 "$iterations"); do
        echo "--- Iteration $i/$iterations ---"

        # Force-stop any previous run
        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        local log_file="/tmp/nettest_instrument_${scenario}_${i}.log"

        # Build instrumentation command
        local inst_args="-e scenario $scenario -e settleTime $settle_ms -e measureDuration $measure_ms -e actionWait $action_wait_ms"
        if [[ "$scenario" == "connected" ]]; then
            inst_args="$inst_args -e deviceName '$DEVICE_NAME'"
        fi

        echo "  Running network measurement ($scenario, settle=${SETTLE_TIME}s, measure=${MEASURE_DURATION}s)..."

        # Run instrumentation in background
        adb shell "am instrument -w \
            $inst_args \
            -e class ${TEST_CLASS}#measureNetwork \
            ${PKG}.test/${TEST_RUNNER}" > "$log_file" 2>&1 &
        local INST_PID=$!

        # Wait for "status=ready" (state reached), then show countdown for measurement
        local waited=0
        local ready_wait=$((SETTLE_TIME + action_wait + 90))
        local ready_found=0
        while ((waited < ready_wait)); do
            if grep -q "status=ready" "$log_file" 2>/dev/null; then
                ready_found=1
                break
            fi
            if grep -q "FAILURES\!\!\!" "$log_file" 2>/dev/null; then
                echo ""
                echo "  ERROR: Instrumentation test failed:"
                cat "$log_file"
                kill "$INST_PID" 2>/dev/null || true
                ready_found=-1
                break
            fi
            sleep 1
            ((waited++))
        done

        if ((ready_found == -1)); then
            echo "  SKIPPING iteration $i due to automation failure."
            echo ""
            continue
        elif ((ready_found == 0)); then
            echo "  ERROR: Timed out waiting for state to be reached (${ready_wait}s)"
            kill "$INST_PID" 2>/dev/null || true
            echo "  SKIPPING iteration $i."
            echo ""
            continue
        fi

        echo "  State reached. Measuring network traffic (${MEASURE_DURATION}s)..."
        countdown "$MEASURE_DURATION" "Measuring"

        # Wait a bit more for the test to finish writing results
        local done_wait=0
        while ((done_wait < 15)); do
            if grep -q "status=done" "$log_file" 2>/dev/null; then
                break
            fi
            sleep 1
            ((done_wait++))
        done

        # Parse results
        local result
        result=$(parse_network_result "$log_file")

        if [[ -z "$result" ]]; then
            echo "  SKIPPING iteration $i — failed to get network stats (TrafficStats unsupported?)."
            cat "$log_file"
            echo ""
            kill "$INST_PID" 2>/dev/null || true
            continue
        fi

        local rx_bytes tx_bytes total_bytes duration_ms
        read -r rx_bytes tx_bytes total_bytes duration_ms <<< "$result"

        local duration_min
        duration_min=$(echo "scale=4; $duration_ms / 60000" | bc)
        local rx_kbmin tx_kbmin total_kbmin
        rx_kbmin=$(echo "scale=2; ($rx_bytes / 1024) / $duration_min" | bc)
        tx_kbmin=$(echo "scale=2; ($tx_bytes / 1024) / $duration_min" | bc)
        total_kbmin=$(echo "scale=2; ($total_bytes / 1024) / $duration_min" | bc)

        echo "  RX: ${rx_kbmin} KB/min | TX: ${tx_kbmin} KB/min | Total: ${total_kbmin} KB/min"
        echo "  (raw: rx=${rx_bytes}B tx=${tx_bytes}B total=${total_bytes}B over ${duration_ms}ms)"
        echo ""

        TOTAL_KBMIN_VALUES+=("$total_kbmin")
        RX_KBMIN_VALUES+=("$rx_kbmin")
        TX_KBMIN_VALUES+=("$tx_kbmin")

        # Test cleans up cast session internally before exiting
        wait "$INST_PID" 2>/dev/null || true
        adb shell am force-stop "$PKG" 2>/dev/null || true
    done

    local count=${#TOTAL_KBMIN_VALUES[@]}
    if ((count == 0)); then
        echo "  No successful measurements for $scenario."
        return
    fi

    echo ""
    echo "=== Results: $scenario ($count successful iterations) ==="
    compute_net_stats "Total traffic" "${TOTAL_KBMIN_VALUES[@]}"
    echo ""
    compute_net_stats "RX traffic" "${RX_KBMIN_VALUES[@]}"
    echo ""
    compute_net_stats "TX traffic" "${TX_KBMIN_VALUES[@]}"
}

# --- Run all scenarios ---

run_scenario "idle"      "$IDLE_ITERS" 0
run_scenario "discovery" "$DISC_ITERS" "$DISC_ACTION_WAIT"
run_scenario "connected" "$CONN_ITERS" "$CONN_ACTION_WAIT"

echo ""
echo "================================================"
echo "  All scenarios complete."
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
echo "================================================"
