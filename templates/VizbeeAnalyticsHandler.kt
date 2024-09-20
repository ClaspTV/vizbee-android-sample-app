import org.json.JSONObject
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.analytics.VizbeeAnalyticsListener
import tv.vizbee.api.analytics.VizbeeAnalyticsManager
import tv.vizbee.api.session.VideoStatus
import tv.vizbee.api.session.VizbeeScreen

class VizbeeAnalyticsHandler : VizbeeAnalyticsListener {
    init {
        VizbeeContext.getInstance().analyticsManager?.addAnalyticsListener(this)
    }

    // UIFlow events

    /**
     * This handler is invoked by the Vizbee SDK when the
     * Cast Introduction card is shown.
     *
     * - Parameter attrs: A JSONObject of flow attributes
     */
    private fun onCastIntroductionCardShown(attrs: JSONObject?) {
        // TODO: Add your custom analytics handler here
    }

    /**
     * This handler is invoked by the Vizbee SDK when the
     * Smart Install card is shown.
     *
     * - Parameter attrs: A JSONObject of flow attributes
     */
    private fun onSmartInstallCardShown(attrs: JSONObject?) {
        // TODO: Add your custom analytics handler here
    }

    // Screen connection events

    /**
     * This handler is invoked by the Vizbee SDK when the
     * mobile app is connecting to a new screen.
     */
    fun onConnecting() {
        // TODO: Add your custom analytics handler here
    }

    /**
     * This handler is invoked by the Vizbee SDK when the
     * mobile app is connected to a new screen.
     *
     * - Parameter screen: VizbeeScreen to which the mobile is connected
     */
    fun onConnectedToScreen(screen: VizbeeScreen?) {
        // TODO: Add your custom analytics handler here
    }

    /**
     * This handler is invoked by the Vizbee SDK when the
     * mobile app is disconnected from a screen.
     */
    fun onDisconnect() {
        // TODO: Add your custom analytics handler here
    }

    // Video status events

    /**
     * This handler is invoked by the Vizbee SDK when the
     * mobile app casts a new video to the screen.
     *
     * - Parameter vzbVideoStatus: VideoStatus
     */
    fun onVideoStart(vzbVideoStatus: VideoStatus?) {
        // TODO: Add your custom analytics handler here
    }

    // VZBAnalyticsDelegate

    override fun onAnalyticsEvent(
        event: VizbeeAnalyticsManager.VZBAnalyticsEventType?,
        attrs: JSONObject?
    ) {
        when (event) {
            VizbeeAnalyticsManager.VZBAnalyticsEventType.VZBAnalyticsEventTypeCastIntroductionCardShown ->
                onCastIntroductionCardShown(attrs)
            VizbeeAnalyticsManager.VZBAnalyticsEventType.VZBAnalyticsEventTypeSmartInstallCardShown ->
                onSmartInstallCardShown(attrs)
            else -> {}
        }
    }
}