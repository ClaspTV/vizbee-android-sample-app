#!/usr/bin/env bash
#
# Measures memory footprint (TOTAL PSS, Java Heap, Native Heap) of the app
# with Google Cast SDK across idle, discovery, and connected scenarios.
#
# Can run standalone (prompts user) or be called from measure_all_chromecast.sh
# with all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_memory_chromecast.sh
#   From unified: ./measure_memory_chromecast.sh --skip-build \
#                   --device-name "TV Name" --idle-iters 30 ...

set -euo pipefail

# --- Wrapper to prevent adb from consuming script stdin ---
adb() { command adb "$@" < /dev/null; }

# --- CLI arg parsing ---

SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build)        SKIP_BUILD=true; shift ;;
        --device-name)       DEVICE_NAME="$2"; shift 2 ;;
        --idle-iters)        IDLE_ITERS="$2"; shift 2 ;;
        --disc-iters)        DISC_ITERS="$2"; shift 2 ;;
        --conn-iters)        CONN_ITERS="$2"; shift 2 ;;
        --settle-time)       SETTLE_TIME="$2"; shift 2 ;;
        --disc-action-wait)  DISC_ACTION_WAIT="$2"; shift 2 ;;
        --conn-action-wait)  CONN_ACTION_WAIT="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# --- Prompt for any parameter not already set via CLI ---

prompt_param() {
    local var_name=$1
    local prompt_text=$2
    local default_value=$3

    if [[ -n "${!var_name:-}" ]]; then
        return
    fi

    local input
    read -rp "$prompt_text [$default_value]: " input
    eval "$var_name=\"${input:-$default_value}\""
}

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
TEST_CLASS="tv.vizbee.demo.ChromecastMemoryBenchmarkHelper"
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
echo "  Chromecast Memory Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Collect configuration (prompt only for values not set via CLI) ---

prompt_param DEVICE_NAME "Chromecast device name (as shown in Google Cast dialog)" "CC with Google TV TV 2"
echo ""

prompt_param IDLE_ITERS  "Iterations for IDLE scenario"      30
prompt_param DISC_ITERS  "Iterations for DISCOVERY scenario"  10
prompt_param CONN_ITERS  "Iterations for CONNECTED scenario"  10
echo ""

prompt_param SETTLE_TIME      "Initial settling time after launch, seconds"            60
prompt_param DISC_ACTION_WAIT "Wait time after cast icon click (discovery), seconds"   10
prompt_param CONN_ACTION_WAIT "Wait time after casting starts (connected), seconds"    15
echo ""

# --- Estimate total time ---

est_idle=$((IDLE_ITERS * (SETTLE_TIME + 2)))
est_disc=$((DISC_ITERS * (SETTLE_TIME + DISC_ACTION_WAIT + 12)))
est_conn=$((CONN_ITERS * (SETTLE_TIME + CONN_ACTION_WAIT + 12)))
est_total=$((est_idle + est_disc + est_conn))
est_min=$((est_total / 60))

echo "--- Configuration ---"
echo "  Device name:       $DEVICE_NAME"
echo "  Idle iterations:   $IDLE_ITERS"
echo "  Disc iterations:   $DISC_ITERS"
echo "  Conn iterations:   $CONN_ITERS"
echo "  Settle time:       ${SETTLE_TIME}s"
echo "  Discovery wait:    ${DISC_ACTION_WAIT}s"
echo "  Cast settle:       ${CONN_ACTION_WAIT}s"
echo "  Estimated time:    ~${est_min} min"
echo "---------------------"
echo ""

# --- Build & Install ---

if [[ "$SKIP_BUILD" == true ]]; then
    echo "Skipping build & install (--skip-build)."
    echo ""
else
    read -rp "Press ENTER to start (builds & installs the app)..."
    echo ""
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
fi

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

run_ui_automation() {
    local scenario=$1
    local action_wait_ms=$2
    local measure_duration_ms=$3

    local settle_ms=$((SETTLE_TIME * 1000))
    local device_name_arg=""
    if [[ "$scenario" == "connected" ]]; then
        device_name_arg="-e deviceName '$DEVICE_NAME'"
    fi

    adb shell "am instrument -w \
        -e scenario $scenario \
        -e actionWait $action_wait_ms \
        -e settleTime $settle_ms \
        -e measureDuration $measure_duration_ms \
        $device_name_arg \
        -e class ${TEST_CLASS}#putInState \
        ${PKG}.test/${TEST_RUNNER}" > /tmp/memtest_cc_instrument.log 2>&1 &
    UI_AUTOMATION_PID=$!

    local waited=0
    local max_wait=$(( (SETTLE_TIME + action_wait_ms / 1000 + 90) ))
    while ((waited < max_wait)); do
        if grep -q "status=ready" /tmp/memtest_cc_instrument.log 2>/dev/null; then
            return 0
        fi
        if grep -q "FAILURES\!\!\!" /tmp/memtest_cc_instrument.log 2>/dev/null; then
            echo ""
            echo "  ERROR: Instrumentation test failed:"
            cat /tmp/memtest_cc_instrument.log
            kill "$UI_AUTOMATION_PID" 2>/dev/null || true
            return 1
        fi
        sleep 1
        ((waited++))
    done

    echo "  ERROR: Timed out waiting for instrumentation test (${max_wait}s)"
    kill "$UI_AUTOMATION_PID" 2>/dev/null || true
    return 1
}

# --- Run a scenario ---

run_scenario() {
    local scenario=$1
    local iterations=$2
    local settle=$3
    local action_wait=$4

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

        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        if [[ "$scenario" == "idle" ]]; then
            adb shell am start-activity -W -S "$PKG/$ACTIVITY" > /dev/null 2>&1
            countdown "$settle" "Stabilizing"
        else
            local action_wait_ms=$((action_wait * 1000))
            local measure_duration_ms=10000
            echo "  Running UI automation ($scenario, settle=${settle}s, action wait=${action_wait}s)..."
            if ! run_ui_automation "$scenario" "$action_wait_ms" "$measure_duration_ms"; then
                echo "  SKIPPING iteration $i due to automation failure."
                echo ""
                continue
            fi
            echo "  State reached. Measuring..."
        fi

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

        if [[ "$scenario" != "idle" ]]; then
            wait "$UI_AUTOMATION_PID" 2>/dev/null || true
        fi
    done

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
