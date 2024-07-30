package tv.vizbee.demo.helper

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("vizbee_mobile_demo_app_shared_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "AUTH_TOKEN"
        private const val KEY_REG_CODE = "REG_CODE"
    }

    fun saveAuthToken(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_AUTH_TOKEN, value)
        editor.apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, "")
    }

    fun saveRegCode(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_REG_CODE, value)
        editor.apply()
    }

    fun getRegCode(): String? {
        return sharedPreferences.getString(KEY_REG_CODE, "")
    }
}