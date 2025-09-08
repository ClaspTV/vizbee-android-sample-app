package tv.vizbee.demo.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.vizbee.api.RequestCallback
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.VizbeeRequest
import tv.vizbee.api.VizbeeStatus
import tv.vizbee.api.session.VizbeeScreen
import tv.vizbee.demo.Constants
import tv.vizbee.demo.activity.MoviePlayerActivity
import tv.vizbee.demo.adapter.PlaylistAdapter
import tv.vizbee.demo.databinding.FragmentVideoListBinding
import tv.vizbee.demo.model.VideoItem
import tv.vizbee.demo.model.VideoStoreFactory

class VideoGalleryFragment : BaseFragment() {
    private lateinit var binding: FragmentVideoListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.playlistRecyclerView.adapter = PlaylistAdapter(
            onItemClick = { playlistItem ->
                callVizbeeSmartPlay(playlistItem)
            }
        ).apply {
            submitList(VideoStoreFactory.mainVideoStoreList)
        }
    }

    private fun callVizbeeSmartPlay(playlistItem: VideoItem) {

        activity?.let {
            getVizbeeRequest(playlistItem).let { request ->
                VizbeeContext.getInstance().smartPlay(it, request)
            }
        }
    }

    private fun getVizbeeRequest(videoItem: VideoItem): VizbeeRequest {
        val request = VizbeeRequest(videoItem, videoItem.guid, 0)
        request.setCallback(object : RequestCallback {
            override fun doPlayOnPhone(status: VizbeeStatus) {
                Log.i(LOG_TAG, "Play on phone with status = $status")
                startVideoPlayback(videoItem)
            }

            override fun didPlayOnTV(screen: VizbeeScreen) {
                Log.i(LOG_TAG, "Played on screen = $screen")
            }
        })
        return request
    }

    private fun startVideoPlayback(videoItem: VideoItem) {
        Log.i(LOG_TAG, "* Starting local playback " + videoItem.title)
        activity?.startActivity(
            Intent(activity, MoviePlayerActivity::class.java)
                .putExtra(Constants.EXTRA_VIDEO_ITEM, videoItem)
                .putExtra(Constants.EXTRA_VIDEO_URL, videoItem.videoURL)
                .putExtra(Constants.EXTRA_START_POSITION, 0L)
                .putExtra(Constants.EXTRA_AUTO_PLAY, true)
        )
    }

    override fun onResume() {
        super.onResume()

        // ---------------------------
        // [BEGIN] Vizbee Integration
        // ---------------------------
        Handler(Looper.getMainLooper()).postDelayed({
            val currentActivity = activity
            if (currentActivity != null && !currentActivity.isFinishing) {
                VizbeeContext.getInstance().smartHelp(currentActivity)
            }
        }, 2000)

        // ---------------------------
        // [END] Vizbee Integration
        // ---------------------------
    }
}