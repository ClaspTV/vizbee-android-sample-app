package tv.vizbee.demo

import android.app.Application
import androidx.tracing.Trace
import com.google.android.gms.cast.framework.CastContext
import tv.vizbee.demo.helper.SharedPreferenceHelper

class VizbeeDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ---------------------------
        // [BEGIN] Google Cast Integration
        // ---------------------------

        Trace.beginSection("GoogleCast.init")
        try {
            CastContext.getSharedInstance(this)
        } finally {
            Trace.endSection()
        }

        // ---------------------------
        // [END] Google Cast Integration
        // ---------------------------

        // init SharedPreferenceHelper
        SharedPreferenceHelper.init(this)
    }
}