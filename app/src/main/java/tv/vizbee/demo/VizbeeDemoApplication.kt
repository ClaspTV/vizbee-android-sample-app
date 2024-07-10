package tv.vizbee.demo

import android.app.Application
import tv.vizbee.demo.vizbee.VizbeeWrapper

class VizbeeDemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        VizbeeWrapper.init(application = this)
    }
}