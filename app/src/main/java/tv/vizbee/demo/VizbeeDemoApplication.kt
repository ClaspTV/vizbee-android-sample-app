package tv.vizbee.demo

import android.app.Application
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.vizbee.VizbeeWrapper

class VizbeeDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ---------------------------
        // [BEGIN] Vizbee Integration
        // ---------------------------

        VizbeeWrapper.init(application = this)

        // ---------------------------
        // [END] Vizbee Integration
        // ---------------------------

        // init SharedPreferenceHelper
        SharedPreferenceHelper.init(this)
    }
}