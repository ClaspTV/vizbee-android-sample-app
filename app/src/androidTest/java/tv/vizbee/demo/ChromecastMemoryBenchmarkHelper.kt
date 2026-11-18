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
 * UIAutomator helper for Chromecast memory benchmarks.
 *
 * Puts the app into discovery or connected state via the standard
 * Google Cast MediaRouteChooserDialog so the shell script can capture
 * dumpsys meminfo externally.
 *
 * Instrumentation args:
 *   deviceName  - Chromecast friendly name (required for "connected")
 *   scenario    - "discovery" or "connected"
 *   actionWait  - ms to wait after the UI action (default 10000)
 *   settleTime  - ms to wait after launch before interacting (default 60000)
 */
@RunWith(AndroidJUnit4::class)
class ChromecastMemoryBenchmarkHelper {

    private lateinit var device: UiDevice
    private lateinit var args: Bundle

    companion object {
        private const val PKG = "tv.vizbee.demo"
        // Google Cast dialog resource IDs (from MediaRouterThemeHelper / MediaRouteChooserDialog)
        private const val CAST_BUTTON_DESC = "Cast. Disconnected"
        private const val CHOOSER_TITLE_RES = "$PKG:id/mr_chooser_title"
        private const val CHOOSER_LIST_RES = "$PKG:id/mr_chooser_list"
        private const val ROUTE_NAME_RES = "$PKG:id/mr_chooser_route_name"

        private const val DEFAULT_ACTION_WAIT = 10_000L
        private const val DIALOG_TIMEOUT = 20_000L
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

        // Launch the app's main activity
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PKG)
            ?: run { fail("Could not get launch intent for $PKG"); return }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        // Wait for the app to fully load
        Thread.sleep(settleTime)

        clickCastIcon()

        // Wait for the chooser dialog to appear
        val dialog = device.wait(Until.findObject(By.res(CHOOSER_TITLE_RES)), DIALOG_TIMEOUT)
        if (dialog == null) {
            fail("Google Cast chooser dialog did not appear within ${DIALOG_TIMEOUT}ms")
        }

        if (scenario == "connected") {
            val deviceName: String = args.getString("deviceName")
                ?: run { fail("deviceName argument is required for connected scenario"); return }
            selectDevice(deviceName, actionWait)
        } else {
            // Discovery: dialog is showing, wait the action wait period
            Thread.sleep(actionWait)
        }

        // Signal ready
        val result = Bundle()
        result.putString("state", scenario)
        result.putString("status", "ready")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result)

        // Keep alive so the state is maintained while script reads meminfo
        Thread.sleep(300_000)
    }

    private fun clickCastIcon() {
        // Find by content description since the cast button has no resource-id
        val castButton = device.wait(
            Until.findObject(By.desc(CAST_BUTTON_DESC)),
            10_000
        )
        if (castButton == null) {
            fail("Cast button not found (content-desc: $CAST_BUTTON_DESC)")
        }
        castButton!!.click()
    }

    private fun selectDevice(deviceName: String, actionWait: Long) {
        // Wait for device list to populate
        device.wait(Until.findObject(By.res(ROUTE_NAME_RES)), DIALOG_TIMEOUT)

        // Find device by case-insensitive name match
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
        Thread.sleep(actionWait)
    }
}
