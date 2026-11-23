# Performance Measurement Steps

Measure each metric with the Vizbee SDK on the main branch. Then create a
separate branch with the SDK removed and repeat to get the delta.

**Prerequisites**: Connect an Android device via USB with Developer Options and
USB Debugging enabled. Verify with `adb devices`.

---

## Startup

### Cold-start time

Measures the total time from process creation to first frame drawn.

**Automated** — builds, installs, and runs 5 cold starts:

```bash
docs/performance/scripts/measure_cold_start.sh
```

Pass a custom iteration count:

```bash
docs/performance/scripts/measure_cold_start.sh 10
```

**Manual** — if you already have the app installed:

```bash
adb shell am force-stop tv.vizbee.demo
adb shell am start-activity -W -S tv.vizbee.demo/.activity.MainActivity
```

Record the `TotalTime` from the output.

### SDK init time (Perfetto trace)

Captures a Perfetto trace during a cold start. The app wraps the full Vizbee
SDK initialization in a `VizbeeSDK.init` trace section which appears in the
trace automatically.

**Automated**:

```bash
docs/performance/scripts/capture_startup_trace.sh
```

Save to a custom path:

```bash
docs/performance/scripts/capture_startup_trace.sh my_trace.perfetto-trace
```

**Manual**:

```bash
adb shell am force-stop tv.vizbee.demo

adb shell perfetto \
  -o /data/misc/perfetto-traces/startup.perfetto-trace \
  -t 15s sched freq idle am wm view -a tv.vizbee.demo &

sleep 2
adb shell am start-activity -S tv.vizbee.demo/.activity.MainActivity

sleep 14
adb pull /data/misc/perfetto-traces/startup.perfetto-trace .
```

Open the trace at [ui.perfetto.dev](https://ui.perfetto.dev) and search
for `VizbeeSDK.init` to see the full SDK initialization duration.

---

## Memory

Measures TOTAL PSS, Java Heap, and Native Heap via `dumpsys meminfo`.

**Automated** — builds, installs, and runs measurements for a given scenario:

```bash
docs/performance/scripts/measure_memory.sh idle        # 30 iterations, fully automatic
docs/performance/scripts/measure_memory.sh discovery    # 10 iterations, pauses for cast icon tap
docs/performance/scripts/measure_memory.sh connected    # 10 iterations, pauses for device connection
```

Pass custom iteration count and stabilization wait (seconds):

```bash
docs/performance/scripts/measure_memory.sh idle 15 45
```

**Manual** — put the app in the target state, then:

```bash
adb shell dumpsys meminfo tv.vizbee.demo
```

Record **TOTAL PSS**, **Java Heap**, and **Native Heap**.

| State | How to reach |
|---|---|
| Idle | App on home screen, 60s after launch |
| Discovery | Cast icon visible, active discovery |
| Connected | Connected to a device, video playing on TV |

---

## CPU

Run `docs/performance/scripts/measure_cpu.sh` (or `measure_all.sh`). It reads the app's CPU time from
`/proc/<pid>/stat` at the start and end of a 60-second window in each state, so it works on every Android version.
CPU % is the app's CPU time divided by the window (100% is one core fully busy). It also prints the CPU time of each
thread. SDK-owned threads: `ServiceHandler`, `GCScannerThread`, SSDP pool threads.

---

## Battery

```bash
adb shell dumpsys batterystats --reset

# ... run the app in target state for 10 minutes ...

adb shell dumpsys batterystats > batterystats.txt
adb bugreport > bugreport.zip
```

Open `bugreport.zip` in [Battery Historian](https://bathist.ef.lc/).
Look for power use, wake locks, and Wi-Fi multicast lock time.

---

## Network

### Data usage

```bash
adb shell dumpsys package tv.vizbee.demo | grep userId=
adb shell cat /proc/net/xt_qtaguid/stats | grep <uid>
```

Sample at start and end of a fixed time window to get KB/min.

### Discovery traffic

Wireshark on the same Wi-Fi network, filtered by device IP:
- mDNS: `udp.port == 5353`
- SSDP: `udp.port == 1900`

---

## Size

Use a release build (minified, resource-shrunk) for accurate numbers.
Build AABs and use `bundletool` for per-ABI download size.

```bash
# 1. Clean and build the release AAB
./gradlew app:clean app:bundleRelease

# 2. Generate split APKs from the AAB
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --overwrite

# 3. Get total download size (MIN/MAX across all device configs)
bundletool get-size total --apks=app.apks

# 4. Get per-ABI breakdown
bundletool get-size total --apks=app.apks --dimensions=ABI
```

Repeat on the branch without the SDK and compare the numbers.
