package tv.vizbee.demo.activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import tv.vizbee.api.RemoteButton
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.session.SessionState
import tv.vizbee.api.session.SessionStateListener
import tv.vizbee.api.session.VizbeeSessionManager
import tv.vizbee.demo.Constants
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.FragmentVideoPlayerBinding
import tv.vizbee.demo.model.VideoItem

class MoviePlayerActivity : Activity() {

    private lateinit var binding: FragmentVideoPlayerBinding
    private var mVideoItem: VideoItem? = null
    private var sessionManager: VizbeeSessionManager? = null
    private val sessionStateListener = SessionStateListener { newState ->
        if (SessionState.CONNECTED == newState) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.hide()
        sessionManager = VizbeeContext.getInstance().sessionManager

        val castButton = binding.exoPlayerView.findViewById<RemoteButton>(R.id.player_remote_button)
        castButton.setDrawableTintColor("#FFFFFF")
        castButton?.let { remoteButton ->
            remoteButton.setOnClickListener {
                if (sessionManager?.isConnected == true) {
                    remoteButton.click()
                } else {
                    remoteButton.click(mVideoItem, binding.exoPlayerView.player?.currentPosition ?: 0)
                }
            }
        }
        registerReceiver()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        // Intended to resume playback when activity enters focus from being partially obscured
        // by another Fragment or dialog.
        binding.exoPlayerView.player?.playWhenReady = true
        sessionManager?.addSessionStateListener(sessionStateListener)
    }

    override fun onStart() {
        super.onStart()
        checkIntent()
    }

    override fun onPause() {
        super.onPause()
        binding.exoPlayerView.player?.playWhenReady = false
        sessionManager?.removeSessionStateListener(sessionStateListener)
    }

    override fun onStop() {
        super.onStop()
        // Override intent extras start position and auto play flag
        intent?.putExtra(Constants.EXTRA_START_POSITION, binding.exoPlayerView.player?.currentPosition)
        intent?.putExtra(Constants.EXTRA_AUTO_PLAY, binding.exoPlayerView.player?.playWhenReady)
        binding.exoPlayerView.player?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        unRegisterReceiver()
    }

    private fun checkIntent() {
        intent?.extras?.let { bundle ->
            if (bundle.containsKey(Constants.EXTRA_VIDEO_URL)) {
                val videoItem: VideoItem? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable(Constants.EXTRA_VIDEO_ITEM, VideoItem::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable(Constants.EXTRA_VIDEO_ITEM)
                    }
                val startPosition = (bundle.getLong(Constants.EXTRA_START_POSITION).toInt())
                val autoPlay = (bundle.getBoolean(Constants.EXTRA_AUTO_PLAY, true))
                intent.replaceExtras(bundle)

                loadMedia(videoItem, startPosition, autoPlay)
            }
        }
    }

    private fun loadMedia(video: VideoItem?, startPosition: Int, autoPlay: Boolean) {
        if (null == video) {
            AlertDialog.Builder(this)
                .setMessage("Error: Video is null")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
            return
        }

        // Create media source
        mVideoItem = video
        val videoUri = Uri.parse(video.videoURL)
        val player = ExoPlayer.Builder(this).build()
        binding.exoPlayerView.player = player
        // Build the media item.
        val mediaItem = MediaItem.fromUri(videoUri)
        // Set the media item to be played.
        player.setMediaItem(mediaItem)
        // Prepare the player.
        player.prepare()
        // Seek to start position
        player.seekTo(startPosition.toLong())
        // Auto Start the playback.
        player.playWhenReady = autoPlay

    }

    private fun registerReceiver() {
        ContextCompat.registerReceiver(
            this,
            mCastConnectedReceiver,
            IntentFilter(Constants.INTENT_CAST_CONNECTED_KEY),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun unRegisterReceiver() {
        unregisterReceiver(
            mCastConnectedReceiver
        )
    }

    private val mCastConnectedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ((intent.extras)?.getBoolean("isConnected")!!) {
                finish()
            }
        }
    }
}

