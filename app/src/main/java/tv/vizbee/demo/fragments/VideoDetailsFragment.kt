package tv.vizbee.demo.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.images.WebImage
import com.squareup.picasso.Picasso
import tv.vizbee.demo.Constants
import tv.vizbee.demo.R
import tv.vizbee.demo.activity.MoviePlayerActivity
import tv.vizbee.demo.databinding.FragmentVideoDetailsBinding
import tv.vizbee.demo.model.VideoItem

class VideoDetailsFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentVideoDetailsBinding
    private lateinit var videoItem: VideoItem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoDetailsBinding.inflate(inflater, container, false)
        videoItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("video", VideoItem::class.java) ?: VideoItem()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("video") ?: VideoItem()
        }

        binding.btnWatchNow.setOnClickListener(this)
        val imageUrl: String = videoItem.imageURL
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.get().load(videoItem.imageURL).into(binding.ivPoster)
        }
        binding.tvTitle.text = videoItem.title

        return binding.root
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_watchNow) {
            onWatchNowClicked()
        }
    }

    private fun onWatchNowClicked() {
        CastContext.getSharedInstance(requireContext(), Runnable::run)
            .addOnSuccessListener { castContext ->
                val castSession = castContext.sessionManager.currentCastSession
                if (castSession?.isConnected == true) {
                    castToReceiver(castSession.remoteMediaClient)
                } else {
                    startLocalPlayback()
                }
            }
            .addOnFailureListener {
                startLocalPlayback()
            }
    }

    private fun castToReceiver(remoteMediaClient: com.google.android.gms.cast.framework.media.RemoteMediaClient?) {
        if (remoteMediaClient == null) return

        val metadata = MediaMetadata(
            if (videoItem.isLive) MediaMetadata.MEDIA_TYPE_GENERIC else MediaMetadata.MEDIA_TYPE_MOVIE
        ).apply {
            putString(MediaMetadata.KEY_TITLE, videoItem.title)
            if (videoItem.imageURL.isNotEmpty()) {
                addImage(WebImage(Uri.parse(videoItem.imageURL)))
            }
        }

        val mediaInfo = MediaInfo.Builder(videoItem.videoURL)
            .setStreamType(
                if (videoItem.isLive) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED
            )
            .setContentType("application/x-mpegURL")
            .setMetadata(metadata)
            .build()

        remoteMediaClient.load(MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build())
        Log.i(LOG_TAG, "* Casting to receiver: ${videoItem.title}")
    }

    private fun startLocalPlayback() {
        Log.i(LOG_TAG, "* Starting local playback " + videoItem.title)
        activity?.startActivity(
            Intent(activity, MoviePlayerActivity::class.java)
                .putExtra(Constants.EXTRA_VIDEO_ITEM, videoItem)
                .putExtra(Constants.EXTRA_VIDEO_URL, videoItem.videoURL)
                .putExtra(Constants.EXTRA_START_POSITION, 0L)
                .putExtra(Constants.EXTRA_AUTO_PLAY, true)
        )
    }

    companion object {
        fun newInstance(video: VideoItem): VideoDetailsFragment {
            val frag = VideoDetailsFragment()
            val args = Bundle()
            args.putParcelable("video", video)
            frag.arguments = args
            return frag
        }
    }
}