#!/usr/bin/env bash
#
# Measures per-app battery drain (mAh/hr) across idle, discovery, and connected
# scenarios using dumpsys batterystats.
#
# Can run standalone (prompts user) or be called from measure_all.sh with
# all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_battery.sh
#   From unified: ./measure_battery.sh --skip-build --device-name "TV Name" \
#                   --idle-iters 3 --disc-iters 3 --conn-iters 3 \
#                   --settle-time 60 --measure-duration 600 \
#                   --disc-action-wait 10 --conn-action-wait 15
#
# Prerequisites:
#   - Android device connected via USB with USB debugging enabled
#   - Java 17 for Gradle build

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
        --measure-duration)  MEASURE_DURATION="$2"; shift 2 ;;
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

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

PKG="tv.vizbee.demo"
ACTIVITY=".activity.MainActivity"
TEST_CLASS="tv.vizbee.demo.MemoryBenchmarkHelper"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
TMPDIR_LOCAL=$(mktemp -d "${TMPDIR:-/tmp}/battery_bench.XXXXXX")

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "================================================"
echo "  Battery Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Collect configuration (prompt only for values not set via CLI) ---

prompt_param DEVICE_NAME      "Chromecast device name (as shown in Vizbee device list)" "CC with Google TV TV 2"
echo ""

prompt_param IDLE_ITERS       "Iterations for IDLE scenario"      3
prompt_param DISC_ITERS       "Iterations for DISCOVERY scenario"  3
prompt_param CONN_ITERS       "Iterations for CONNECTED scenario"  3
echo ""

prompt_param SETTLE_TIME      "Initial settling time after launch, seconds"            60
prompt_param MEASURE_DURATION "Measurement duration, seconds"                          600
prompt_param DISC_ACTION_WAIT "Wait time after cast icon click (discovery), seconds"   10
prompt_param CONN_ACTION_WAIT "Wait time after casting starts (connected), seconds"    15
echo ""

# --- Estimate total time ---

est_idle=$((IDLE_ITERS * (SETTLE_TIME + MEASURE_DURATION + 4)))
est_disc=$((DISC_ITERS * (SETTLE_TIME + DISC_ACTION_WAIT + MEASURE_DURATION + 14)))
est_conn=$((CONN_ITERS * (SETTLE_TIME + CONN_ACTION_WAIT + MEASURE_DURATION + 14)))
est_total=$((est_idle + est_disc + est_conn))
est_min=$((est_total / 60))

echo "--- Configuration ---"
echo "  Device name:         $DEVICE_NAME"
echo "  Idle iterations:     $IDLE_ITERS"
echo "  Disc iterations:     $DISC_ITERS"
echo "  Conn iterations:     $CONN_ITERS"
echo "  Settle time:         ${SETTLE_TIME}s"
echo "  Measure duration:    ${MEASURE_DURATION}s ($(echo "scale=1; $MEASURE_DURATION / 60" | bc) min)"
echo "  Discovery wait:      ${DISC_ACTION_WAIT}s"
echo "  Cast settle:         ${CONN_ACTION_WAIT}s"
echo "  Estimated time:      ~${est_min} min"
echo "  Results dir:         $TMPDIR_LOCAL"
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

# --- Resolve UID after install (UID changes on reinstall) ---

APP_UID_LINE=$(adb shell cmd package list packages -U 2>/dev/null | grep "package:${PKG} " || true)
APP_UID=$(echo "$APP_UID_LINE" | sed 's/.*uid://' | tr -d '[:space:]')

if [[ -z "$APP_UID" ]]; then
    echo "ERROR: Could not resolve UID for $PKG."
    exit 1
fi

# Convert numeric UID (e.g. 10234) to batterystats format (u0a234)
APP_ID=$((APP_UID - 10000))
UID_STR="u0a${APP_ID}"
echo "App UID: $APP_UID ($UID_STR)"
echo ""

# --- Helpers ---

countdown() {
    local secs=$1
    local label="${2:-Waiting}"
    local c
    for ((c = secs; c > 0; c--)); do
        printf "\r  %s: %3ds remaining..." "$label" "$c"
        sleep 1
    done
    printf "\r  %s: done.              \n" "$label"
}

# Runs the instrumentation test to put the app in discovery/connected state.
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
        ${PKG}.test/${TEST_RUNNER}" > /tmp/batterytest_instrument.log 2>&1 &
    UI_AUTOMATION_PID=$!

    local waited=0
    local max_wait=$(( (SETTLE_TIME + action_wait_ms / 1000 + 90) ))
    while ((waited < max_wait)); do
        if grep -q "status=ready" /tmp/batterytest_instrument.log 2>/dev/null; then
            return 0
        fi
        if grep -q "FAILURES\!\!\!" /tmp/batterytest_instrument.log 2>/dev/null; then
            echo ""
            echo "  ERROR: Instrumentation test failed:"
            cat /tmp/batterytest_instrument.log
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

