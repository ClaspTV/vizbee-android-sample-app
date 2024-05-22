package tv.vizbee.demo.model

object VideoStoreFactory {

    // get main video store list
    val mainVideoStoreList: List<VideoItem>
        get() = listOf(
            VideoItem(
                "Elephants Dream",
                "Free HLS video",
                "elephants",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://s3.amazonaws.com/vizbee/images/demoapp/elephants_dream.jpg",
                "HLS"
            ),
            VideoItem(
                "Tears of Steel",
                "Thousands of years ago, five African tribes war over a meteorite containing the metal vibranium. One warrior ingests a heart-shaped herb affected by the metal and gains superhuman abilities, becoming the first Black Panther.",
                "tears",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/TearsOfSteel.m3u8",
                "https://www.washingtonpost.com/graphics/2019/entertainment/oscar-nominees-movie-poster-design/img/black-panther-web.jpg",
                "HLS"
            ),
            VideoItem(
                "Akamai Live Stream",
                "In 1947, John Nash arrives at Princeton University as a co-recipient, with Martin Hansen, of the Carnegie Scholarship for Mathematics",
                "akamai-live-stream",
                true,
                "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                "https://images.unsplash.com/photo-1604311795833-25e1d5c128c6?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=927&q=80",
                "Live"
            ),
            VideoItem(
                "Sintel",
                "sintel",
                true,
                "http://peach.themazzone.com/durian/movies/sintel-2048-surround.mp4",
                "https://s3.amazonaws.com/vizbee/images/demoapp/sintel.jpg",
                "MP4"
            )
        )
}