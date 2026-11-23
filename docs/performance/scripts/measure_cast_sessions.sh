#!/usr/bin/env bash
#
# Measures memory footprint after the 1st and Nth cast session to detect
# memory growth and leaks.  Uses CastSessionsBenchmarkHelper#castSessions
# which launches the app, connects to a Chromecast, plays a video, stops,
# disconnects, and repeats — reporting "status=disconnected" after each
# session so this script can capture dumpsys meminfo at the right moments.
#
# Can run standalone (prompts user) or be called from measure_all.sh with
# all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_cast_sessions.sh
#   From unified: ./measure_cast_sessions.sh --skip-build \
#                   --device-name "TV Name" --sessions 10 --iters 3 \
#                   --settle-time 60 --action-wait 10 \
#                   --play-time 30 --after-time 45

set -euo pipefail

# --- Wrapper to prevent adb from consuming script stdin ---
adb() { command adb "$@" < /dev/null; }

# --- CLI arg parsing ---

SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build)    SKIP_BUILD=true; shift ;;
        --device-name)   DEVICE_NAME="$2"; shift 2 ;;
        --sessions)      SESSIONS="$2"; shift 2 ;;
        --iters)         ITERS="$2"; shift 2 ;;
        --settle-time)   SETTLE_TIME="$2"; shift 2 ;;
        --action-wait)   ACTION_WAIT="$2"; shift 2 ;;
        --play-time)     PLAY_TIME="$2"; shift 2 ;;
        --after-time)    AFTER_TIME="$2"; shift 2 ;;
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
TEST_CLASS="tv.vizbee.demo.CastSessionsBenchmarkHelper"
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
echo "  Cast Sessions Memory Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Collect configuration ---

prompt_param DEVICE_NAME "Chromecast device name" "CC with Google TV TV 2"
prompt_param SESSIONS    "Number of cast sessions per iteration" 10
prompt_param ITERS       "Number of iterations (full runs)" 3
prompt_param SETTLE_TIME "Settling time after launch, seconds" 60
prompt_param ACTION_WAIT "Wait time after casting starts, seconds" 10
prompt_param PLAY_TIME   "Video play time per session, seconds" 30
prompt_param AFTER_TIME  "Wait time after disconnect, seconds" 45
echo ""

# --- Estimate ---

est_per_session=$((SETTLE_TIME + ACTION_WAIT + PLAY_TIME + AFTER_TIME + 20))
est_per_iter=$((SETTLE_TIME + SESSIONS * est_per_session))
est_total=$((ITERS * est_per_iter))
est_min=$((est_total / 60))

echo "--- Configuration ---"
echo "  Device name:     $DEVICE_NAME"
echo "  Sessions:        $SESSIONS per iteration"
echo "  Iterations:      $ITERS"
echo "  Settle time:     ${SETTLE_TIME}s"
echo "  Action wait:     ${ACTION_WAIT}s"
echo "  Play time:       ${PLAY_TIME}s"
echo "  After time:      ${AFTER_TIME}s"
echo "  Estimated time:  ~${est_min} min"
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

# --- Run cast sessions and capture memory ---

PSS_AFTER_1=()
JAVA_AFTER_1=()
NATIVE_AFTER_1=()
PSS_AFTER_N=()
JAVA_AFTER_N=()
NATIVE_AFTER_N=()

INSTRUMENT_LOG="/tmp/cast_sessions_instrument.log"

