#!/usr/bin/env bash
#
# Measures CPU usage (%) of the app with Google Cast SDK across idle,
# discovery, and connected scenarios using Perfetto traces + trace_processor.
#
# Can run standalone (prompts user) or be called from measure_all_chromecast.sh
# with all parameters pre-set via CLI args.
#
# Usage:
#   Standalone:  ./measure_cpu_chromecast.sh
#   From unified: ./measure_cpu_chromecast.sh --skip-build \
#                   --device-name "TV Name" --idle-iters 5 ...
#
# Prerequisites:
#   - Android device connected via USB with USB debugging enabled
#   - trace_processor on PATH, or the android-profiler skill bundled binary
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

# --- trace_processor detection ---

TRACE_PROCESSOR="${TRACE_PROCESSOR:-}"
SKILL_TP="${PROJECT_DIR}/.claude/skills/android-profiler/references/perfetto/bin/trace_processor"
if [[ -z "$TRACE_PROCESSOR" ]]; then
    if command -v trace_processor_shell &>/dev/null; then
        TRACE_PROCESSOR="trace_processor_shell"
    elif command -v trace_processor &>/dev/null; then
        TRACE_PROCESSOR="trace_processor"
    elif [[ -x "$SKILL_TP" ]]; then
        chmod +x "$SKILL_TP"
        TRACE_PROCESSOR="$SKILL_TP"
    elif [[ -x "./trace_processor" ]]; then
        TRACE_PROCESSOR="./trace_processor"
    else
        echo "ERROR: trace_processor not found."
        echo ""
        echo "Install it:"
        echo "  curl -LO https://get.perfetto.dev/trace_processor"
        echo "  chmod +x trace_processor"
        echo "  sudo mv trace_processor /usr/local/bin/trace_processor"
        echo ""
        echo "Or set TRACE_PROCESSOR=/path/to/trace_processor"
        exit 1
    fi
fi
echo "Using trace processor: $TRACE_PROCESSOR"

PKG="tv.vizbee.demo"
ACTIVITY=".activity.MainActivity"
TEST_CLASS="tv.vizbee.demo.ChromecastMemoryBenchmarkHelper"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
DEVICE_TRACE="/data/misc/perfetto-traces/cpu_bench.perfetto-trace"
DEVICE_CONFIG="/data/misc/perfetto-configs/cpu_bench.pbtxt"
TMPDIR_LOCAL=$(mktemp -d "${TMPDIR:-/tmp}/cpu_bench_cc.XXXXXX")

TRACE_CONFIG_FILE="${TMPDIR_LOCAL}/cpu_trace_config.pbtxt"

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "================================================"
echo "  Chromecast CPU Benchmark — $DEVICE (Android $ANDROID_VER)"
echo "================================================"
echo ""

# --- Collect configuration (prompt only for values not set via CLI) ---

prompt_param DEVICE_NAME      "Chromecast device name (as shown in Google Cast dialog)" "CC with Google TV TV 2"
echo ""

prompt_param IDLE_ITERS       "Iterations for IDLE scenario"      5
prompt_param DISC_ITERS       "Iterations for DISCOVERY scenario"  5
prompt_param CONN_ITERS       "Iterations for CONNECTED scenario"  5
echo ""

prompt_param SETTLE_TIME      "Initial settling time after launch, seconds"            60
prompt_param TRACE_DURATION   "Perfetto trace duration, seconds"                       60
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
echo "  Trace duration:    ${TRACE_DURATION}s"
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

# --- Write Perfetto trace config ---

generate_trace_config() {
    cat > "$TRACE_CONFIG_FILE" <<PBTXT
buffers {
  size_kb: 32768
  fill_policy: RING_BUFFER
}
duration_ms: $((TRACE_DURATION * 1000))

data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "sched/sched_waking"
      ftrace_events: "sched/sched_process_exit"
      ftrace_events: "sched/sched_process_free"
      ftrace_events: "sched/sched_process_fork"
      ftrace_events: "task/task_newtask"
      ftrace_events: "task/task_rename"
      ftrace_events: "power/cpu_frequency"
      ftrace_events: "power/cpu_idle"
      ftrace_events: "power/suspend_resume"
      atrace_apps: "$PKG"
    }
  }
}

data_sources {
  config {
    name: "linux.process_stats"
    process_stats_config {
      scan_all_processes_on_start: true
      proc_stats_poll_ms: 10000
    }
  }
}
PBTXT
}

