package tv.vizbee.demo.vizbee

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONException
import org.json.JSONObject
import tv.vizbee.api.ISmartPlayAdapter
import tv.vizbee.api.ScreenType
import tv.vizbee.api.VideoMetadata
import tv.vizbee.api.VideoStreamInfo
import tv.vizbee.api.VideoTrackInfo
import tv.vizbee.demo.Constants
import tv.vizbee.demo.activity.MoviePlayerActivity
import tv.vizbee.demo.model.VideoItem
import tv.vizbee.utils.ICommandCallback
import tv.vizbee.utils.Logger
import tv.vizbee.utils.VizbeeError

class VizbeeAppAdapter : ISmartPlayAdapter, CoroutineScope by MainScope() {

    /**
     * This adapter method is invoked by the Vizbee SDK to get
     * metadata in the Vizbee format for a given video.
     *
     * - Parameter appVideoObject: The videoObject used in the app
     * - Parameter callback: callback on successful creation of VZBVideoMetadata or callback on failure
     */
    override fun getMetadataFromVideo(appVideoObject: Any, callback: ICommandCallback<VideoMetadata>) {

        val video = appVideoObject as VideoItem
        val metadata = VideoMetadata().apply {
            guid = video.guid
            title = video.title
            subtitle = video.description
            imageURL = video.imageURL
            isLive = appVideoObject.isLive
            customMetadata = JSONObject().apply {
                put("userId", "")
            }
        }
        callback.onSuccess(metadata)
    }

    /**
     * This adapter method is invoked by the Vizbee SDK to get
     * streaming info in the Vizbee format for a given video.
     *
     * - Parameter appVideoObject: The videoObject used in the app
     * - Parameter forScreen: The target screen to which the video is being cast
     * - Parameter callback: callback on successful creation of VZBStreamInfo or callback on failure
     */
    override fun getStreamingInfoFromVideo(appVideoObject: Any, screenType: ScreenType, callback: ICommandCallback<VideoStreamInfo>) {

        Logger.d(LOG_TAG,"getStreamingInfoFromVideo invoked")
        // read data
        val video = appVideoObject as VideoItem

        Logger.d(LOG_TAG,"getStreamingInfoFromVideo video = $video")
        // make stream info
        val streamInfo = VideoStreamInfo().apply {
            guid = video.guid
            videoURL = video.videoURL
            customStreamInfo = JSONObject().apply {
                put("streamURL", video.videoURL)
            }
        }

        Logger.d(LOG_TAG,"Sending streaming info $streamInfo")
        // send stream info
        callback.onSuccess(streamInfo)
    }

    /**
     * This adapter method is invoked by the Vizbee SDK when
     * the mobile app 'joins' a receiver that is already playing a video.
     * The method is used by the Vizbee SDK to get metadata about the
     * video playing on the receiver by using the GUID of the video.
     *
     * - Parameter guid: GUID of the video
     * - Parameter callback: callback on successful creation of VZBVideoInfo or callback on failure
     */
    override fun getVideoInfoByGUID(guid: String, callback: ICommandCallback<Any>) {

        // EXAMPLE:
        /*
        AppNetworking.shared.fetchVideo(guid: String) { video : VideoModel? ->
            video?.let {
               callback.onSuccess(it)
            } ?: run {
               callback.onFailure(VizbeeError.newError("Unable to resolve video details for guid: $guid"))
           }
        }
        */

        // default
        callback.onFailure(VizbeeError.newError("Error resolving video for guid: $guid"))
    }

    /**
     * This adapter method is invoked by the Vizbee SDK in SmartPlay flow
     * or the Disconnect flow to start playback of a video on the phone.
     *
     * - Parameter activityContext: activityContext with which VizbeeSDK invokes SmartPlay
     * - Parameter appVideoObject: app's video object
     * - Parameter atPosition: resume position of video
     * - Parameter autoPlay: indicates if the video should start auto playing
     */
    override fun playOnLocalDevice(activityContext: Context?, appVideoObject: Any, position: Long, autoPlay: Boolean) {

        val myVideo = appVideoObject as VideoItem?
        if (myVideo?.videoURL == null) {
            return
        }

        val i = Intent(activityContext, MoviePlayerActivity::class.java)
            .putExtra(Constants.EXTRA_VIDEO_ITEM, myVideo)
            .putExtra(Constants.EXTRA_VIDEO_URL, myVideo.videoURL)
            .putExtra(Constants.EXTRA_START_POSITION, position)
            .putExtra(Constants.EXTRA_AUTO_PLAY, autoPlay)

        activityContext?.startActivity(i)
    }

    companion object{
        private val LOG_TAG = VizbeeAppAdapter::class.java.simpleName
    }
}