package tv.vizbee.demo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import tv.vizbee.demo.databinding.ItemPlaylistBinding
import tv.vizbee.demo.model.VideoItem

class PlaylistAdapter(
    private val onItemClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, PlaylistAdapter.ViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(movie: VideoItem) {
            binding.apply {
                // Load movie poster with Glide
                Glide.with(ivMoviePoster)
                    .load(movie.imageURL)
                    .transform(RoundedCorners(16))
                    .into(ivMoviePoster)

                // Set movie details
                tvMovieTitle.text = movie.title
                tvMovieDescription.text = movie.subTitle
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.guid == newItem.guid
        }

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
}