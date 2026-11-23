#!/usr/bin/env bash
#
# Measures CPU usage (%) of the app across idle, discovery, and connected
# scenarios from the kernel's per-process CPU counters (/proc), on any Android version.
#
# Can run standalone (prompts user) or be called from measure_all.sh with
# all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_cpu.sh
#   From unified: ./measure_cpu.sh --skip-build --device-name "TV Name" \
#                   --idle-iters 5 --disc-iters 5 --conn-iters 5 \
#                   --settle-time 60 --trace-duration 60 \
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
        --trace-duration)    TRACE_DURATION="$2"; shift 2 ;;
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
TMPDIR_LOCAL=$(mktemp -d "${TMPDIR:-/tmp}/cpu_bench.XXXXXX")


# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "================================================"
echo "  CPU Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Collect configuration (prompt only for values not set via CLI) ---

prompt_param DEVICE_NAME      "Chromecast device name (as shown in Vizbee device list)" "CC with Google TV TV 2"
echo ""

prompt_param IDLE_ITERS       "Iterations for IDLE scenario"      5
prompt_param DISC_ITERS       "Iterations for DISCOVERY scenario"  5
prompt_param CONN_ITERS       "Iterations for CONNECTED scenario"  5
echo ""

prompt_param SETTLE_TIME      "Initial settling time after launch, seconds"            60
prompt_param TRACE_DURATION   "CPU measurement window, seconds"                       60
prompt_param DISC_ACTION_WAIT "Wait time after cast icon click (discovery), seconds"   10
prompt_param CONN_ACTION_WAIT "Wait time after casting starts (connected), seconds"    15
echo ""

# --- Estimate total time ---

est_idle=$((IDLE_ITERS * (SETTLE_TIME + TRACE_DURATION + 4)))
est_disc=$((DISC_ITERS * (SETTLE_TIME + DISC_ACTION_WAIT + TRACE_DURATION + 14)))
est_conn=$((CONN_ITERS * (SETTLE_TIME + CONN_ACTION_WAIT + TRACE_DURATION + 14)))
est_total=$((est_idle + est_disc + est_conn))
est_min=$((est_total / 60))

echo "--- Configuration ---"
echo "  Device name:       $DEVICE_NAME"
echo "  Idle iterations:   $IDLE_ITERS"
echo "  Disc iterations:   $DISC_ITERS"
echo "  Conn iterations:   $CONN_ITERS"
echo "  Settle time:       ${SETTLE_TIME}s"
echo "  CPU window:        ${TRACE_DURATION}s"
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
        ${PKG}.test/${TEST_RUNNER}" > /tmp/cputest_instrument.log 2>&1 &
    UI_AUTOMATION_PID=$!

    local waited=0
    local max_wait=$(( (SETTLE_TIME + action_wait_ms / 1000 + 90) ))
    while ((waited < max_wait)); do
        if grep -q "status=ready" /tmp/cputest_instrument.log 2>/dev/null; then
            return 0
        fi
        if grep -q "FAILURES\!\!\!" /tmp/cputest_instrument.log 2>/dev/null; then
            echo ""
            echo "  ERROR: Instrumentation test failed:"
            cat /tmp/cputest_instrument.log
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

# Prints the app's CPU counters: its pid, the uptime in seconds, the process's CPU ticks (all threads, including
# threads that ended), then one line per live thread: tid, CPU ticks, name. Fields are read after the ")" that
# ends the name, because thread names can contain spaces. A tick is 1/100 s on Android.
proc_snapshot() {
    adb shell "p=\$(pidof $PKG); [ -n \"\$p\" ] || exit 1; echo \"pid \$p\"; echo \"uptime \$(cut -d' ' -f1 /proc/uptime)\"; echo \"process \$(sed 's/^.*) //' /proc/\$p/stat | cut -d' ' -f12,13)\"; for t in /proc/\$p/task/*; do s=\$(sed 's/^.*) //' \$t/stat 2>/dev/null | cut -d' ' -f12,13) && [ -n \"\$s\" ] && echo \"thread \${t##*/} \$s \$(cat \$t/comm 2>/dev/null)\"; done" | tr -d '\r'
}

# Measures the app's CPU usage (%) over TRACE_DURATION seconds from the kernel's counters in /proc. 100% is one
# core fully busy. Prints nothing if the app was not running or restarted during the window.
capture_and_analyze() {
    local sample_file="$1"

    proc_snapshot > "$sample_file.before" || true
    countdown "$TRACE_DURATION" "Measuring CPU" >&2
    proc_snapshot > "$sample_file.after" || true

    awk '
        FNR == 1 { f++ }
        $1 == "pid" { pid[f] = $2 }
        $1 == "uptime" { up[f] = $2 }
        $1 == "process" { ticks[f] = $2 + $3 }
        END {
            if (pid[1] == "" || pid[1] != pid[2] || up[2] <= up[1]) exit
            printf "%.4f\n", (ticks[2] - ticks[1]) / (up[2] - up[1])
        }' "$sample_file.before" "$sample_file.after"
}

