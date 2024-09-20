import org.json.JSONObject

class VizbeeSigninAdapter {
    /**
     * This adapter method is invoked by the Vizbee SDK on connection to
     * a new device to get user signin info. The returned signin info is sent
     * to the receiver for automatic signin.
     *
     * - Return : A JSONObject with fields
     * {
     *   "userId" : "<required id for user>",
     *   "userLogin" : "<required login for user>",
     *   "userFullName" : "<required fullname for user>", // this is shown in the welcome message on receiver
     *   "accessToken" : "<accessToken for user>", // one of access or refresh token must be included
     *   "refreshToken" : "<refreshToken for user>", // one of access or refresh token must be included
     * }
     */
    fun getSigninInfo(callback: (JSONObject?) -> Unit) {
        // EXAMPLE:
        /*
        AccountManager.session.getRefreshToken()
            .subscribe({ refreshToken: String? ->
                val userInfo = JSONObject().apply {
                    put("userId", AccountManager.user.profileId() ?: "")
                    put("userLogin", AccountManager.user.email ?: "")
                    put("userFullName", AccountManager.user.userName ?: "")
                    put("accessToken", AccountManager.user.sessionToken ?: "")
                    put("refreshToken", refreshToken ?: "")
                }
                callback(userInfo)
            }, { throwable: Throwable? ->
                callback(null)
            })
        */

        // default
        callback(null)
    }
}