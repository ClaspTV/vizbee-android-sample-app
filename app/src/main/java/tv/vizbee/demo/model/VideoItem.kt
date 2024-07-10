package tv.vizbee.demo.model

import android.os.Build
import android.os.Parcel
import android.os.Parcelable

class VideoItem() : Parcelable {

    var title: String = ""
    var subTitle: String = ""
    var guid: String = ""
    var isLive: Boolean = false
    var videoURL: String = ""
    var imageURL: String = ""
    var description: String = "N/A"
    var genre: String = "N/A"
    var rating: String = "N/A"
    var duration: Int = -1
    var director: String = ""
    var cast: List<String> = ArrayList()
    var adCuePoints: List<Long> = ArrayList()
    var shouldDelayBeAlternate: Boolean = false
    var delayInSeconds: Long = 0

    constructor(isLive: Boolean) : this() {
        this.isLive = isLive
        if (isLive) {
            // Ad a pre-roll ad for LIVE videos
            this.adCuePoints = arrayListOf(0L)
        } else {
            this.adCuePoints = arrayListOf(0L, 780L, 1250L, 2200L)
        }
    }

    constructor(
        title: String,
        subTitle: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String
    ) : this(isLive) {
        this.title = title
        this.subTitle = subTitle
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
    }

    constructor(
        title: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String
    ) : this(isLive) {
        this.title = title
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(subTitle)
        dest.writeString(guid)
        dest.writeByte((if (isLive) 1 else 0).toByte())
        dest.writeString(videoURL)
        dest.writeString(imageURL)
        dest.writeString(description)
        dest.writeString(genre)
        dest.writeString(rating)
        dest.writeInt(duration)
        dest.writeString(director)
        dest.writeStringList(cast)
        dest.writeList(adCuePoints)
        dest.writeLong(delayInSeconds)
        dest.writeByte((if (shouldDelayBeAlternate) 1 else 0).toByte())
    }

    companion object CREATOR : Parcelable.Creator<VideoItem> {
        override fun createFromParcel(parcel: Parcel): VideoItem {
            return VideoItem(parcel)
        }

        override fun newArray(size: Int): Array<VideoItem?> {
            return arrayOfNulls(size)
        }
    }

    protected constructor(parcel: Parcel) : this() {
        title = parcel.readString()!!
        subTitle = parcel.readString()!!
        guid = parcel.readString()!!
        isLive = parcel.readByte() != 0.toByte()
        videoURL = parcel.readString()!!
        imageURL = parcel.readString()!!
        description = parcel.readString()!!
        genre = parcel.readString()!!
        rating = parcel.readString()!!
        duration = parcel.readInt()
        director = parcel.readString()!!
        cast = parcel.createStringArrayList()!!
        adCuePoints = parcel.readArrayListCompat(Long::class.java.classLoader)
        delayInSeconds = parcel.readLong()
        shouldDelayBeAlternate = parcel.readByte() != 0.toByte()
    }

    private inline fun <reified Long> Parcel.readArrayListCompat(classLoader: ClassLoader?): List<Long> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableListOf<Long>().also { list ->
                readParcelableList(list, classLoader, Long::class.java)
            }
        } else {
            @Suppress("DEPRECATION")
            readArrayList(classLoader) as? List<Long>?: listOf()
        }
    }
}
