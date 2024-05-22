package tv.vizbee.demo.vizbee

import org.json.JSONObject
import tv.vizbee.homesso.IVizbeeHomeSSOAdapter
import tv.vizbee.homesso.model.VizbeeSignInInfo
import tv.vizbee.homesso.model.VizbeeTVSignInStatus
import tv.vizbee.utils.ICommandCallback
import tv.vizbee.utils.Logger

class VizbeeHomeSSOAdapter : IVizbeeHomeSSOAdapter {

    /**
     * This method lets the HomeSSO SDK know whether the mobile app is already signed in
     * or not.
     * @param callback callback via which the app tells SDK about its sign in status
     */
    override fun getSignInInfo(callback: ICommandCallback<List<VizbeeSignInInfo>>) {

        // Implement this method to verify whether the user has signed in via
        // the given sign-in method or not and invoke callback.onSuccess(isSignedIn)

        val mobileSignInInfo =
            VizbeeSignInInfo("MVPD", true, JSONObject())
        var signInInfo: List<VizbeeSignInInfo> = listOf(mobileSignInInfo)
            callback.onSuccess(signInInfo)
    }

    /**
     * This method authenticates the TV using the mobile auth token and the TV reg code.
     * Use case 1: If the user has already signed in on the mobile: it invokes the backend API and
     * passes the reg code along with the mobile auth token to authenticate the TV.
     * Use case 2: If the user hasn't signed in on the mobile: it takes the user to the sign in page
     * and once the sign in is done, it invokes the backend API to authenticate the TV.
     * @param status sign in status of the TV
     */
    override fun onReceiveTVSignInStatus(status: VizbeeTVSignInStatus) {
        Logger.i(
            LOG_TAG,
            "Received sign in status = $status context = ${VizbeeWrapper.context?.get()}"
        )
        /*if (status.vizbeeSignInState == VizbeeSignInState.SIGN_IN_IN_PROGRESS && status.signInType.equals("MVPD")) {

            val appPreferences = SenderApplication.getApplication().preferences
            var regCode = status.customData.optString("regcode")
            if (regCode.isNotEmpty()) {

                appPreferences.setRegCode(regCode)
                if (appPreferences.getAuthToken()?.isEmpty() == true) {
                    launchLoginScreen()
                } else {
                    AccountManager.updateRegCodeStatus(appPreferences.getAuthToken() ?: "");
                }
            } else {
                Logger.i(LOG_TAG, "onTVSignInStatus received RegCode is empty")
            }
        }*/

    }

    companion object {
        private const val LOG_TAG = "VZBApp_VizbeeHomeSSOAdapter"
    }

}