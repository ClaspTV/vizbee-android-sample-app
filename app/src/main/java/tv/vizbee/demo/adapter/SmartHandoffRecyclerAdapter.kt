package tv.vizbee.demo.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.demo.myverizonapp.utils.BaseRecyclerAdapter
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.ItemSmartHandoffRecyclerViewBinding
import tv.vizbee.demo.model.handoffvideo.HandoffVideoItem

class SmartHandoffRecyclerAdapter(
    private val onItemClick: ((HandoffVideoItem) -> Unit)? = null
) : BaseRecyclerAdapter<HandoffVideoItem, SmartHandoffRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = getView(parent, R.layout.item_smart_handoff_recycler_view)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemSmartHandoffRecyclerViewBinding.bind(itemView)

        fun bind(item: HandoffVideoItem) {
            // Set title
            binding.itemTitle.text = item.title

            // Set subtitle
            binding.itemSubtitle.text = item.subtitle

            // Load background image from drawable resource
            binding.itemBackgroundImage.setImageResource(item.thumbnailResId)

            // Load logo image from drawable resource
            binding.itemLogoImage.setImageResource(item.logoResId)

            // Handle card click - triggers same callback
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }

            // Handle Watch Now button click - triggers same callback
            binding.btnWatchNow.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}