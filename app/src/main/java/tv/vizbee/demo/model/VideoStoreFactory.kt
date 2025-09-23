package tv.vizbee.demo.model

object VideoStoreFactory {

    // get main video store list
    val mainVideoStoreList: List<VideoItem>
        get() = listOf(
            // Featured Movies
            VideoItem(
                "Interstellar",
                "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
                "7ca0ddb0-b8a9-4df7-8154-aac6ca17e5c7",
                false,
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://image.tmdb.org/t/p/original/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                "Sci-Fi"
            ),
            VideoItem(
                "Inception",
                "Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets is offered a chance to regain his old life as payment for a task considered to be impossible: \"inception\", the implantation of another person's idea into a target's subconscious.",
                "0d17e70f-8000-4ec2-8b16-033999ce1ad1",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://image.tmdb.org/t/p/original/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
                "Sci-Fi/Thriller"
            ),
            VideoItem(
                "Iron Man",
                "After being held captive in an Afghan cave, billionaire engineer Tony Stark creates a unique weaponized suit of armor to fight evil.",
                "b6d10475-226e-4dbf-add4-763faefb655d",
                false,
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://image.tmdb.org/t/p/original/78lPtwv72eTNqFW9COBYI0dWDJa.jpg",
                "Superhero"
            ),

            // Trending Movies
            VideoItem(
                "The Matrix",
                "Set in the 22nd century, The Matrix tells the story of a computer hacker who joins a group of underground insurgents fighting the vast and powerful computers who now rule the earth.",
                "95db9a88-bc9a-44be-aeb0-47a838079c03",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://image.tmdb.org/t/p/original/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
                "Sci-Fi/Action"
            ),
            VideoItem(
                "Joker",
                "During the 1980s, a failed stand-up comedian is driven insane and turns to a life of crime and chaos in Gotham City while becoming an infamous psychopathic crime figure.",
                "60ea4768-d695-4adc-adda-a60963294483",
                false,
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://image.tmdb.org/t/p/original/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg",
                "Crime/Drama"
            ),
            VideoItem(
                "Captain America: Civil War",
                "Following the events of Age of Ultron, the collective governments of the world pass an act designed to regulate all superhuman activity. This polarizes opinion amongst the Avengers, causing two factions to side with Iron Man or Captain America, which causes an epic battle between former allies.",
                "b69c3546-a45c-468f-a627-17b0a7d3a01e",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://image.tmdb.org/t/p/original/rAGiXaUfPzY7CDEyNKUofk3Kw2e.jpg",
                "Superhero/Action"
            ),
            VideoItem(
                "Inglourious Basterds",
                "In Nazi-occupied France during World War II, a group of Jewish-American soldiers known as \"The Basterds\" are chosen specifically to spread fear throughout the Third Reich by scalping and brutally killing Nazis.",
                "a8bc95c7-9bc8-4d0a-a688-e3ce410a5e4b",
                false,
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://image.tmdb.org/t/p/original/7sfbEnaARXDDhKm0CZ7D7uc2sbo.jpg",
                "War/Drama"
            ),
            VideoItem(
                "Captain America: The First Avenger",
                "During World War II, Steve Rogers is a sickly man from Brooklyn who's transformed into super-soldier Captain America to aid in the war effort.",
                "151b14d6-2cf3-4365-b4be-2d96223836c2",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://image.tmdb.org/t/p/original/vSNxAJTlD0r02V9sPYpOjqDZXUK.jpg",
                "Superhero/War"
            ),

            // Popular Movies
            VideoItem(
                "The Lord of the Rings: The Two Towers",
                "Frodo and Sam are trekking to Mordor to destroy the One Ring of Power while Gimli, Legolas and Aragorn search for the orc-captured Merry and Pippin.",
                "1adc8b97-2ebd-4089-bf3f-cbf8b371b08a",
                false,
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://image.tmdb.org/t/p/original/5VTN0pR8gcqV3EPUHHfMGnJYN9L.jpg",
                "Fantasy/Adventure"
            ),
            VideoItem(
                "Mad Max: Fury Road",
                "An apocalyptic story set in the furthest reaches of our planet, in a stark desert landscape where humanity is broken, and most everyone is crazed fighting for the necessities of life.",
                "7e73a4d2-c85e-4840-bdd5-897c060ca914",
                false,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/ElephantsDream.m3u8",
                "https://image.tmdb.org/t/p/original/8tZYtuWezp8JbcsvHYO0O46tFbo.jpg",
                "Action/Post-Apocalyptic"
            )
        )

    // Get videos by playlist
    fun getPlaylistVideos(playlistName: String): List<VideoItem> {
        return when (playlistName) {
            "Featured Movies" -> mainVideoStoreList.filter {
                it.guid in listOf(
                    "7ca0ddb0-b8a9-4df7-8154-aac6ca17e5c7",
                    "0d17e70f-8000-4ec2-8b16-033999ce1ad1",
                    "b6d10475-226e-4dbf-add4-763faefb655d"
                )
            }

            "Trending Movies" -> mainVideoStoreList.filter {
                it.guid in listOf(
                    "95db9a88-bc9a-44be-aeb0-47a838079c03",
                    "60ea4768-d695-4adc-adda-a60963294483",
                    "b69c3546-a45c-468f-a627-17b0a7d3a01e",
                    "a8bc95c7-9bc8-4d0a-a688-e3ce410a5e4b",
                    "151b14d6-2cf3-4365-b4be-2d96223836c2"
                )
            }

            "Popular Movies" -> mainVideoStoreList.filter {
                it.guid in listOf(
                    "7ca0ddb0-b8a9-4df7-8154-aac6ca17e5c7",
                    "0d17e70f-8000-4ec2-8b16-033999ce1ad1",
                    "b6d10475-226e-4dbf-add4-763faefb655d",
                    "1adc8b97-2ebd-4089-bf3f-cbf8b371b08a",
                    "7e73a4d2-c85e-4840-bdd5-897c060ca914"
                )
            }

            else -> mainVideoStoreList
        }
    }

    // Get video by ID
    fun getVideoById(id: String): VideoItem? {
        return mainVideoStoreList.find { it.guid == id }
    }
}