#!/usr/bin/env bash
#
# Captures a Perfetto trace during a cold start of the app.
# The trace includes CPU scheduling, activity manager, and app-level
# trace sections (e.g. VizbeeSDK.init).
#
# Usage: ./capture_startup_trace.sh [output_file]
#   output_file  Path to save the trace (default: startup_trace.perfetto-trace)
#
# Open the trace at https://ui.perfetto.dev and search for "VizbeeSDK.init".

set -euo pipefail

OUTPUT="${1:-startup_trace.perfetto-trace}"
PKG="tv.vizbee.demo"
ACTIVITY=".activity.MainActivity"
DEVICE_TRACE="/data/misc/perfetto-traces/startup.perfetto-trace"
TRACE_DURATION=15

# --- Preflight ---

if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo "ERROR: No Android device connected. Connect a device via USB and enable USB debugging."
    exit 1
fi

DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VER=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "Device: $DEVICE (Android $ANDROID_VER)"
echo ""

# --- Capture ---

echo "Force-stopping app..."
adb shell am force-stop "$PKG"
sleep 1

echo "Starting Perfetto trace (${TRACE_DURATION}s)..."
adb shell perfetto \
    -o "$DEVICE_TRACE" \
    -t "${TRACE_DURATION}s" \
    sched freq idle am wm view \
    -a "$PKG" \
    --background > /dev/null 2>&1

sleep 2

echo "Cold-launching app..."
adb shell am start-activity -S "$PKG/$ACTIVITY" > /dev/null 2>&1

echo "Waiting for trace to finish..."
sleep $((TRACE_DURATION - 1))

echo "Pulling trace..."
adb pull "$DEVICE_TRACE" "$OUTPUT" > /dev/null 2>&1

echo ""
echo "=== Done ==="
echo "Trace saved to: $OUTPUT"
echo ""
echo "Open at https://ui.perfetto.dev and search for:"
echo "  VizbeeSDK.init  — Full SDK initialization time"
