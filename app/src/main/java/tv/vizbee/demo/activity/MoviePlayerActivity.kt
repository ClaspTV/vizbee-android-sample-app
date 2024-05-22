package tv.vizbee.demo.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import tv.vizbee.api.RemoteButton
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.session.VizbeeSessionManager
import tv.vizbee.demo.R
import tv.vizbee.demo.model.VideoItem

const val EXTRA_VIDEO_ITEM = "extra_video_item"
const val EXTRA_VIDEO_URL = "extra_video_url"
const val EXTRA_START_POSITION = "extra_start_position"
const val EXTRA_AUTO_PLAY = "extra_auto_play"

class MoviePlayerActivity : Activity() {

    private lateinit var mExoPlayerView: PlayerView
    private var mVideoItem: VideoItem? = null

    private var sessionManager: VizbeeSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_video_player)
        actionBar?.hide()

        mExoPlayerView = findViewById(R.id.exoPlayerView)

        sessionManager = VizbeeContext.getInstance().sessionManager

        /*val castButton = findViewById<RemoteButton>(R.id.player_remote_button)
        castButton.setDrawableTintColor("#0000FF");*/

       /* castButton?.let { remoteButton ->
            remoteButton.setOnClickListener {
                if (sessionManager?.isConnected == true) {
                    remoteButton.click()
                } else {
                    remoteButton.click(mVideoItem, mExoPlayerView.player?.currentPosition?: 0)
                }
            }
        }*/
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        // Intended to resume playback when activity enters focus from being partially obscured
        // by another Fragment or dialog.
        mExoPlayerView.player?.playWhenReady = true
    }

    override fun onStart() {
        super.onStart()
        checkIntent()
    }

    override fun onPause() {
        super.onPause()
        mExoPlayerView.player?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        // Override intent extras start position and auto play flag
        intent?.putExtra(EXTRA_START_POSITION, mExoPlayerView.player?.currentPosition)
        intent?.putExtra(EXTRA_AUTO_PLAY, mExoPlayerView.player?.playWhenReady)
        mExoPlayerView.player?.release()
    }

    private fun checkIntent() {
        intent?.extras?.let { bundle ->
            if (bundle.containsKey(EXTRA_VIDEO_URL)) {
                val videoItem: VideoItem? = bundle.getParcelable(EXTRA_VIDEO_ITEM)
                val startPosition = (bundle.getLong(EXTRA_START_POSITION).toInt())
                val autoPlay = (bundle.getBoolean(EXTRA_AUTO_PLAY, true))
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

        val videoUri = Uri.parse(video.videoURL)
        val player = ExoPlayer.Builder(this).build()
        mExoPlayerView.player = player
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
}

