package tv.vizbee.demo.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import tv.vizbee.demo.R
import tv.vizbee.demo.compose.ComposeExamplesActivity
import tv.vizbee.demo.fragments.IFragmentController
import tv.vizbee.demo.fragments.VideoDetailsFragment
import tv.vizbee.demo.fragments.VideoGalleryFragment
import tv.vizbee.demo.model.VideoItem

class MainActivity : AppCompatActivity(),
    IFragmentController {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showVideoGalleryFragment()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // ... existing menu items ...

            R.id.compose_examples -> {
                startActivity(Intent(this, ComposeExamplesActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}