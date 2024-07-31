package tv.vizbee.demo.vizbee

import android.app.Application
import android.content.Context
import android.content.Intent
import com.google.android.gms.cast.framework.CastContext
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.session.*
import tv.vizbee.demo.Constants
import tv.vizbee.demo.R
import tv.vizbee.homesso.VizbeeHomeSSOManager
import java.lang.ref.WeakReference

object VizbeeWrapper: SessionStateListener {

    private var isConnected: Boolean = false
    var context: WeakReference<Context>? = null
    private var vizbeeSessionManager: VizbeeSessionManager? = null

    // ------------------
    // MARK: - SDK init
    // -----------------

    fun init(application: Application) {

        // Init Vizbee after castContext setup for lock/notification controls
        CastContext.getSharedInstance(application.applicationContext)
        context  = WeakReference(application.applicationContext)

        /*
         * SDK init
         */
        val appId = application.getString(R.string.vizbee_app_id)
        val appAdapter = VizbeeAppAdapter()
        // Enable Vizbee SDK logging
        VizbeeContext.getInstance().enableVerboseLogging()

        // Initialise Vizbee SDK
        VizbeeContext.getInstance().init(application, appId, appAdapter)

        // Initialise HomeSSO
        VizbeeHomeSSOManager.initialize(application.applicationContext, VizbeeHomeSSOAdapter())

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

        // send cast connected broadcast
        isConnected = true
        val intent = Intent(Constants.INTENT_CAST_CONNECTED_KEY)
        intent.putExtra("isConnected", true)
        context?.get()?.sendBroadcast(intent)
    }

    private fun onDisconnected() {

        if (isConnected) {

            // send cast disconnected broadcast
            isConnected = false
            val intent = Intent(Constants.INTENT_CAST_CONNECTED_KEY)
            intent.putExtra("isConnected", false)
            context?.get()?.sendBroadcast(intent)
        }
    }
}