# Vizbee Android Demo App

This example demonstrates how to integrate Vizbee casting functionality into an Android app.

## Integration Steps for your Android app
Look for the block comments with text "[BEGIN] Vizbee Integration" and "[END] Vizbee Integration" in the code for an easy understanding of the integration.

### Build Setup
1. Add the Vizbee repository to your Android app’s root [settings.gradle](settings.gradle).
2. Add Vizbee SDK dependency to your app module's [build.gradle](/app/build.gradle).

### [AndroidManifest Setup](/app/src/main/AndroidManifest.xml)
1. Add Vizbee RemoteActivity to your app's manifest.
2. Add `android:usesCleartextTraffic="true"` attribute to application tag. If your app instead uses a custom network security config, add the `cleartextTrafficPermitted` flag to your network security config XML file as specified in `network_security_config.xml` snippet.
3. Enabling casting to Amazon Fire TV for Android 9.0+ devices by adding `<uses-library android:name="org.apache.http.legacy" android:required="false"/>`
4. Create and implement the [CastOptionsProvider](app/src/main/java/tv/vizbee/demo/CastOptionsProvider.kt) and refer it from [AndroidManifest.xml](/app/src/main/AndroidManifest.xml). 

### Code Setup
1. Copy the files under [vizbee package](app/src/main/java/tv/vizbee/demo/vizbee) to your app under an appropriate package.

### SDK Initialisation
1. In your [application](app/src/main/java/tv/vizbee/demo/VizbeeDemoApplication.kt) class, initialise Vizbee Sender SDK via [VizbeeWrapper](app/src/main/java/tv/vizbee/demo/vizbee/VizbeeWrapper.kt) helper file.

### Add CastIcon
1. Adding Vizbee cast button to Toolbar in an Android [menu_main](app/src/main/res/menu/menu_main.xml)
2. Adding Vizbee cast button to Player in an Android [exo_player_controls](app/src/main/res/layout/exo_player_controls.xml)

### Add SmartHelp
1. Invoke the SmartHelp API only in your app’s main Activity or Fragment . I.e., this API should be invoked just after the main screen or home screen of your app is rendered [VideoGalleryFragment](app/src/main/java/tv/vizbee/demo/fragments/VideoGalleryFragment.kt)

### Implement Smartplay
1. Integrate the SmartPlay API to start casting content to the receivers or play locally on the sender device [VideoDetailsFragment](app/src/main/java/tv/vizbee/demo/fragments/VideoDetailsFragment.kt)

### Add Miniplayer
1. Add miniplayer to all necessary screens to display casting controls to the user while they browse the app during casting [fragment_video_details](app/src/main/res/layout/app/src/main/res/layout/fragment_video_details.xml.xml)

## Documentation
* [Vizbee Android App Overview and Developer Guide](https://console.vizbee.tv/app/vzb2000001/develop/guides/android-continuity)
* [Code Snippets](https://console.vizbee.tv/app/vzb2000001/develop/guides/android-snippets)
* [Troubleshooting](https://console.vizbee.tv/app/vzb2000001/develop/guides/sender-troubleshooting-snippets)