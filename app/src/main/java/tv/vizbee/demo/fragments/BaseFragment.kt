package tv.vizbee.demo.fragments

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    val LOG_TAG: String = javaClass.simpleName
    lateinit var mFragmentController: IFragmentController
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mFragmentController = if (context is IFragmentController) {
            context
        } else {
            throw RuntimeException("Must attach to an Activity implementing the IFragmentController interface")
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mFragmentController = if (activity is IFragmentController) {
            activity
        } else {
            throw RuntimeException("Must attach to an Activity implementing the IFragmentController interface")
        }
    }

    fun actionBarHeight(context: Context?): Int {
        if (null == context) {
            Log.w(LOG_TAG, "Cannot get action bar height for null context")
            return 0
        }

        var actionBarHeight = 0

        val typedValue = TypedValue()
        if ((null != context.theme) &&
            context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true) &&
            (null != context.resources)
        ) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                typedValue.data,
                context.resources.displayMetrics
            )
        }

        Log.v(LOG_TAG, "Action bar height is: $actionBarHeight")
        return actionBarHeight
    }
}