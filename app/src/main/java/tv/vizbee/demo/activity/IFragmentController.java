package tv.vizbee.demo.activity;

import tv.vizbee.demo.model.VideoItem;

public interface IFragmentController {
    void showVideoGalleryFragment();
    void showVideoDetailsFragment(VideoItem videoItem);
}
