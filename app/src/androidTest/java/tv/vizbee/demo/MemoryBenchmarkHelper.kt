package tv.vizbee.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
 * UIAutomator helper for memory benchmarks.
 *
 * Puts the app into discovery or connected state so the shell script
 * can capture dumpsys meminfo externally.
 *
 * Instrumentation args:
 *   deviceName  - Chromecast friendly name (required for "connected")
 *   scenario    - "discovery" or "connected"
 *   actionWait  - ms to wait after the UI action (default 10000)
 */
@RunWith(AndroidJUnit4::class)
class MemoryBenchmarkHelper {

    private lateinit var device: UiDevice
    private lateinit var args: Bundle

    companion object {
        private const val PKG = "tv.vizbee.demo"
        // Vizbee SDK resource IDs
        private const val CAST_BUTTON_RES = "$PKG:id/clasptv_remote_menu_item"
        private const val DEVICE_LIST_RES = "$PKG:id/deviceList_list"
        private const val DEVICE_NAME_RES = "$PKG:id/deviceinfo_friendly_name"
        private const val DEVICE_CARD_RES = "$PKG:id/vzb_bottom_sheet_bottom_sheet"

        private const val DEFAULT_ACTION_WAIT = 10_000L
        private const val DISCOVERY_TIMEOUT = 20_000L
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        args = InstrumentationRegistry.getArguments()
    }

    @Test
    fun putInState() {
        val scenario = args.getString("scenario", "discovery")
        val actionWait = args.getString("actionWait", "$DEFAULT_ACTION_WAIT")!!.toLong()
        val settleTime = args.getString("settleTime", "60000")!!.toLong()

        // Launch the app's main activity explicitly
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PKG)
            ?: run { fail("Could not get launch intent for $PKG"); return }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        // Wait for the app to fully load
        Thread.sleep(settleTime)

        clickCastIcon()

        if (scenario == "connected") {
            val deviceName: String = args.getString("deviceName")
                ?: run { fail("deviceName argument is required for connected scenario"); return }
            selectDevice(deviceName, actionWait)
        } else {
            // Discovery: just wait for the device list card to appear
            val card = device.wait(Until.findObject(By.res(DEVICE_CARD_RES)), DISCOVERY_TIMEOUT)
            if (card == null) {
                fail("Device selection card did not appear within ${DISCOVERY_TIMEOUT}ms")
            }
            Thread.sleep(actionWait)
        }

        // Signal ready by printing to instrumentation output
        val result = Bundle()
        result.putString("state", scenario)
        result.putString("status", "ready")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result)

        // Keep the test alive so the state is maintained while script reads meminfo.
        // The script will force-stop the app to end this.
        Thread.sleep(300_000)
    }

    private fun clickCastIcon() {
        val castButton = device.wait(Until.findObject(By.res(CAST_BUTTON_RES)), 10_000)
        if (castButton == null) {
            fail("Cast button not found (resource: $CAST_BUTTON_RES)")
        }
        castButton!!.click()
    }

    private fun selectDevice(deviceName: String, actionWait: Long) {
        // Wait for device list to appear
        val list = device.wait(Until.findObject(By.res(DEVICE_LIST_RES)), DISCOVERY_TIMEOUT)
        if (list == null) {
            fail("Device list did not appear within ${DISCOVERY_TIMEOUT}ms")
        }

        // Wait for device name views to populate
        device.wait(Until.findObject(By.res(DEVICE_NAME_RES)), DISCOVERY_TIMEOUT)

        // Find device by case-insensitive name match (SDK uppercases names)
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
