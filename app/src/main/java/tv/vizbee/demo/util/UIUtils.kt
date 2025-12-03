package tv.vizbee.demo.util

import android.content.Context
import android.util.Log
import android.util.TypedValue

object UIUtils {
    private const val LOG_TAG = "UIUtils"

    fun actionBarHeight(context: Context?): Int {
        if (context == null) {
            Log.w(LOG_TAG, "Cannot get action bar height for null context")
            return 0
        }

        val typedValue = TypedValue()
        return if (context.theme?.resolveAttribute(android.R.attr.actionBarSize, typedValue, true) == true) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, context.resources.displayMetrics).also {
                Log.v(LOG_TAG, "Action bar height is: $it")
            }
        } else {
            0
        }
    }
}