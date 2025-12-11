package tv.vizbee.demo.model.handoffvideo

import tv.vizbee.demo.R

class HandoffVideoStoreFactory {
    // 1. create 3 HandoffVideoItem objects with dummy data
    // 2. return the list of HandoffVideoItem objects
    companion object {
        fun getHandoffVideoItems(): List<HandoffVideoItem> {
            return listOf(
                HandoffVideoItem(
                    "1",
                    "NFL Game",
                    "The official source for NFL Game",
                    "nfl",
                    R.drawable.nfl,
                    R.drawable.nfl_logo
                ),
                HandoffVideoItem(
                    "2",
                    "Fox Sports Game",
                    "The official source for Game",
                    "fox_sports",
                    R.drawable.fox_sports,
                    R.drawable.fox_sports_logo
                ),
                HandoffVideoItem(
                    "3",
                    "CBS Game",
                    "The official source Game",
                    "cbs_sports",
                    R.drawable.cbs_sports,
                    R.drawable.cbs_sports_logo
                )
            )
        }
    }
}