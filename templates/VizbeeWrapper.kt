import android.app.Application
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.lang.ref.WeakReference
import myapp.R
import tv.myapp.vizbee.VizbeeAnalyticsHandler
import tv.myapp.vizbee.VizbeeSigninAdapter
import tv.myapp.vizbee.VizbeeAppAdapter
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.session.*
import tv.vizbee.api.session.VideoClient.VideoStatusListener

interface VizbeeLocalPlayerController {
    fun playVideoOnPhone(appVideoObject: Any?, position: Long, shouldAutoPlay: Boolean)
}

object VizbeeWrapper : SessionStateListener, VideoStatusListener {
    @JvmField var INTENT_CAST_CONNECTED_KEY: String = "CAST_CONNECTED"
    var isConnected: Boolean = false
        private set
    var context: WeakReference<Context>? = null
    private lateinit var vizbeeAnalyticsHandler: VizbeeAnalyticsHandler
    private var vizbeeSessionManager: VizbeeSessionManager? = null
    var localPlayerController: WeakReference<VizbeeLocalPlayerController>? = null

    // ------------------
    // MARK: - SDK init
    // -----------------
    fun init(application: Application) {
        context = WeakReference(application.applicationContext)

        /*
         * SDK init
         */
        val appId = application.getString(R.string.vizbee_app_id)
        val appAdapter = VizbeeAppAdapter()
        VizbeeContext.getInstance().init(application, appId, appAdapter)

        /*
         * Setup session manager
         */
        vizbeeSessionManager = VizbeeContext.getInstance().sessionManager
        vizbeeSessionManager?.addSessionStateListener(this)

        /*
         * Init vizbee analytics
         */
        vizbeeAnalyticsHandler = VizbeeAnalyticsHandler()
    }

    // ------------------
    // MARK: - SignIn
    // ------------------
    private fun doSignIn() {
        val vizbeeSigninAdapter = VizbeeSigninAdapter()
        val currentSession = vizbeeSessionManager?.currentSession ?: return
        vizbeeSigninAdapter.getSigninInfo { authInfo ->
            authInfo?.let {
                currentSession.sendEventWithName(
                    "tv.vizbee.homesign.signin",
                    JSONObject().apply {
                        put("authInfo", authInfo)
                    }
                )
            }
        }
    }

    // ----------------------------
    // MARK: - Session Management
    // ----------------------------
    override fun onSessionStateChanged(newState: Int) {
        when (newState) {
            SessionState.NOT_CONNECTED,
            SessionState.NO_DEVICES_AVAILABLE -> {
                onDisconnected()
            }
            SessionState.CONNECTING -> {
                // analytics handler
                vizbeeAnalyticsHandler.onConnecting()
            }
            SessionState.CONNECTED -> {
                onConnected()
            }
        }
    }

    private fun onConnected() {
        addVideoStatusListener()
        // post cast connected notification
        isConnected = true
        val intent = Intent(INTENT_CAST_CONNECTED_KEY)
        intent.putExtra("isConnected", true)
        context?.get()?.sendBroadcast(intent)
        // analytics handler
        val screen = vizbeeSessionManager?.currentSession?.vizbeeScreen
        vizbeeAnalyticsHandler.onConnectedToScreen(screen)
        // signin handler
        doSignIn()
    }

    private fun onDisconnected() {
        if (isConnected) {
            removeVideoStatusListener()
            // post cast connected notification
            isConnected = false
            val intent = Intent(INTENT_CAST_CONNECTED_KEY)
            intent.putExtra("isConnected", false)
            context?.get()?.sendBroadcast(intent)
            // analytics handler
            vizbeeAnalyticsHandler.onDisconnect()
        }
    }

    fun disconnectSession() {
        vizbeeSessionManager?.disconnectSession()
    }

    // --------------------------------
    // MARK: - Video Status Management
    // --------------------------------
    private fun addVideoStatusListener() {
        vizbeeSessionManager?.currentSession?.videoClient?.addVideoStatusListener(this)
    }

    private fun removeVideoStatusListener() {
        vizbeeSessionManager?.currentSession?.videoClient?.removeVideoStatusListener(this)
    }

    override fun onVideoStatusUpdated(videoStatus: VideoStatus) {
        when (videoStatus.playerState) {
            VideoStatus.PLAYER_STATE_STARTED ->
                vizbeeAnalyticsHandler.onVideoStart(videoStatus)
        }
    }
}