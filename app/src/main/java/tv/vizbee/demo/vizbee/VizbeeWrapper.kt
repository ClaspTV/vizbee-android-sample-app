package tv.vizbee.demo.vizbee

import android.app.Application
import android.content.Context
import android.content.Intent
import com.google.android.gms.cast.framework.CastContext
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.plugin.*
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
//        VizbeeContext.getInstance().enableVerboseLogging()

        // Initialise Vizbee SDK
        VizbeeContext.getInstance().init(application, appId, appAdapter)

        // Initialise HomeSSO
        VizbeeHomeSSOManager.initialize(application.applicationContext, VizbeeHomeSSOAdapter())

        /*
         * Setup session manager
         */
        vizbeeSessionManager = VizbeeContext.getInstance().sessionManager
        vizbeeSessionManager?.addSessionStateListener(this)

        val pluginManager = VizbeeContext.getInstance().pluginManager

        // Configure rules
        val rules = createPluginRules()
        pluginManager.configure(rules)
    }

    val searchPluginId = "simple_search"
    val coreDeviceCardPlugin = "core_device_card"
    val tvRemotePlugin = "tv_remote_plugin"
    private val contentBrowsingRouteCompanionLayoutActionsDefault = listOf(
        LayoutAction(
            coreDeviceCardPlugin,
            CardPluginViewType.MINI,
            CardPluginPosition.TOP
        ),
        LayoutAction(
            tvRemotePlugin,
            CardPluginViewType.FULL_MINUS_MINI,
            CardPluginPosition.BOTTOM
        )
    )
    private val contentBrowsingRouteCompanionLayoutActionsDeviceExpanded = listOf(
        LayoutAction(
            coreDeviceCardPlugin,
            CardPluginViewType.FULL_MINUS_MINI,
            CardPluginPosition.TOP
        ),
        LayoutAction(
            tvRemotePlugin,
            CardPluginViewType.MINI,
            CardPluginPosition.BOTTOM
        )
    )
    private val searchRouteCompanionLayoutActionsDefault = listOf(
        LayoutAction(
            coreDeviceCardPlugin,
            CardPluginViewType.MINI,
            CardPluginPosition.TOP
        ),
        LayoutAction(
            searchPluginId,
            CardPluginViewType.FULL_MINUS_MINI,
            CardPluginPosition.BOTTOM
        )
    )
    private val searchRouteCompanionLayoutActionsDeviceExpanded = listOf(
        LayoutAction(
            coreDeviceCardPlugin,
            CardPluginViewType.FULL_MINUS_MINI,
            CardPluginPosition.TOP
        ),
        LayoutAction(
            searchPluginId,
            CardPluginViewType.MINI,
            CardPluginPosition.BOTTOM
        )
    )

    private fun createPluginRules(): List<LayoutRule> = listOf(
        // Rule 1: When device core plugin is tapped in full-minus-mini mode, expand it to full (take care of search/tv remote plugin also)
        LayoutRule(
            name = "Device Core Expand",
            trigger = CardPluginEvent.OnViewTapped(coreDeviceCardPlugin),
            condition = { _, layout ->
                layout[coreDeviceCardPlugin] == CardPluginViewType.MINI &&
                        layout[tvRemotePlugin] == CardPluginViewType.FULL_MINUS_MINI
            },
            actions = contentBrowsingRouteCompanionLayoutActionsDeviceExpanded
        ),

        LayoutRule(
            name = "Device Core Expand",
            trigger = CardPluginEvent.OnViewTapped(coreDeviceCardPlugin),
            condition = { _, layout ->
                layout[coreDeviceCardPlugin] == CardPluginViewType.MINI &&
                        layout[searchPluginId] == CardPluginViewType.FULL_MINUS_MINI
            },
            actions = searchRouteCompanionLayoutActionsDeviceExpanded
        ),

        // Rule 2: When search plugin is tapped in mini mode, expand it to full-minus-mini and show device core in mini
        LayoutRule(
            name = "Search Expand - Show Search Full",
            trigger = CardPluginEvent.OnViewTapped(searchPluginId),
            condition = { _, layout ->
                layout[searchPluginId] == CardPluginViewType.MINI
            },
            actions = searchRouteCompanionLayoutActionsDefault
        )

        // Rule 3: When tv remote plugin is tapped in mini mode, expand it to full-minus-mini and show device core in mini
        ,LayoutRule(
            name = "TV Remote Expand - Show TV Remote Full",
            trigger = CardPluginEvent.OnViewTapped(tvRemotePlugin),
            condition = { _, layout ->
                layout[tvRemotePlugin] == CardPluginViewType.MINI
            },
            actions = contentBrowsingRouteCompanionLayoutActionsDefault
        )
    )

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