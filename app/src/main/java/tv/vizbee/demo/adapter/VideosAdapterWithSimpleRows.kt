package tv.vizbee.demo.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import tv.vizbee.demo.R
import tv.vizbee.demo.activity.IFragmentController
import tv.vizbee.demo.model.VideoItem

class VideosAdapterWithSimpleRows(context: Context, resource: Int, videoItems: List<VideoItem>) :
    BaseVideosAdapter(context, resource, videoItems) {

    private var fragmentController: IFragmentController? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView =
            convertView ?: inflater.inflate(R.layout.video_list_item, parent, false)
        rowView.tag = position

        rowView.setOnClickListener {
            val videoItem = getItem(position)
            fragmentController?.showVideoDetailsFragment(videoItem)
        }

        val item = getItem(position)

        val thumbView: ImageView = rowView.findViewById(R.id.video)
        val imageUrl = item?.imageURL
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.get().load(item?.imageURL).into(thumbView)
        }

        val title: TextView = rowView.findViewById(R.id.text1)
        title.text = item?.title

        val details: TextView = rowView.findViewById(R.id.text2)
        details.text = item?.description

        return rowView
    }

    override fun setFragmentController(fragmentController: IFragmentController) {
        this.fragmentController = fragmentController
    }
}
