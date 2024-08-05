package tv.vizbee.demo.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.vizbee.demo.R
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

class MainActivity : AppCompatActivity(),
    IFragmentController {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showVideoGalleryFragment()
        Logger.d(LOG_TAG, "handleLogin onCreate")
        handleLogin(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            Logger.d(LOG_TAG, "handleLogin onNewIntent")
            handleLogin(it)
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
            .replace(R.id.container, VideoGalleryFragment(), tag)
            .commit()
    }

    override fun showVideoDetailsFragment(videoItem: VideoItem) {
        val tag = VideoDetailsFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, VideoDetailsFragment.newInstance(videoItem), tag)
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
                    R.id.container,
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
            R.id.account -> if (isUserLoggedIn) {
                signOut(authToken)
            } else {
                showUserLoginFragment(false)
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

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}