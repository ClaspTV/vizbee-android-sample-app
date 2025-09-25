package tv.vizbee.demo.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.vizbee.api.VizbeeContext
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.ActivityMainBinding
import tv.vizbee.demo.fragments.IFragmentController
import tv.vizbee.demo.fragments.UserLoginFragment
import tv.vizbee.demo.fragments.VideoDetailsFragment
import tv.vizbee.demo.fragments.VideoGalleryFragment
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.model.VideoItem
import tv.vizbee.demo.network.LoginApiInterface
import tv.vizbee.demo.network.NetworkInstance
import tv.vizbee.demo.vizbee.VizbeeHomeSSOAdapter
import tv.vizbee.utils.Logger


class MainActivity : AppCompatActivity(), IFragmentController {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOG_TAG, "onCreate")

        val splashInitTime = System.currentTimeMillis()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            return@setKeepOnScreenCondition if (System.currentTimeMillis() - splashInitTime < 2000) {
                Log.d(LOG_TAG, "Splash In progress")
                true
            } else {
                Log.d(LOG_TAG, "Splash ended")
                init()
                false
            }
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun init() {
        showVideoGalleryFragment()
        Logger.d(LOG_TAG, "handleLogin onCreate")
        handleLogin(intent)

        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false) // Hide title
            actionBar.setDisplayUseLogoEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setLogo(R.drawable.app_logo) // Set your logo
        }

        if (!SharedPreferenceHelper.isInstallationDeeplinkUsed()) {
            connectToStoreAndRetrieve()
        }
        handleIntent(intent)
    }

    private fun connectToStoreAndRetrieve() {
        Log.d(LOG_TAG, "Connecting to store to retrieve referrer information")
        val referrerClient: InstallReferrerClient = InstallReferrerClient.newBuilder(this).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        // Connection established.
                        val response: ReferrerDetails = referrerClient.installReferrer
                        val referrerUrl: String = response.installReferrer
                        val referrerClickTime: Long = response.referrerClickTimestampSeconds
                        val appInstallTime: Long = response.installBeginTimestampSeconds
                        val instantExperienceLaunched: Boolean = response.googlePlayInstantParam

                        // print all the information that can be done using above information
                        Log.i(LOG_TAG, "Referrer URL: $referrerUrl")
                        referrerClient.endConnection()
                        SharedPreferenceHelper.saveInstallationDeeplinkUsed(true)

                        deeplink("?$referrerUrl".toUri())
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        // Connection couldn't be established.
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            Logger.d(LOG_TAG, "handleLogin onNewIntent")
            handleLogin(it)
            handleIntent(it)
        }
    }

    private fun handleLogin(intent: Intent) {
        intent.extras?.let {
            var launchLogin = it.getBoolean(VizbeeHomeSSOAdapter.INTENT_LAUNCH_LOGIN_SCREEN)
            if (launchLogin) {
                Logger.d(LOG_TAG, "LoginFragment Launch Called")
                showUserLoginFragment(true)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        Logger.d(LOG_TAG, "Handling the intent")
        val data: Uri? = intent?.data
        data?.let { uri ->
            Log.d(LOG_TAG, "The intent contains: uri = $uri")
            if (uri.host?.equals("demo.vizbee.tv") == true && uri.path?.equals("/deeplink") == true) {
                uri.getQueryParameter("referrer")?.let {
                    // decode string in case it is encoded
                    val decodedUri = "?${Uri.decode(it)}".toUri()
                    Log.d(LOG_TAG, "Decoded: uri = $decodedUri")
                    deeplink(decodedUri)
                } ?: kotlin.run {
                    deeplink(uri)
                }
            }
        }
    }

    private fun deeplink(uri: Uri) {
        Handler(Looper.getMainLooper()).postDelayed({
            VizbeeContext.getInstance().handleDeeplink(this, uri)
        }, 100)
    }

    //---
    // IFragmentController methods
    //---

    override fun showVideoGalleryFragment() {
        val tag = VideoGalleryFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, VideoGalleryFragment(), tag)
            .commitAllowingStateLoss()
    }

    override fun showVideoDetailsFragment(videoItem: VideoItem) {
        val tag = VideoDetailsFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, VideoDetailsFragment.newInstance(videoItem), tag)
            .addToBackStack(null)
            .commit()
    }

    override fun showUserLoginFragment(isHomeSSOLogin: Boolean?) {
        val tag = UserLoginFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }

        isHomeSSOLogin?.let {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_container,
                    UserLoginFragment.newInstance(it), tag
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun popBackStack() {
        supportFragmentManager.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.account)
        val isUserLoggedIn = (SharedPreferenceHelper.getAuthToken()?.isNotEmpty() == true)
        item.title =
            if (isUserLoggedIn) getString(R.string.sign_out) else getString(R.string.sign_in)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val authToken = SharedPreferenceHelper.getAuthToken()
        val isUserLoggedIn = (authToken?.isNotEmpty() == true)
        when (item.itemId) {
            R.id.account -> {
                if (isUserLoggedIn) {
                    signOut(authToken)
                } else {
                    showUserLoginFragment(false)
                }
            }

            R.id.menu_item_help -> {
                // Take user to a webview with a url
                val url = "https://vizbee.tv/omni-demo"
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun signOut(authToken: String?) {

        authToken?.let {
            val loginApiInterface =
                NetworkInstance.getInstance().create(LoginApiInterface::class.java)

            val call = loginApiInterface.signOut(authToken)
            call.enqueue(
                object : Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {
                        SharedPreferenceHelper.saveAuthToken("")
                        Toast.makeText(baseContext, "Signout success", Toast.LENGTH_LONG).show()
                        invalidateOptionsMenu()
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Logger.e(LOG_TAG, "Signout Failure", t)
                        Toast.makeText(baseContext, "Signout success", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOG_TAG, "* MainActivity onResume")
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}