# Per-thread CPU over the window, threads with the same name added together, busiest first
extract_thread_breakdown() {
    local sample_file="$1"

    printf "%10s %8s  %s\n" "total_ms" "cpu_pct" "thread_name"
    awk '
        FNR == 1 { f++ }
        $1 == "uptime" { up[f] = $2 }
        $1 == "thread" {
            name = $5; for (i = 6; i <= NF; i++) name = name " " $i
            if (f == 1) before[$2] = $3 + $4; else ms[name] += ($3 + $4 - before[$2]) * 10
        }
        END {
            for (n in ms) if (ms[n] > 0) printf "%10.0f %8.4f  %s\n", ms[n], ms[n] / 10 / (up[2] - up[1]), n
        }' "$sample_file.before" "$sample_file.after" | sort -rn | head -15
}

compute_cpu_stats() {
    local label=$1
    shift
    local values=("$@")
    local count=${#values[@]}

    local sorted=($(printf '%s\n' "${values[@]}" | sort -g))

    local sum
    sum=$(printf '%s\n' "${values[@]}" | awk '{s+=$1} END {print s}')
    local avg
    avg=$(echo "scale=4; $sum / $count" | bc)

    local mid=$((count / 2))
    local p50
    if ((count % 2 == 0)); then
        p50=$(echo "scale=4; (${sorted[mid - 1]} + ${sorted[mid]}) / 2" | bc)
    else
        p50=${sorted[$mid]}
    fi

    local p90_idx
    p90_idx=$(echo "$count * 9 / 10" | bc)
    if ((p90_idx >= count)); then p90_idx=$((count - 1)); fi
    local p90=${sorted[$p90_idx]}

    echo "  $label:"
    echo "    Raw (%): ${values[*]}"
    echo "    Avg:  ${avg}%"
    echo "    P50:  ${p50}%"
    echo "    P90:  ${p90}%"
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

    local CPU_VALUES=()
    local LAST_TRACE=""

    for i in $(seq 1 "$iterations"); do
        echo "--- Iteration $i/$iterations ---"

        # Force-stop any previous run
        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        local trace_file="${TMPDIR_LOCAL}/${scenario}_${i}.cpu"

        if [[ "$scenario" == "idle" ]]; then
            # Simple cold launch + wait
            adb shell am start-activity -W -S "$PKG/$ACTIVITY" > /dev/null 2>&1
            countdown "$settle" "Stabilizing"

            echo "  Measuring CPU (${TRACE_DURATION}s)..."
            local cpu_pct
            cpu_pct=$(capture_and_analyze "$trace_file")
        else
            # Instrumentation handles launch, settle, and UI actions
            local action_wait_ms=$((action_wait * 1000))
            # Hold state for trace duration + 10s buffer
            local measure_duration_ms=$(( (TRACE_DURATION + 10) * 1000 ))
            echo "  Running UI automation ($scenario, settle=${settle}s, action wait=${action_wait}s)..."
            if ! run_ui_automation "$scenario" "$action_wait_ms" "$measure_duration_ms"; then
                echo "  SKIPPING iteration $i due to automation failure."
                echo ""
                continue
            fi
            echo "  State reached. Measuring CPU (${TRACE_DURATION}s)..."
            local cpu_pct
            cpu_pct=$(capture_and_analyze "$trace_file")
        fi

        if [[ -z "$cpu_pct" ]]; then
            echo "  SKIPPING iteration $i — failed to extract CPU usage."
            echo ""
            continue
        fi

        echo "  CPU usage: ${cpu_pct}%"
        echo ""

        CPU_VALUES+=("$cpu_pct")
        LAST_TRACE="$trace_file"

        # Wait for test to finish (includes cast cleanup for connected)
        if [[ "$scenario" != "idle" ]]; then
            wait "$UI_AUTOMATION_PID" 2>/dev/null || true
        fi
        adb shell am force-stop "$PKG" 2>/dev/null || true
    done

    local count=${#CPU_VALUES[@]}
    if ((count == 0)); then
        echo "  No successful measurements for $scenario."
        return
    fi

    echo ""
    echo "=== Results: $scenario ($count successful iterations) ==="
    compute_cpu_stats "CPU usage" "${CPU_VALUES[@]}"

    # Show per-thread breakdown from the last successful iteration
    if [[ -n "$LAST_TRACE" && -f "$LAST_TRACE.after" ]]; then
        echo ""
        echo "  Per-thread breakdown (last iteration):"
        extract_thread_breakdown "$LAST_TRACE" | while IFS= read -r line; do
            echo "    $line"
        done
    fi
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
echo "  CPU samples saved in: $TMPDIR_LOCAL"
echo "================================================"
