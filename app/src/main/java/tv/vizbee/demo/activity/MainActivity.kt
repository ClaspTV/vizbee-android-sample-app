package tv.vizbee.demo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import tv.vizbee.demo.R
import tv.vizbee.demo.fragments.VideoDetailsFragment
import tv.vizbee.demo.fragments.VideoGalleryFragment
import tv.vizbee.demo.model.VideoItem

class MainActivity : AppCompatActivity(), IFragmentController {
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

}