package tv.vizbee.demo.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import tv.vizbee.demo.fragments.IFragmentController
import tv.vizbee.demo.databinding.VideoListItemBinding
import tv.vizbee.demo.model.VideoItem

class VideosAdapterWithSimpleRows(context: Context, resource: Int, videoItems: List<VideoItem>) :
    BaseVideosAdapter(context, resource, videoItems) {

    private lateinit var binding: VideoListItemBinding
    private var fragmentController: IFragmentController? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = VideoListItemBinding.inflate(inflater, parent, false)
        val rowView = binding.root

        rowView.tag = position
        rowView.setOnClickListener {
            val videoItem = getItem(position)
            fragmentController?.showVideoDetailsFragment(videoItem)
        }

        val item = getItem(position)

        binding.video.loadImage(item?.imageURL)

        binding.text1.text = item?.title
        binding.text2.text = item?.description

        return rowView
    }

    private fun ImageView.loadImage(imageUrl: String?) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.get().load(imageUrl).into(this)
        }
    }

    override fun setFragmentController(fragmentController: IFragmentController) {
        this.fragmentController = fragmentController
    }
}
