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
 * UIAutomator helper for Chromecast memory/CPU/battery benchmarks.
 *
 * Puts the app into discovery or connected state via the standard
 * Google Cast MediaRouteChooserDialog so the shell script can capture
 * metrics externally (dumpsys meminfo, Perfetto trace, etc.).
 *
 * Instrumentation args:
 *   scenario        - "discovery" or "connected"
 *   deviceName      - Chromecast friendly name (required for "connected")
 *   actionWait      - ms to wait after casting starts (default 10000)
 *   settleTime      - ms to wait after launch before actions (default 60000)
 *   measureDuration - ms to hold state for external measurement (default 300000)
 */
@RunWith(AndroidJUnit4::class)
class ChromecastCastSessionsBenchmarkHelper {

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
        private const val DEFAULT_MEASURE_DURATION = 300_000L
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
    fun castSessions() {
        val sessions = args.getString("sessions", "10")!!.toInt()
        val actionWait = args.getString("actionWait", "$DEFAULT_ACTION_WAIT")!!.toLong()
        val settleTime = args.getString("settleTime", "60000")!!.toLong()
        val playTime = args.getString("playTime", "30000")!!.toLong()
        val afterTime = args.getString("afterTime", "45000")!!.toLong()
        val deviceName: String = args.getString("deviceName")
            ?: run { fail("deviceName argument is required"); return }
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PKG)
            ?: run { fail("Could not get launch intent for $PKG"); return }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        Thread.sleep(settleTime)
        for (session in 1..sessions) {
            clickCastIcon()
            device.wait(Until.findObject(By.res(CHOOSER_TITLE_RES)), DIALOG_TIMEOUT)
            selectDevice(deviceName)
            waitForConnection()
            castVideo(actionWait)
            Thread.sleep(playTime)
            stopCasting()
            val done = Bundle()
            done.putString("status", "disconnected")
            done.putString("session", "$session")
            InstrumentationRegistry.getInstrumentation().sendStatus(0, done)
            Thread.sleep(if (session == sessions) afterTime + 60_000 else afterTime)
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
        // Wait for the cast icon to show connected state
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
        // Find the connected cast button — press back if not visible
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

        // Click "STOP CASTING" button (android:id/button1 in MediaRouteControllerDialog)
        val stopButton = device.wait(Until.findObject(By.textContains("STOP CASTING")), UI_TIMEOUT)
            ?: device.findObject(By.textContains("Stop casting"))
            ?: device.findObject(By.textContains("Stop Casting"))
        stopButton?.click()

        // Wait for the stop command to reach the receiver before the test exits
        Thread.sleep(3_000)
    }
}
