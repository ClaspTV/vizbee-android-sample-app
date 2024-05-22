package tv.vizbee.demo.vizbee

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import java.lang.ref.WeakReference

import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.session.*
import tv.vizbee.api.session.VideoClient.VideoStatusListener
import tv.vizbee.demo.R

object VizbeeWrapper: SessionStateListener, VideoStatusListener {

    var INTENT_CAST_CONNECTED_KEY: String = "CAST_CONNECTED"
    private var isConnected: Boolean = false
    var context: WeakReference<Context>? = null
    private var vizbeeSessionManager: VizbeeSessionManager? = null

    // ------------------
    // MARK: - SDK init
    // -----------------

    fun init(application: Application) {

        Log.d("VizbeeWrapper", "init called")
        context  = WeakReference(application.applicationContext)

        /*
         * SDK init
         */
        val appId = application.getString(R.string.vizbee_app_id)
        val appAdapter = VizbeeAppAdapter()
        VizbeeContext.getInstance().enableVerboseLogging()
        VizbeeContext.getInstance().init(application, appId, appAdapter)

        /*
         * Setup session manager
         */
        vizbeeSessionManager = VizbeeContext.getInstance().sessionManager
        vizbeeSessionManager?.addSessionStateListener(this)

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
    }

    private fun onDisconnected() {

        if (isConnected) {

            removeVideoStatusListener()

            // post cast connected notification
            isConnected = false
            val intent = Intent(INTENT_CAST_CONNECTED_KEY)
            intent.putExtra("isConnected", false)
            context?.get()?.sendBroadcast(intent)
        }
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

    }
}