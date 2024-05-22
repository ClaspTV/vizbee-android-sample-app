package tv.vizbee.demo.fragments

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import tv.vizbee.demo.activity.IFragmentController

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

}