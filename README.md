# Vizbee Android Demo App

This example demonstrates how to integrate Vizbee casting functionality into an Android mobile app.

## Vizbee Continuity Integration Steps for your Android mobile app
Look for the block comments with text "[BEGIN] Vizbee Integration" and "[END] Vizbee Integration" in the code for an easy understanding of the integration.

### Build Setup
1. Add the Vizbee repository to your Android mobile app’s root [settings.gradle](settings.gradle).
2. Add Vizbee SDK dependency to your app module's [build.gradle](/app/build.gradle).

### AndroidManifest Setup
1. Add Vizbee RemoteActivity to your app's [AndroidManifest.xml](/app/src/main/AndroidManifest.xml).
2. Add `android:usesCleartextTraffic="true"` attribute to application tag. If your app instead uses a custom network security config, add the `cleartextTrafficPermitted` flag to your network security config XML file as specified in `network_security_config.xml` snippet.
3. Create and implement the [CastOptionsProvider](app/src/main/java/tv/vizbee/demo/CastOptionsProvider.kt) and refer it from [AndroidManifest.xml](/app/src/main/AndroidManifest.xml).

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
1. Add miniplayer to all necessary screens to display casting controls to the user while they browse the app during casting [fragment_video_details](app/src/main/res/layout/fragment_video_details.xml)

### Styling
1. The Vizbee SDK components can be customized to match your app's specific theme. Please refer to the below files on how to incorporate them into both light and dark themes.  
   [vizbee.xml(light)](app/src/main/res/values/vizbee.xml)<br>
   [vizbee.xml(night)](app/src/main/res/values-night/vizbee.xml)
2. The Vizbee miniPlayer needs to be styled at the app level. Please refer to the files below for guidance.
   [colors.xml(light)](app/src/main/res/values/colors.xml)<br>
   [colors.xml(night)](app/src/main/res/values-night/colors.xml)

## Documentation
* [Vizbee Android mobile App Developer Guide](https://console.vizbee.tv/app/vzb2000001/develop/guides/android-continuity)
* [Code Snippets](https://console.vizbee.tv/app/vzb2000001/develop/guides/android-snippets)
* [Troubleshooting](https://console.vizbee.tv/app/vzb2000001/develop/guides/sender-troubleshooting-snippets)

## Vizbee HomeSSO Integration Steps for your Android mobile app
The Vizbee Android HomeSSO Demo App showcases the integration of the VizbeeHomeSSO Mobile SDK (a wrapper built on top of the Vizbee Continuity SDK) within an Android mobile app. Look for the block comments with text "[BEGIN] Vizbee Integration" and "[END] Vizbee Integration" in the code for an easy understanding of the integration.

### Build Setup
1. Add the Vizbee repository to your Android mobile app’s root [settings.gradle](settings.gradle).
2. Add Vizbee HomeSSO SDK dependency to your app module's [build.gradle](/app/build.gradle).

### Code Setup
1. Copy [VizbeeHomeSSOAdapter](app/src/main/java/tv/vizbee/demo/vizbee/VizbeeHomeSSOAdapter.kt) class, it serves as an implementation of the `IVizbeeHomeSSOAdapter` interface, providing methods required by the VizbeeHomeSSO SDK during its initialization and operation.

### SDK Initialisation
1. Initialize the VizbeeHomeSSO SDK by initializing `VizbeeHomeSSOManager` via `initialize(application, adapter)` method just after initialising the Vizbee Continuity SDK [VizbeeWrapper](app/src/main/java/tv/vizbee/demo/vizbee/VizbeeWrapper.kt).

### Update Reg Code Status with Mobile Access Token
1. After a successful login on the app, you can update the regCode status with mobile access token for TV login by calling the `VizbeeHomeSSOAdapter().updateRegCodeStatus(authToken)` method with the authToken [UserLoginFragment](app/src/main/java/tv/vizbee/demo/fragments/UserLoginFragment.kt).