# Parse per-app drain from dumpsys batterystats output.
parse_app_drain() {
    local stats_file=$1
    local uid=$2

    local in_section=0
    local found_uid=0
    local mah=""
    local breakdown=""
    while IFS= read -r line; do
        if [[ "$line" =~ "Estimated power use (mAh):" ]]; then
            in_section=1
            continue
        fi
        if ((in_section == 0)); then
            continue
        fi
        # Section ends at a non-indented line (next top-level section)
        if [[ "$line" =~ ^[^[:space:]] ]]; then
            break
        fi
        # Match our app's UID line (uppercase "UID")
        if [[ "$line" =~ UID\ ${uid}:\ ([0-9.]+) ]]; then
            mah="${BASH_REMATCH[1]}"
            found_uid=1
            continue
        fi
        # The line immediately after the UID line has component breakdown
        if ((found_uid)); then
            breakdown=$(echo "$line" | sed 's/^[[:space:]]*//' | sed 's/ *(.*//')
            break
        fi
    done < "$stats_file"

    if ((found_uid)); then
        echo "$mah|$breakdown"
        return 0
    fi

    echo ""
    return 1
}

# Parse whole-device computed drain from batterystats
parse_computed_drain() {
    local stats_file=$1
    grep "Computed drain:" "$stats_file" | head -1 | sed 's/.*Computed drain: //' | awk -F'[, ]' '{print $1}'
}

# Extract wake lock summary for the app
extract_wake_locks() {
    local stats_file=$1
    local uid=$2

    local in_uid=0
    local found_wl=0
    while IFS= read -r line; do
        if [[ "$line" =~ "UID ${uid}:" ]] || [[ "$line" =~ "Uid ${uid}:" ]]; then
            in_uid=1
            continue
        fi
        if ((in_uid)) && [[ "$line" =~ ^[[:space:]]+(UID|Uid)\ (u[0-9]|[0-9]) ]]; then
            break
        fi
        if ((in_uid)) && [[ "$line" =~ "Wake lock" ]]; then
            echo "    $line"
            found_wl=1
        fi
        if ((in_uid)) && [[ "$line" =~ "Wifi Multicast" ]]; then
            echo "    $line"
        fi
    done < "$stats_file"

    if ((found_wl == 0)); then
        echo "    (none)"
    fi
}