for iter in $(seq 1 "$ITERS"); do
    echo ""
    echo "========================================"
    echo "  Iteration $iter/$ITERS ($SESSIONS sessions)"
    echo "========================================"
    echo ""

    # Force-stop any previous run
    adb shell am force-stop "$PKG" 2>/dev/null || true
    sleep 1

    # Clear the log
    > "$INSTRUMENT_LOG"

    # Launch the instrumentation test
    local_settle_ms=$((SETTLE_TIME * 1000))
    local_action_ms=$((ACTION_WAIT * 1000))
    local_play_ms=$((PLAY_TIME * 1000))
    local_after_ms=$((AFTER_TIME * 1000))

    adb shell "am instrument -w \
        -e sessions $SESSIONS \
        -e deviceName '$DEVICE_NAME' \
        -e settleTime $local_settle_ms \
        -e actionWait $local_action_ms \
        -e playTime $local_play_ms \
        -e afterTime $local_after_ms \
        -e class ${TEST_CLASS}#castSessions \
        ${PKG}.test/${TEST_RUNNER}" > "$INSTRUMENT_LOG" 2>&1 &
    INSTRUMENT_PID=$!

    # Monitor for session completions and capture meminfo
    local sessions_seen=0
    local max_wait=$(( SETTLE_TIME + SESSIONS * (ACTION_WAIT + PLAY_TIME + AFTER_TIME + 120) + 120 ))
    local waited=0

    while ((waited < max_wait)); do
        # Check for failure
        if grep -q "FAILURES\!\!\!" "$INSTRUMENT_LOG" 2>/dev/null; then
            echo "  ERROR: Instrumentation test failed:"
            grep -A 5 "FAILURES" "$INSTRUMENT_LOG"
            kill "$INSTRUMENT_PID" 2>/dev/null || true
            break
        fi

        # Count completed sessions
        local current_sessions
        current_sessions=$(grep -c "status=disconnected" "$INSTRUMENT_LOG" 2>/dev/null || echo 0)

        # New session completed
        while ((sessions_seen < current_sessions)); do
            sessions_seen=$((sessions_seen + 1))
            echo "  Session $sessions_seen/$SESSIONS completed."

            # Capture meminfo after session 1 and last session
            if ((sessions_seen == 1)) || ((sessions_seen == SESSIONS)); then
                MEMINFO=$(adb shell dumpsys meminfo "$PKG" 2>&1)
                read -r pss java native <<< "$(echo "$MEMINFO" | parse_meminfo)"

                if [[ -z "$pss" || -z "$java" || -z "$native" ]]; then
                    echo "    WARNING: Failed to parse meminfo after session $sessions_seen."
                else
                    local pss_mb=$(echo "scale=1; $pss / 1024" | bc)
                    local java_mb=$(echo "scale=1; $java / 1024" | bc)
                    local native_mb=$(echo "scale=1; $native / 1024" | bc)
                    echo "    TOTAL PSS: ${pss_mb} MB | Java Heap: ${java_mb} MB | Native Heap: ${native_mb} MB"

                    if ((sessions_seen == 1)); then
                        PSS_AFTER_1+=("$pss")
                        JAVA_AFTER_1+=("$java")
                        NATIVE_AFTER_1+=("$native")
                    else
                        PSS_AFTER_N+=("$pss")
                        JAVA_AFTER_N+=("$java")
                        NATIVE_AFTER_N+=("$native")
                    fi
                fi
            fi
        done

        # Check if test finished
        if ! kill -0 "$INSTRUMENT_PID" 2>/dev/null; then
            break
        fi

        sleep 2
        ((waited += 2))
    done

    # Wait for process to finish
    wait "$INSTRUMENT_PID" 2>/dev/null || true
    adb shell am force-stop "$PKG" 2>/dev/null || true

    if ((sessions_seen < SESSIONS)); then
        echo "  WARNING: Only $sessions_seen/$SESSIONS sessions completed in iteration $iter."
    fi
    echo ""
done

# --- Results ---

echo ""
echo "================================================"
echo "  Cast Sessions Memory Results"
echo "================================================"

if [[ ${#PSS_AFTER_1[@]} -gt 0 ]]; then
    echo ""
    echo "=== After 1st session (${#PSS_AFTER_1[@]} iterations) ==="
    compute_stats "TOTAL PSS" "${PSS_AFTER_1[@]}"
    echo ""
    compute_stats "Java Heap" "${JAVA_AFTER_1[@]}"
    echo ""
    compute_stats "Native Heap" "${NATIVE_AFTER_1[@]}"
else
    echo "  No data for 1st session."
fi

if [[ ${#PSS_AFTER_N[@]} -gt 0 ]]; then
    echo ""
    echo "=== After ${SESSIONS}th session (${#PSS_AFTER_N[@]} iterations) ==="
    compute_stats "TOTAL PSS" "${PSS_AFTER_N[@]}"
    echo ""
    compute_stats "Java Heap" "${JAVA_AFTER_N[@]}"
    echo ""
    compute_stats "Native Heap" "${NATIVE_AFTER_N[@]}"
else
    echo "  No data for ${SESSIONS}th session."
fi

# Memory growth
if [[ ${#PSS_AFTER_1[@]} -gt 0 && ${#PSS_AFTER_N[@]} -gt 0 ]]; then
    echo ""
    # Use medians for growth calculation
    sorted_1=($(printf '%s\n' "${PSS_AFTER_1[@]}" | sort -n))
    sorted_n=($(printf '%s\n' "${PSS_AFTER_N[@]}" | sort -n))
    mid_1=$(( ${#sorted_1[@]} / 2 ))
    mid_n=$(( ${#sorted_n[@]} / 2 ))
    p50_1=${sorted_1[$mid_1]}
    p50_n=${sorted_n[$mid_n]}
    growth_kb=$((p50_n - p50_1))
    growth_mb=$(echo "scale=1; $growth_kb / 1024" | bc)
    per_session_kb=$(echo "scale=1; $growth_kb / ($SESSIONS - 1)" | bc)
    per_session_mb=$(echo "scale=2; $growth_kb / 1024 / ($SESSIONS - 1)" | bc)
    echo "  Memory growth (P50): ${growth_mb} MB over $((SESSIONS - 1)) sessions (${per_session_mb} MB/session)"
fi

echo ""
echo "================================================"
echo "  All cast sessions complete."
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
echo "================================================"
