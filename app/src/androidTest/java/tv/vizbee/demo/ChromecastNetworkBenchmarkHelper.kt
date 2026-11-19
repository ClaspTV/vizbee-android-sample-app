package tv.vizbee.demo

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Bundle
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UIAutomator helper for Chromecast network traffic benchmarks.
 *
 * Puts the app into idle, discovery, or connected state via the standard
 * Google Cast MediaRouteChooserDialog, then measures per-UID network bytes
 * via TrafficStats over a measurement window.
 *
 * Instrumentation args:
 *   scenario        - "idle", "discovery", or "connected"
 *   deviceName      - Chromecast friendly name (required for "connected")
 *   settleTime      - ms to wait after launch before actions (default 60000)
 *   actionWait      - ms to wait after casting starts (default 10000)
 *   measureDuration - ms to measure network traffic (default 180000 = 3 min)
 */
@RunWith(AndroidJUnit4::class)
class ChromecastNetworkBenchmarkHelper {

    private lateinit var device: UiDevice
    private lateinit var args: Bundle

    companion object {
        private const val PKG = "tv.vizbee.demo"

        // Google Cast dialog resource IDs
        private const val CAST_BUTTON_DISCONNECTED_DESC = "Cast. Disconnected"
        private const val CAST_BUTTON_CONNECTED_DESC = "Cast. Connected"
        private const val CHOOSER_TITLE_RES = "$PKG:id/mr_chooser_title"
        private const val ROUTE_NAME_RES = "$PKG:id/mr_chooser_route_name"
        private const val CAST_STOP_BUTTON_RES = "$PKG:id/mr_cast_stop_button"

        // App resource IDs
        private const val VIDEO_LIST_RES = "$PKG:id/VideoList"
        private const val WATCH_NOW_RES = "$PKG:id/btn_watchNow"
        private const val FIRST_VIDEO_TITLE = "Elephants Dream"

        private const val DEFAULT_ACTION_WAIT = 10_000L
        private const val DEFAULT_SETTLE_TIME = 60_000L
        private const val DEFAULT_MEASURE_DURATION = 180_000L
        private const val DIALOG_TIMEOUT = 20_000L
        private const val CONNECTION_TIMEOUT = 60_000L
        private const val UI_TIMEOUT = 10_000L
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        args = InstrumentationRegistry.getArguments()
    }

    @Test
    fun measureNetwork() {
        val scenario = args.getString("scenario", "idle")
        val actionWait = args.getString("actionWait", "$DEFAULT_ACTION_WAIT")!!.toLong()
        val settleTime = args.getString("settleTime", "$DEFAULT_SETTLE_TIME")!!.toLong()
        val measureDuration = args.getString("measureDuration", "$DEFAULT_MEASURE_DURATION")!!.toLong()

        // Launch the app
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PKG)
            ?: run { fail("Could not get launch intent for $PKG"); return }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        Thread.sleep(settleTime)

        // Reach target state
        when (scenario) {
            "discovery" -> {
                clickCastIcon()
                val dialog = device.wait(Until.findObject(By.res(CHOOSER_TITLE_RES)), DIALOG_TIMEOUT)
                if (dialog == null) {
                    fail("Google Cast chooser dialog did not appear within ${DIALOG_TIMEOUT}ms")
                }
                Thread.sleep(actionWait)
            }
            "connected" -> {
                clickCastIcon()
                val dialog = device.wait(Until.findObject(By.res(CHOOSER_TITLE_RES)), DIALOG_TIMEOUT)
                if (dialog == null) {
                    fail("Google Cast chooser dialog did not appear within ${DIALOG_TIMEOUT}ms")
                }
                val deviceName: String = args.getString("deviceName")
                    ?: run { fail("deviceName argument is required for connected scenario"); return }
                selectDevice(deviceName)
                waitForConnection()
                castVideo(actionWait)
            }
        }

