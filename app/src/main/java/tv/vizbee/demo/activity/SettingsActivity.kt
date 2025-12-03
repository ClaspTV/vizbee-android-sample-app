package tv.vizbee.demo.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import tv.vizbee.demo.databinding.ActivitySettingsBinding
import tv.vizbee.demo.util.UIUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupClickListeners()

        if (Build.VERSION.SDK_INT >= 35) {
            val actionBarHeight = UIUtils.actionBarHeight(this)
            binding.root.setPadding(0, actionBarHeight - 50, 0, 0)
        }
    }

    private fun setupActionBar() {
        val actionBar: ActionBar? = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // Show back button
            setDisplayShowHomeEnabled(true)
            title = "Settings"
        }
    }

    /**
     * Modern approach: Handle WindowInsets to avoid content behind system bars
     * This ensures content doesn't go behind the ActionBar or status bar
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.i(LOG_TAG, "Applying window insets: $insets")

            // Apply padding to the root view to account for system bars
            // top = status bar + action bar area
            // bottom = navigation bar
            view.updatePadding(
                top = insets.top,
                bottom = insets.bottom
            )

            // Return CONSUMED to indicate we've handled the insets
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupClickListeners() {
        // Smart Handoff feature click listener
        binding.smartHandoffContainer.setOnClickListener {
            startActivity(SmartHandoffActivity.createIntent(this))
            finish()
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

    companion object {
        private const val LOG_TAG = "SettingsActivity"

        /**
         * Create intent to launch SettingsActivity
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}