import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import tv.vizbee.api.ISmartPlayAdapter
import tv.vizbee.api.ScreenType
import tv.vizbee.api.VideoMetadata
import tv.vizbee.api.VideoStreamInfo
import tv.vizbee.utils.ICommandCallback
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
        // EXAMPLE:
        /*
        val video = appVideoObject as VideoModel
        val metadata = VideoMetadata().apply {
            guid = video.guid
            title = video.title
            subtitle = video.subtitle
            imageURL = video.imageUrl
            isLive = video.isLive
            customMetadata = JSONObject().apply {
                put("userId", "")
            }
        }
        callback.onSuccess(metadata)
        */
        // default
        callback.onFailure(VizbeeError.newError("Not implemented"))
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
        // EXAMPLE:
        /*
        val video = appVideoObject as VideoModel
        val streams = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "hls")
                put("url", video.hlsStreamUrl)
            })
            put(JSONObject().apply {
                put("type", "dash")
                put("url", video.dashStreamUrl)
            })
        }
        val streamInfo = VideoStreamInfo().apply {
            guid = video.guid
            videoURL = video.hlsStreamUrl
            customStreamInfo = JSONObject().apply {
                put("authToken", video.authToken)
                put("streams", streams)
            }
        }
        callback.onSuccess(streamInfo)
        */
        // default
        callback.onFailure(VizbeeError.newError("Not implemented"))
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
        // EXAMPLE:
        /*
        VizbeeWrapper.localPlayerController?.get()?.let {
            val video = appVideoObject as VideoModel
            it.playVideoOnPhone(video, position, autoPlay)
        }
        */
    }
}