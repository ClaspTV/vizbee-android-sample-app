package tv.vizbee.demo.util

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.ShimmerFrameLayout

class GlideUtil {
    companion object {
        fun loadImage(
            imageView: ImageView?,
            url: String?,
            transitionOptions: DrawableTransitionOptions = DrawableTransitionOptions.withCrossFade(),
            shimmerView: ShimmerFrameLayout? = null
        ) {
            startShimmer(shimmerView)
            imageView?.let {
                url?.let {
                    Glide.with(imageView.context)
                        .load(url)
                        .transition(transitionOptions)
                        .listener(object : RequestListener<Drawable> {
                            override fun onResourceReady(
                                res: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                src: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                stopShimmer(shimmerView)
                                return false
                            }

                            override fun onLoadFailed(
                                e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                            ): Boolean {
                                stopShimmer(shimmerView)
                                return false
                            }
                        })
                        .into(imageView)
                } ?: run {
                    stopShimmer(shimmerView)
                }
            } ?: run {
                stopShimmer(shimmerView)
            }
        }

        private fun startShimmer(shimmerView: ShimmerFrameLayout?) {
            shimmerView?.apply {
                startShimmer()
                visibility = View.VISIBLE
            }
        }

        private fun stopShimmer(shimmerView: ShimmerFrameLayout?) {
            shimmerView?.apply {
                stopShimmer()
                visibility = View.GONE
            }
        }
    }
}