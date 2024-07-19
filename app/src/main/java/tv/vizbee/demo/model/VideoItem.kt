package tv.vizbee.demo.model

import android.os.Parcel
import android.os.Parcelable

data class VideoItem(
    var title: String = "",
    var subTitle: String = "",
    var guid: String = "",
    var isLive: Boolean = false,
    var videoURL: String = "",
    var imageURL: String = "",
    var description: String = "N/A",
    var genre: String = "N/A",
    var rating: String = "N/A",
    var duration: Int = -1,
    var director: String = "",
    var cast: List<String> = ArrayList(),
    var adCuePoints: List<Long> = ArrayList(),
    var shouldDelayBeAlternate: Boolean = false,
    var delayInSeconds: Long = 0L
) : Parcelable {

    constructor(isLive: Boolean) : this() {
        if (isLive) {
            this.adCuePoints = arrayListOf(0L)
        } else {
            this.adCuePoints = listOf(0L, 780L, 1250L, 2200L)
        }
    }

    constructor(
        title: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String,
        genre: String,
        rating: String,
        duration: Int,
        director: String,
        cast: List<String>
    ) : this(isLive) {
        this.title = title
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
        this.genre = genre
        this.rating = rating
        this.duration = duration
        this.director = director
        this.cast = cast
    }

    constructor(
        title: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String,
        duration: Int
    ) : this(isLive) {
        this.title = title
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
        this.duration = duration
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

    constructor(
        title: String,
        subTitle: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String,
        delayInSeconds: Long,
        shouldDelayBeAlternate: Boolean
    ) : this(isLive) {
        this.title = title
        this.subTitle = subTitle
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
        this.delayInSeconds = delayInSeconds
        this.shouldDelayBeAlternate = shouldDelayBeAlternate
    }

    constructor(
        title: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String,
        genre: String
    ) : this(isLive) {
        this.title = title
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
        this.genre = genre
    }

    constructor(
        title: String,
        subTitle: String,
        guid: String,
        isLive: Boolean,
        videoURL: String,
        imageURL: String,
        description: String,
        genre: String
    ) : this(isLive) {
        this.title = title
        this.subTitle = subTitle
        this.guid = guid
        this.isLive = isLive
        this.videoURL = videoURL
        this.imageURL = imageURL
        this.description = description
        this.genre = genre
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(subTitle)
        dest.writeString(guid)
        dest.writeByte(if (isLive) 1 else 0)
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
        dest.writeByte(if (shouldDelayBeAlternate) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<VideoItem> {
        override fun createFromParcel(parcel: Parcel): VideoItem {
            return VideoItem(parcel)
        }

        override fun newArray(size: Int): Array<VideoItem?> {
            return arrayOfNulls(size)
        }
    }

    private constructor(parcel: Parcel) : this() {
        title = parcel.readString() ?: ""
        subTitle = parcel.readString() ?: ""
        guid = parcel.readString() ?: ""
        isLive = parcel.readByte() != 0.toByte()
        videoURL = parcel.readString() ?: ""
        imageURL = parcel.readString() ?: ""
        description = parcel.readString() ?: ""
        genre = parcel.readString() ?: ""
        rating = parcel.readString() ?: ""
        duration = parcel.readInt()
        director = parcel.readString() ?: ""
        cast = parcel.createStringArrayList() ?: ArrayList()
        adCuePoints = parcel.readArrayList(Long::class.java.classLoader) as ArrayList<Long>
        delayInSeconds = parcel.readLong()
        shouldDelayBeAlternate = parcel.readByte() != 0.toByte()
    }
}
