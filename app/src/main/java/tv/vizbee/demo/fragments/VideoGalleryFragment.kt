package tv.vizbee.demo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import tv.vizbee.api.VizbeeContext
import tv.vizbee.demo.R
import tv.vizbee.demo.activity.IFragmentController
import tv.vizbee.demo.adapter.BaseVideosAdapter
import tv.vizbee.demo.adapter.VideosAdapterWithSimpleRows
import tv.vizbee.demo.model.VideoItem
import tv.vizbee.demo.model.VideoStoreFactory

class VideoGalleryFragment : BaseFragment() {
    private var videoListView: ListView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_video_list, container, false)
        videoListView = root.findViewById(R.id.VideoList)
        val videoAdapter: BaseVideosAdapter? = context?.let { getVideoAdapter(it) }
        videoAdapter?.setFragmentController(mFragmentController)
        videoListView?.adapter = videoAdapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun getVideoAdapter(context: Context): BaseVideosAdapter {
        val videoList: List<VideoItem> = VideoStoreFactory.mainVideoStoreList
        return VideosAdapterWithSimpleRows(
            context,
            R.layout.video_list_item, videoList
        )
    }

    override fun onResume() {
        super.onResume()
        VizbeeContext.getInstance().smartHelp(requireContext())
    }
}