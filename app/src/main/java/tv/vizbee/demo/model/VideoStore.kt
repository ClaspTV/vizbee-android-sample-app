package tv.vizbee.demo.model

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch

class VideoStore {
    val videoList: List<VideoItem>

    constructor() {
        videoList = ArrayList()
    }

    constructor(videoItemList: List<VideoItem>) {
        videoList = videoItemList
    }

    fun getVideo(guid: String?): VideoItem? {
        for (vi in videoList) {
            if (vi.guid.equals(guid, ignoreCase = true)) {
                return vi
            }
        }
        return null
    }

    fun getVideoWithDelay(guid: String?, delay: Long): VideoItem? {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            vinfo = getVideo(guid)
            latch.countDown()
        }, delay)
        try {
            latch.await()
        } catch (e: InterruptedException) {
            vinfo = getVideo(guid)
        }
        return vinfo
    }

    companion object {
        private var vinfo: VideoItem? = null
    }
}