package tv.vizbee.demo.vizbee

import android.content.Intent
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.vizbee.config.controller.IDUtils
import tv.vizbee.demo.Constants
import tv.vizbee.demo.activity.MainActivity
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.model.RegCodeStatusRequest
import tv.vizbee.demo.network.LoginApiInterface
import tv.vizbee.demo.network.NetworkInstance
import tv.vizbee.homesso.IVizbeeHomeSSOAdapter
import tv.vizbee.homesso.model.VizbeeSignInInfo
import tv.vizbee.homesso.model.VizbeeSignInState
import tv.vizbee.homesso.model.VizbeeTVSignInStatus
import tv.vizbee.utils.ICommandCallback
import tv.vizbee.utils.Logger
import tv.vizbee.utils.appstatemonitor.AppStateMonitor

class VizbeeHomeSSOAdapter : IVizbeeHomeSSOAdapter {

    // ---------------------------
    // [BEGIN] Vizbee Integration
    // ---------------------------

    /**
     * This method lets the HomeSSO SDK know whether the mobile app is already signed in
     * or not.
     * @param callback callback via which the app tells SDK about its sign in status
     */
    override fun getSignInInfo(callback: ICommandCallback<List<VizbeeSignInInfo>>) {

        // Implement this method to verify whether the user has signed in via
        // the given sign-in method or not and invoke callback.onSuccess(isSignedIn)

        val authToken = SharedPreferenceHelper.getAuthToken()
        val isSignedIn = (authToken?.isNotEmpty() == true)
        val mobileSignInInfo =
            VizbeeSignInInfo(Constants.LOGIN_TYPE_MVPD, isSignedIn, JSONObject())
        val signInInfo: List<VizbeeSignInInfo> = listOf(mobileSignInInfo)
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
        if (status.vizbeeSignInState == VizbeeSignInState.SIGN_IN_IN_PROGRESS && status.signInType == Constants.LOGIN_TYPE_MVPD
        ) {

            val regCode = status.customData.optString("regcode")
            if (regCode.isNotEmpty()) {

                SharedPreferenceHelper.saveRegCode(regCode)

                if (SharedPreferenceHelper.getAuthToken()?.isEmpty() == true) {
                    launchLoginScreen()
                } else {
                    updateRegCodeStatus(SharedPreferenceHelper.getAuthToken() ?: "")
                }
            } else {
                Logger.i(LOG_TAG, "onTVSignInStatus received RegCode is empty")
            }
        }

    }

    private fun launchLoginScreen() {

        val intent = Intent(VizbeeWrapper.context?.get(), MainActivity::class.java)
        intent.putExtra(INTENT_LAUNCH_LOGIN_SCREEN, true)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        AppStateMonitor.getInstance().visibleActivity?.startActivity(intent)
    }

    fun updateRegCodeStatus(authToken: String) {
        val loginApiInterface = NetworkInstance.getInstance().create(LoginApiInterface::class.java)
        val regCodeStatusRequest = RegCodeStatusRequest(
            IDUtils.getMyDeviceID()
        )
        val call = loginApiInterface.updateRegCodeStatus(
            SharedPreferenceHelper.getRegCode() ?: "",
            authToken,
            regCodeStatusRequest
        )
        call.enqueue(
            object : Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    if (response.isSuccessful) {
                        Logger.d(LOG_TAG, "Update Reg code successful")
                    }
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Logger.e(LOG_TAG, "Update Reg code failed with error=", t)
                }
            })
    }

    companion object {
        private const val LOG_TAG = "VizbeeHomeSSOAdapter"
        const val INTENT_LAUNCH_LOGIN_SCREEN: String = "INTENT_LAUNCH_LOGIN_SCREEN"
    }

    // ---------------------------
    // [END] Vizbee Integration
    // ---------------------------
}