        // Signal that state is reached
        val stateBundle = Bundle()
        stateBundle.putString("state", scenario)
        stateBundle.putString("status", "ready")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, stateBundle)

        // Measure network traffic
        val uid = Process.myUid()
        val rxBefore = TrafficStats.getUidRxBytes(uid)
        val txBefore = TrafficStats.getUidTxBytes(uid)
        val startTime = System.currentTimeMillis()

        Thread.sleep(measureDuration)

        val rxAfter = TrafficStats.getUidRxBytes(uid)
        val txAfter = TrafficStats.getUidTxBytes(uid)
        val elapsed = System.currentTimeMillis() - startTime

        val rxDelta = if (rxBefore == TrafficStats.UNSUPPORTED.toLong() || rxAfter == TrafficStats.UNSUPPORTED.toLong()) -1L else rxAfter - rxBefore
        val txDelta = if (txBefore == TrafficStats.UNSUPPORTED.toLong() || txAfter == TrafficStats.UNSUPPORTED.toLong()) -1L else txAfter - txBefore
        val totalDelta = if (rxDelta < 0 || txDelta < 0) -1L else rxDelta + txDelta

        // Output results
        val result = Bundle()
        result.putString("status", "done")
        result.putString("rxBytes", rxDelta.toString())
        result.putString("txBytes", txDelta.toString())
        result.putString("totalBytes", totalDelta.toString())
        result.putString("durationMs", elapsed.toString())
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result)

        // Clean up cast session so the TV stops playing
        if (scenario == "connected") {
            stopCasting()
        }
    }

    // --- UI actions ---

    private fun clickCastIcon() {
        val castButton = device.wait(
            Until.findObject(By.desc(CAST_BUTTON_DISCONNECTED_DESC)),
            10_000
        )
        if (castButton == null) {
            fail("Cast button not found (content-desc: $CAST_BUTTON_DISCONNECTED_DESC)")
        }
        castButton!!.click()
    }

    private fun selectDevice(deviceName: String) {
        device.wait(Until.findObject(By.res(ROUTE_NAME_RES)), DIALOG_TIMEOUT)

        val allDevices = device.findObjects(By.res(ROUTE_NAME_RES))
        val target = allDevices.firstOrNull {
            it.text?.equals(deviceName, ignoreCase = true) == true
        }
        if (target == null) {
            val available = allDevices.mapNotNull { it.text }
            fail(
                "Device '$deviceName' not found in cast dialog. " +
                "Available devices: ${available.joinToString(", ") { "'$it'" }}"
            )
        }

        target!!.click()
    }

    private fun waitForConnection() {
        val gallery = device.wait(Until.findObject(By.res(VIDEO_LIST_RES)), CONNECTION_TIMEOUT)
        if (gallery == null) {
            fail("Video gallery did not reappear after device selection (connection may have failed)")
        }
        val connected = device.wait(
            Until.findObject(By.desc(CAST_BUTTON_CONNECTED_DESC)),
            CONNECTION_TIMEOUT
        )
        if (connected == null) {
            fail("Cast session did not reach connected state")
        }
        // The cast icon updates before the SDK session is fully connected.
        // Allow time for the internal handshake to complete.
        Thread.sleep(5_000)
    }

    private fun castVideo(actionWait: Long) {
        val video = device.wait(Until.findObject(By.text(FIRST_VIDEO_TITLE)), UI_TIMEOUT)
        if (video == null) {
            fail("Video '$FIRST_VIDEO_TITLE' not found in gallery")
        }
        video!!.click()

        val watchNow = device.wait(Until.findObject(By.res(WATCH_NOW_RES)), UI_TIMEOUT)
        if (watchNow == null) {
            fail("Watch Now button not found")
        }
        watchNow!!.click()

        Thread.sleep(actionWait)
    }

    private fun stopCasting() {
        val castButton = device.wait(
            Until.findObject(By.desc(CAST_BUTTON_CONNECTED_DESC)),
            5_000
        )
        if (castButton == null) {
            device.pressBack()
            device.wait(Until.findObject(By.desc(CAST_BUTTON_CONNECTED_DESC)), UI_TIMEOUT)
                ?.click() ?: return
        } else {
            castButton.click()
        }

        val stopButton = device.wait(Until.findObject(By.textContains("STOP CASTING")), UI_TIMEOUT)
            ?: device.findObject(By.textContains("Stop casting"))
            ?: device.findObject(By.textContains("Stop Casting"))
        stopButton?.click()
    }
}
