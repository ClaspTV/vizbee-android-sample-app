package tv.vizbee.demo.fragments;

import tv.vizbee.demo.model.VideoItem;

public interface IFragmentController {
    void showVideoGalleryFragment();
    void showVideoDetailsFragment(VideoItem videoItem);
    void showUserLoginFragment(Boolean isHomeSSOLogin);
    void popBackStack();
}
