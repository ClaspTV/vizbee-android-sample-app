package tv.vizbee.demo.adapter

import android.content.Context
import android.widget.ArrayAdapter
import tv.vizbee.demo.activity.IFragmentController
import tv.vizbee.demo.model.VideoItem

open class BaseVideosAdapter(
    context: Context,
    resource: Int,
    videoItems: List<VideoItem>
) : ArrayAdapter<VideoItem>(context, resource, videoItems) {

    private var fragmentController: IFragmentController? = null

    open fun setFragmentController(fragmentController: IFragmentController) {
        this.fragmentController = fragmentController
    }
}
