package tv.vizbee.demo.model

import android.os.Parcel
import android.os.Parcelable

open class Video : Parcelable {
    var guid: String?
    var title: String?
    var subtitle: String?
    var imageURL: String?
    var videoURL: String?
    var captionsURL: String?

    constructor(
        guid: String = "", title: String = "", subtitle: String = "", imageURL: String = "",
        videoURL: String = "", captionsURL: String = ""
    ) {
        this.guid = guid
        this.title = title
        this.subtitle = subtitle
        this.imageURL = imageURL
        this.videoURL = videoURL
        this.captionsURL = captionsURL
    }

    protected constructor(`in`: Parcel) {
        guid = `in`.readString()
        title = `in`.readString()
        subtitle = `in`.readString()
        imageURL = `in`.readString()
        videoURL = `in`.readString()
        captionsURL = `in`.readString()
    }

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