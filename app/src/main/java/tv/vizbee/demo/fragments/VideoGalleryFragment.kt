package tv.vizbee.demo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.vizbee.api.VizbeeContext
import tv.vizbee.demo.R
import tv.vizbee.demo.adapter.BaseVideosAdapter
import tv.vizbee.demo.adapter.VideosAdapterWithSimpleRows
import tv.vizbee.demo.databinding.FragmentVideoListBinding
import tv.vizbee.demo.model.VideoItem
import tv.vizbee.demo.model.VideoStoreFactory

class VideoGalleryFragment : BaseFragment() {

    private var _binding: FragmentVideoListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoListBinding.inflate(inflater, container, false)
        val videoAdapter: BaseVideosAdapter? = context?.let { getVideoAdapter(it) }
        videoAdapter?.setFragmentController(mFragmentController)
        binding.VideoList.adapter = videoAdapter
        return binding.getRoot()
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

        // ---------------------------
        // [BEGIN] Vizbee Integration
        // ---------------------------

        VizbeeContext.getInstance().smartHelp(requireActivity())

        // ---------------------------
        // [END] Vizbee Integration
        // ---------------------------
    }
}