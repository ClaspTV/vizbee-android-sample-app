package tv.vizbee.demo.model

import android.os.Parcel
import android.os.Parcelable

open class Video protected constructor(`in`: Parcel) : Parcelable {
    private var guid: String? = `in`.readString()
    private var title: String? = `in`.readString()
    private var subtitle: String? = `in`.readString()
    private var imageURL: String? = `in`.readString()
    private var videoURL: String? = `in`.readString()
    private var captionsURL: String? = `in`.readString()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(guid)
        dest.writeString(title)
        dest.writeString(subtitle)
        dest.writeString(imageURL)
        dest.writeString(videoURL)
        dest.writeString(captionsURL)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video?> = object : Parcelable.Creator<Video?> {
            override fun createFromParcel(`in`: Parcel): Video {
                return Video(`in`)
            }

            override fun newArray(size: Int): Array<Video?> {
                return arrayOfNulls(size)
            }
        }
    }
}