package tv.vizbee.demo

import android.app.Application
import android.util.Log
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.vizbee.VizbeeWrapper

class VizbeeDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("VizbeeDemoApplication", "onCreate")

        // ---------------------------
        // [BEGIN] Vizbee Integration
        // ---------------------------

        VizbeeWrapper.init(application = this)

        // ---------------------------
        // [END] Vizbee Integration
        // ---------------------------

        Log.d("VizbeeDemoApplication", "onCreate: Vizbee init finished")

        // init SharedPreferenceHelper
        SharedPreferenceHelper.init(this)
    }
}