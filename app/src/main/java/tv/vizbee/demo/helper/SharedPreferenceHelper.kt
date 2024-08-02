package tv.vizbee.demo.helper

import android.content.Context
import android.content.SharedPreferences

object SharedPreferenceHelper {

    private const val SHARED_PREF_NAME = "vizbee_mobile_demo_app_shared_prefs"
    private lateinit var sharedPreferences: SharedPreferences

    private const val KEY_AUTH_TOKEN = "AUTH_TOKEN"
    private const val KEY_REG_CODE = "REG_CODE"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
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