generate_trace_config

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
        ${PKG}.test/${TEST_RUNNER}" > /tmp/cputest_cc_instrument.log 2>&1 &
    UI_AUTOMATION_PID=$!

    local waited=0
    local max_wait=$(( (SETTLE_TIME + action_wait_ms / 1000 + 90) ))
    while ((waited < max_wait)); do
        if grep -q "status=ready" /tmp/cputest_cc_instrument.log 2>/dev/null; then
            return 0
        fi
        if grep -q "FAILURES\!\!\!" /tmp/cputest_cc_instrument.log 2>/dev/null; then
            echo ""
            echo "  ERROR: Instrumentation test failed:"
            cat /tmp/cputest_cc_instrument.log
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

capture_and_analyze() {
    local trace_file="$1"

    adb push "$TRACE_CONFIG_FILE" "$DEVICE_CONFIG" > /dev/null 2>&1
    adb shell perfetto \
        -c "$DEVICE_CONFIG" --txt \
        -o "$DEVICE_TRACE" \
        --background > /dev/null 2>&1

    countdown "$TRACE_DURATION" "Recording trace" >&2

    sleep 2

    adb pull "$DEVICE_TRACE" "$trace_file" > /dev/null 2>&1

    local sql
    sql="SELECT ROUND(
      SUM(IIF(ss.dur = -1, trace_end() - ss.ts, ss.dur)) * 100.0
      / (trace_end() - trace_start()), 4
    ) AS cpu_pct
    FROM sched_slice AS ss
    JOIN thread AS t USING(utid)
    WHERE t.upid = COALESCE(
      (SELECT t2.upid FROM thread AS t2
       WHERE t2.name = '${PKG}' AND t2.is_main_thread = 1 LIMIT 1),
      (SELECT p.upid FROM process AS p
       WHERE p.name = '${PKG}' LIMIT 1)
    );"

    local result
    result=$("$TRACE_PROCESSOR" query "$trace_file" "$sql" 2>/dev/null | tail -1 | tr -d '[:space:]"')

    if [[ -z "$result" || "$result" == "NULL" || "$result" == "[NULL]" ]]; then
        echo ""
        return 1
    fi

    echo "$result"
}

extract_thread_breakdown() {
    local trace_file="$1"

    local sql
    sql="SELECT
      t.name AS thread_name,
      ROUND(SUM(IIF(ss.dur = -1, trace_end() - ss.ts, ss.dur)) / 1e6, 1) AS total_ms,
      ROUND(SUM(IIF(ss.dur = -1, trace_end() - ss.ts, ss.dur)) * 100.0
        / (trace_end() - trace_start()), 4) AS cpu_pct
    FROM sched_slice AS ss
    JOIN thread AS t USING(utid)
    WHERE t.upid = COALESCE(
      (SELECT t2.upid FROM thread AS t2
       WHERE t2.name = '${PKG}' AND t2.is_main_thread = 1 LIMIT 1),
      (SELECT p.upid FROM process AS p
       WHERE p.name = '${PKG}' LIMIT 1)
    )
    GROUP BY t.name
    ORDER BY total_ms DESC
    LIMIT 15;"

    "$TRACE_PROCESSOR" query "$trace_file" "$sql" 2>/dev/null
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

    local CPU_VALUES=()
    local LAST_TRACE=""

    for i in $(seq 1 "$iterations"); do
        echo "--- Iteration $i/$iterations ---"

        adb shell am force-stop "$PKG" 2>/dev/null || true
        sleep 1

        local trace_file="${TMPDIR_LOCAL}/${scenario}_${i}.perfetto-trace"

        if [[ "$scenario" == "idle" ]]; then
            adb shell am start-activity -W -S "$PKG/$ACTIVITY" > /dev/null 2>&1
            countdown "$settle" "Stabilizing"

            echo "  Capturing Perfetto trace (${TRACE_DURATION}s)..."
            local cpu_pct
            cpu_pct=$(capture_and_analyze "$trace_file")
        else
            local action_wait_ms=$((action_wait * 1000))
            local measure_duration_ms=$(( (TRACE_DURATION + 10) * 1000 ))
            echo "  Running UI automation ($scenario, settle=${settle}s, action wait=${action_wait}s)..."
            if ! run_ui_automation "$scenario" "$action_wait_ms" "$measure_duration_ms"; then
                echo "  SKIPPING iteration $i due to automation failure."
                echo ""
                continue
            fi
            echo "  State reached. Capturing Perfetto trace (${TRACE_DURATION}s)..."
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

    if [[ -n "$LAST_TRACE" && -f "$LAST_TRACE" ]]; then
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
echo "  Traces saved in: $TMPDIR_LOCAL"
echo "================================================"
