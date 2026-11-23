#!/usr/bin/env bash
#
# Measures memory footprint after each cast session disconnect to detect
# memory growth and leaks.  Uses CastSessionsBenchmarkHelper#castSessions
# which launches the app, connects to a Chromecast, plays a video, stops,
# disconnects, and repeats — reporting "status=disconnected" after each
# session so this script can capture dumpsys meminfo at the right moments.
#
# Matches the iOS benchmark methodology:
#   10 sessions in a row (connect, 30s playback, disconnect, 30s wait).
#   Memory captured after each disconnect, slope computed over all sessions.
#   Single run (not iterated).
#
# Can run standalone (prompts user) or be called from measure_all.sh with
# all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_cast_sessions.sh
#   From unified: ./measure_cast_sessions.sh --skip-build \
#                   --device-name "TV Name" --sessions 10 \
#                   --settle-time 60 --action-wait 10 \
#                   --play-time 30 --after-time 30

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
prompt_param SESSIONS    "Number of cast sessions" 10
prompt_param SETTLE_TIME "Settling time after launch, seconds" 60
prompt_param ACTION_WAIT "Wait time after casting starts, seconds" 10
prompt_param PLAY_TIME   "Video play time per session, seconds" 30
prompt_param AFTER_TIME  "Wait time after disconnect, seconds" 30
echo ""

# --- Estimate ---

est_per_session=$((ACTION_WAIT + PLAY_TIME + AFTER_TIME + 20))
est_total=$((SETTLE_TIME + SESSIONS * est_per_session))
est_min=$((est_total / 60))

echo "--- Configuration ---"
echo "  Device name:     $DEVICE_NAME"
echo "  Sessions:        $SESSIONS"
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

# --- Run cast sessions and capture memory after each disconnect ---

PSS_VALUES=()
JAVA_VALUES=()
NATIVE_VALUES=()

INSTRUMENT_LOG="/tmp/cast_sessions_instrument.log"

echo ""
echo "========================================"
echo "  Running $SESSIONS cast sessions"
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
sessions_seen=0
max_wait=$(( SETTLE_TIME + SESSIONS * (ACTION_WAIT + PLAY_TIME + AFTER_TIME + 120) + 120 ))
waited=0

while ((waited < max_wait)); do
    # Check for failure
    if grep -q "FAILURES\!\!\!" "$INSTRUMENT_LOG" 2>/dev/null; then
        echo "  ERROR: Instrumentation test failed:"
        grep -A 5 "FAILURES" "$INSTRUMENT_LOG"
        kill "$INSTRUMENT_PID" 2>/dev/null || true
        break
    fi

    # Count completed sessions
    current_sessions=$(grep -c "status=disconnected" "$INSTRUMENT_LOG" 2>/dev/null || echo 0)

    # New session completed — capture meminfo
    while ((sessions_seen < current_sessions)); do
        sessions_seen=$((sessions_seen + 1))

        MEMINFO=$(adb shell dumpsys meminfo "$PKG" 2>&1)
        read -r pss java native <<< "$(echo "$MEMINFO" | parse_meminfo)"

        if [[ -z "$pss" || -z "$java" || -z "$native" ]]; then
            echo "  Session $sessions_seen/$SESSIONS: WARNING — failed to parse meminfo"
        else
            pss_mb=$(echo "scale=1; $pss / 1024" | bc)
            java_mb=$(echo "scale=1; $java / 1024" | bc)
            native_mb=$(echo "scale=1; $native / 1024" | bc)
            echo "  Session $sessions_seen/$SESSIONS: PSS ${pss_mb} MB | Java ${java_mb} MB | Native ${native_mb} MB"

            PSS_VALUES+=("$pss")
            JAVA_VALUES+=("$java")
            NATIVE_VALUES+=("$native")
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
    echo "  WARNING: Only $sessions_seen/$SESSIONS sessions completed."
fi

# --- Results ---

echo ""
echo "================================================"
echo "  Cast Sessions Memory Results"
echo "================================================"

count=${#PSS_VALUES[@]}
if ((count == 0)); then
    echo "  No successful measurements."
else
    # Print all values in MB
    echo ""
    echo "  Session-by-session TOTAL PSS (MB):"
    for i in $(seq 0 $((count - 1))); do
        mb=$(echo "scale=1; ${PSS_VALUES[$i]} / 1024" | bc)
        echo "    Session $((i + 1)): ${mb} MB"
    done

    # First and last session
    first_pss=${PSS_VALUES[0]}
    last_pss=${PSS_VALUES[$((count - 1))]}
    first_mb=$(echo "scale=1; $first_pss / 1024" | bc)
    last_mb=$(echo "scale=1; $last_pss / 1024" | bc)
    echo ""
    echo "  After 1st session:    ${first_mb} MB"
    echo "  After ${count}th session:  ${last_mb} MB"

    # Growth
    if ((count > 1)); then
        growth_kb=$((last_pss - first_pss))
        growth_mb=$(echo "scale=1; $growth_kb / 1024" | bc)
        per_session_mb=$(echo "scale=2; $growth_kb / 1024 / ($count - 1)" | bc)
        echo "  Total growth:         ${growth_mb} MB"
        echo "  Growth per session:   ${per_session_mb} MB/session"

        # Growth from session 3 onward (after caches fill, per iOS methodology)
        if ((count >= 3)); then
            s3_pss=${PSS_VALUES[2]}
            growth_from_3=$((last_pss - s3_pss))
            growth_from_3_mb=$(echo "scale=1; $growth_from_3 / 1024" | bc)
            sessions_from_3=$((count - 3))
            if ((sessions_from_3 > 0)); then
                per_session_from_3=$(echo "scale=2; $growth_from_3 / 1024 / $sessions_from_3" | bc)
                echo "  Growth from session 3: ${growth_from_3_mb} MB (${per_session_from_3} MB/session)"
            fi
        fi
    fi
fi

echo ""
echo "================================================"
echo "  All cast sessions complete."
echo "  Device: $DEVICE (Android $ANDROID_VER)"
echo "  Branch: $(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
echo "================================================"
