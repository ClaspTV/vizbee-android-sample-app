package tv.vizbee.demo.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.vizbee.api.RequestCallback
import tv.vizbee.api.SmartHandoffCardVisibility
import tv.vizbee.api.SmartHandoffContext
import tv.vizbee.api.SmartHelpOptions
import tv.vizbee.api.VizbeeContext
import tv.vizbee.api.VizbeeRequest
import tv.vizbee.api.VizbeeStatus
import tv.vizbee.api.session.VizbeeScreen
import tv.vizbee.demo.Constants
import tv.vizbee.demo.activity.MoviePlayerActivity
import tv.vizbee.demo.adapter.SmartHandoffRecyclerAdapter
import tv.vizbee.demo.databinding.FragmentSmartHandoffBinding
import tv.vizbee.demo.model.handoffvideo.HandoffVideoItem
import tv.vizbee.demo.model.handoffvideo.HandoffVideoStoreFactory
import tv.vizbee.demo.model.video.VideoItem
import tv.vizbee.demo.model.video.VideoStoreFactory
import tv.vizbee.utils.Logger

class SmartHandoffFragment : BaseFragment() {
    private lateinit var binding: FragmentSmartHandoffBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmartHandoffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= 35) {
            val actionBarHeight = actionBarHeight(context)
            binding.root.setPadding(0, actionBarHeight - 50, 0, 0)
        }

        // Get only the first 3 items from the video store
        val smartHandoffItems = HandoffVideoStoreFactory.getHandoffVideoItems()

        binding.smartHandoffRecyclerView.adapter = SmartHandoffRecyclerAdapter(onItemClick = {
            Logger.d(LOG_TAG, "Smart Handoff item clicked: ${it.title}")
            callVizbeeSmartHelp(it)
        }).apply {
            addAll(smartHandoffItems)
        }
    }

    private fun callVizbeeSmartHelp(item: HandoffVideoItem) {
        Log.d(Companion.LOG_TAG, "callVizbeeSmartHelp called")
        activity?.let {
            val smartHelpOptions = SmartHelpOptions()
            smartHelpOptions.enabledSubflows = SmartHelpOptions.SUBFLOW_SMART_HANDOFF
            smartHelpOptions.smartHandoffContext = SmartHandoffContext(item.configName)
            smartHelpOptions.smartHandoffCardVisibility = SmartHandoffCardVisibility.SmartHandoffCardVisibilityForceShow
            val request = getVizbeeRequest(VideoStoreFactory.mainVideoStoreList[0])
            Handler().postDelayed({
                VizbeeContext.getInstance().smartHelp(smartHelpOptions, request, requireContext())
            }, 1000)
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

    companion object {
        private const val LOG_TAG = "SmartHandoffFragment"
    }
}