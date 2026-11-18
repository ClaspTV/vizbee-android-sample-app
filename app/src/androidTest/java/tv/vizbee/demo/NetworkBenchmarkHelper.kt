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
 * UIAutomator helper for network traffic benchmarks.
 *
 * Puts the app into idle, discovery, or connected state, then measures
 * per-UID network bytes via TrafficStats over a measurement window.
 *
 * Instrumentation args:
 *   scenario        - "idle", "discovery", or "connected"
 *   deviceName      - Chromecast friendly name (required for "connected")
 *   settleTime      - ms to wait after launch before actions (default 60000)
 *   actionWait      - ms to wait after the UI action (default 10000)
 *   measureDuration - ms to measure network traffic (default 180000 = 3 min)
 */
@RunWith(AndroidJUnit4::class)
class NetworkBenchmarkHelper {

    private lateinit var device: UiDevice
    private lateinit var args: Bundle

    companion object {
        private const val PKG = "tv.vizbee.demo"
        private const val CAST_BUTTON_RES = "$PKG:id/clasptv_remote_menu_item"
        private const val DEVICE_LIST_RES = "$PKG:id/deviceList_list"
        private const val DEVICE_NAME_RES = "$PKG:id/deviceinfo_friendly_name"
        private const val DEVICE_CARD_RES = "$PKG:id/vzb_bottom_sheet_bottom_sheet"

        private const val DEFAULT_ACTION_WAIT = 10_000L
        private const val DEFAULT_SETTLE_TIME = 60_000L
        private const val DEFAULT_MEASURE_DURATION = 180_000L
        private const val DISCOVERY_TIMEOUT = 20_000L
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

        // Wait for the app to fully load
        Thread.sleep(settleTime)

        // Reach target state
        when (scenario) {
            "discovery" -> {
                clickCastIcon()
                val card = device.wait(Until.findObject(By.res(DEVICE_CARD_RES)), DISCOVERY_TIMEOUT)
                if (card == null) {
                    fail("Device selection card did not appear within ${DISCOVERY_TIMEOUT}ms")
                }
                Thread.sleep(actionWait)
            }
            "connected" -> {
                clickCastIcon()
                val deviceName: String = args.getString("deviceName")
                    ?: run { fail("deviceName argument is required for connected scenario"); return }
                selectDevice(deviceName, actionWait)
            }
            // "idle" — no UI action needed
        }

        // Signal that state is reached
        val stateBundle = Bundle()
        stateBundle.putString("state", scenario)
        stateBundle.putString("status", "ready")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, stateBundle)

        // Measure network traffic over the measurement window
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

        // Output results via instrumentation status
        val result = Bundle()
        result.putString("status", "done")
        result.putString("rxBytes", rxDelta.toString())
        result.putString("txBytes", txDelta.toString())
        result.putString("totalBytes", totalDelta.toString())
        result.putString("durationMs", elapsed.toString())
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result)
    }

    private fun clickCastIcon() {
        val castButton = device.wait(Until.findObject(By.res(CAST_BUTTON_RES)), 10_000)
        if (castButton == null) {
            fail("Cast button not found (resource: $CAST_BUTTON_RES)")
        }
        castButton!!.click()
    }

    private fun selectDevice(deviceName: String, actionWait: Long) {
        val list = device.wait(Until.findObject(By.res(DEVICE_LIST_RES)), DISCOVERY_TIMEOUT)
        if (list == null) {
            fail("Device list did not appear within ${DISCOVERY_TIMEOUT}ms")
        }

        device.wait(Until.findObject(By.res(DEVICE_NAME_RES)), DISCOVERY_TIMEOUT)

        val allDevices = device.findObjects(By.res(DEVICE_NAME_RES))
        val target = allDevices.firstOrNull {
            it.text?.equals(deviceName, ignoreCase = true) == true
        }
        if (target == null) {
            val available = allDevices.mapNotNull { it.text }
            fail(
                "Device '$deviceName' not found in device list. " +
                "Available devices: ${available.joinToString(", ") { "'$it'" }}"
            )
        }

        target!!.click()
        Thread.sleep(actionWait)
    }
}
