package tv.vizbee.demo.fragments

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.vizbee.api.SmartHandoffCardVisibility
import tv.vizbee.api.SmartHandoffContext
import tv.vizbee.api.SmartHelpOptions
import tv.vizbee.api.VizbeeContext
import tv.vizbee.demo.adapter.SmartHandoffRecyclerAdapter
import tv.vizbee.demo.databinding.FragmentSmartHandoffBinding
import tv.vizbee.demo.model.handoffvideo.HandoffVideoItem
import tv.vizbee.demo.model.handoffvideo.HandoffVideoStoreFactory

class SmartHandoffFragment : BaseFragment() {
    private lateinit var binding: FragmentSmartHandoffBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmartHandoffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= 35) {
            val actionBarHeight = actionBarHeight(context)
            binding.root.setPadding(0, actionBarHeight - 50, 0, 0)
        }

        // Get only the first 3 items from the video store
        val smartHandoffItems = HandoffVideoStoreFactory.getHandoffVideoItems()

        binding.smartHandoffRecyclerView.adapter = SmartHandoffRecyclerAdapter(onItemClick = {
            callVizbeeSmartHelp(it)
        }).apply {
            addAll(smartHandoffItems)
        }
    }

    private fun callVizbeeSmartHelp(item: HandoffVideoItem) {
        Log.d(Companion.LOG_TAG, "callVizbeeSmartHelp called")
        activity?.let {
            val smartHelpOptions = SmartHelpOptions()
            smartHelpOptions.enabledSubflows = SmartHelpOptions.SUBFLOW_SMART_HANDOFF
            smartHelpOptions.smartHandoffContext = SmartHandoffContext(item.configName)
            smartHelpOptions.smartHandoffCardVisibility = SmartHandoffCardVisibility.SmartHandoffCardVisibilityForceShow
            Handler().postDelayed({
                VizbeeContext.getInstance().smartHelp(smartHelpOptions, requireContext())
            }, 1000)
        }
    }

    companion object {
        private const val LOG_TAG = "SmartHandoffFragment"
    }
}