package tv.vizbee.demo.model.handoffvideo

import androidx.annotation.DrawableRes

/**
 * Data class for handoff video items
 *
 * @param guid Unique identifier for the video
 * @param title Title of the video
 * @param subtitle Subtitle or description of the video
 * @param thumbnailResId Drawable resource ID for the thumbnail image (e.g., R.drawable.nfl)
 */
data class HandoffVideoItem(
    val guid: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val thumbnailResId: Int,
    @DrawableRes val logoResId: Int
)