compute_battery_stats() {
    local label=$1
    shift
    local values=("$@")
    local count=${#values[@]}

    local sorted=($(printf '%s\n' "${values[@]}" | sort -g))

    local sum
    sum=$(printf '%s\n' "${values[@]}" | awk '{s+=$1} END {print s}')
    local avg
    avg=$(printf "%.4f" "$(echo "scale=6; $sum / $count" | bc)")

    local mid=$((count / 2))
    local p50
    if ((count % 2 == 0)); then
        p50=$(printf "%.4f" "$(echo "scale=6; (${sorted[mid - 1]} + ${sorted[mid]}) / 2" | bc)")
    else
        p50=${sorted[$mid]}
    fi

    local p90_idx
    p90_idx=$(echo "$count * 9 / 10" | bc)
    if ((p90_idx >= count)); then p90_idx=$((count - 1)); fi
    local p90=${sorted[$p90_idx]}

    echo "  $label:"
    echo "    Raw: ${values[*]}"
    echo "    Avg:  ${avg}"
    echo "    P50:  ${p50}"
    echo "    P90:  ${p90}"
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
    echo "  Measurement duration: ${MEASURE_DURATION}s per iteration"
    echo "========================================"
    echo ""

    local MAH_VALUES=()
    local MAHHR_VALUES=()
    local NS_MAH_VALUES=()
    local NS_MAHHR_VALUES=()

    for i in $(seq 1 "$iterations"); do
        echo "--- Iteration $i/$iterations ---"

        # Force-stop any previous run
        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        # Reset battery stats and simulate unplugged state
        echo "  Resetting batterystats and simulating unplugged state..."
        adb shell dumpsys batterystats --reset > /dev/null 2>&1
        adb shell dumpsys battery unplug > /dev/null 2>&1

        if [[ "$scenario" == "idle" ]]; then
            # Simple cold launch + settle
            adb shell am start-activity -W -S "$PKG/$ACTIVITY" > /dev/null 2>&1
            countdown "$settle" "Stabilizing"
        else
            # Instrumentation handles launch, settle, and UI actions
            local action_wait_ms=$((action_wait * 1000))
            # Hold state for measurement duration + 10s buffer
            local measure_duration_ms=$(( (MEASURE_DURATION + 10) * 1000 ))
            echo "  Running UI automation ($scenario, settle=${settle}s, action wait=${action_wait}s)..."
            if ! run_ui_automation "$scenario" "$action_wait_ms" "$measure_duration_ms"; then
                echo "  SKIPPING iteration $i due to automation failure."
                # Restore battery state before skipping
                adb shell dumpsys battery reset > /dev/null 2>&1
                echo ""
                continue
            fi
            echo "  State reached."
        fi

        # Measurement window
        echo "  Measuring battery drain for ${MEASURE_DURATION}s..."
        countdown "$MEASURE_DURATION" "Measuring"

        # Restore battery state
        adb shell dumpsys battery reset > /dev/null 2>&1

        # Collect batterystats
        local stats_file="${TMPDIR_LOCAL}/${scenario}_${i}_batterystats.txt"
        adb shell dumpsys batterystats > "$stats_file" 2>&1

        # Parse per-app drain
        local result
        result=$(parse_app_drain "$stats_file" "$UID_STR") || true

        local app_mah breakdown
        if [[ -z "$result" ]]; then
            echo "  WARNING: App not found in batterystats (negligible power use). Recording as 0 mAh."
            app_mah="0"
            breakdown="(below measurement threshold)"
        else
            app_mah=$(echo "$result" | cut -d'|' -f1)
            breakdown=$(echo "$result" | cut -d'|' -f2)
        fi

        # Convert to mAh/hr
        local mah_hr
        mah_hr=$(echo "scale=4; $app_mah * 3600 / $MEASURE_DURATION" | bc)

        # Extract non-screen drain (SDK-attributable: cpu + wifi + wakelock + sensors etc.)
        local non_screen_mah="0"
        if [[ -n "$breakdown" && "$breakdown" != "(below measurement threshold)" ]]; then
            non_screen_mah=$(echo "$breakdown" | tr ' ' '\n' | grep '=' | grep -v 'screen=' | grep -v ':' | sed 's/.*=//' | awk '{s+=$1} END {printf "%.4f", s}')
            if [[ -z "$non_screen_mah" ]]; then non_screen_mah="0"; fi
        fi
        local non_screen_mah_hr
        non_screen_mah_hr=$(echo "scale=4; $non_screen_mah * 3600 / $MEASURE_DURATION" | bc)

        # Whole-device drain for reference
        local computed_drain
        computed_drain=$(parse_computed_drain "$stats_file")

        echo "  App drain:        ${app_mah} mAh (${mah_hr} mAh/hr)"
        echo "  Non-screen drain: ${non_screen_mah} mAh (${non_screen_mah_hr} mAh/hr)"
        echo "  Breakdown:        ${breakdown:-n/a}"
        echo "  Device drain:     ${computed_drain:-n/a} mAh (total)"

        # Wake lock info
        echo "  Wake locks:"
        extract_wake_locks "$stats_file" "$UID_STR"
        echo ""

        MAH_VALUES+=("$app_mah")
        MAHHR_VALUES+=("$mah_hr")
        NS_MAH_VALUES+=("$non_screen_mah")
        NS_MAHHR_VALUES+=("$non_screen_mah_hr")

        # Wait for test to finish (includes cast cleanup for connected)
        if [[ "$scenario" != "idle" ]]; then
            wait "$UI_AUTOMATION_PID" 2>/dev/null || true
        fi
        adb shell am force-stop "$PKG" 2>/dev/null || true
    done

    local count=${#MAH_VALUES[@]}
    if ((count == 0)); then
        echo "  No successful measurements for $scenario."
        return
    fi

    echo ""
    echo "=== Results: $scenario ($count iterations) ==="
    compute_battery_stats "App drain (mAh)" "${MAH_VALUES[@]}"
    echo ""
    compute_battery_stats "App drain (mAh/hr)" "${MAHHR_VALUES[@]}"
    echo ""
    compute_battery_stats "Non-screen drain (mAh)" "${NS_MAH_VALUES[@]}"
    echo ""
    compute_battery_stats "Non-screen drain (mAh/hr)" "${NS_MAHHR_VALUES[@]}"
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
echo "  Raw batterystats saved in: $TMPDIR_LOCAL"
echo "================================================"
