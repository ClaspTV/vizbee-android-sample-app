package tv.vizbee.demo.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.ActivitySmartHandoffBinding
import tv.vizbee.demo.fragments.IFragmentController
import tv.vizbee.demo.fragments.SmartHandoffFragment
import tv.vizbee.demo.fragments.UserLoginFragment
import tv.vizbee.demo.fragments.VideoDetailsFragment
import tv.vizbee.demo.fragments.VideoGalleryFragment
import tv.vizbee.demo.model.VideoItem

class SmartHandoffActivity : AppCompatActivity(), IFragmentController {

    private lateinit var binding: ActivitySmartHandoffBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmartHandoffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()

        // Load SmartHandoffFragment if not already loaded
        if (savedInstanceState == null) {
            showSmartHandoffFragment()
        }
    }

    private fun setupActionBar() {
        val actionBar: ActionBar? = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // Show back button
            setDisplayShowHomeEnabled(true)
            title = "SmartHandoff"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button press
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //---
    // IFragmentController methods
    //---

    override fun showVideoGalleryFragment() {
        // Not needed in SmartHandoffActivity
        Log.w(LOG_TAG, "showVideoGalleryFragment called in SmartHandoffActivity")
    }

    override fun showVideoDetailsFragment(videoItem: VideoItem) {
        val tag = VideoDetailsFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.smart_handoff_content_container, VideoDetailsFragment.newInstance(videoItem), tag)
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
                    R.id.smart_handoff_content_container,
                    UserLoginFragment.newInstance(it), tag
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun showSmartHandoffFragment() {
        val tag = SmartHandoffFragment::class.java.simpleName

        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            // Do not add fragment if its already on the stack
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.smart_handoff_content_container, SmartHandoffFragment(), tag)
            .commitAllowingStateLoss()
    }

    override fun popBackStack() {
        supportFragmentManager.popBackStack()
    }

    companion object {
        private const val LOG_TAG = "SmartHandoffActivity"

        /**
         * Create intent to launch SmartHandoffActivity
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, SmartHandoffActivity::class.java)
        }
    }
}