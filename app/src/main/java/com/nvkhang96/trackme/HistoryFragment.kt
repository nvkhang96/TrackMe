package com.nvkhang96.trackme

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.snackbar.Snackbar
import com.nvkhang96.trackme.adatpers.SessionAdapter
import com.nvkhang96.trackme.databinding.FragmentHistoryBinding
import com.nvkhang96.trackme.utilities.PermissionsRequestCode
import com.nvkhang96.trackme.utilities.locationPermissionApproved
import com.nvkhang96.trackme.utilities.requestLocationPermission
import com.nvkhang96.trackme.viewmodels.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val viewModel: HistoryViewModel by navGraphViewModels(R.id.nav_main) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val sessionAdapter = SessionAdapter()
        binding.sessionList.apply {
            adapter = sessionAdapter
        }

        binding.addSession.setOnClickListener {
            if (locationPermissionApproved()) {
                navigateToRecord()
            } else {
                requestLocationPermission()
            }
        }

        subscribeUi(sessionAdapter)

        binding.sessionList.adapter?.notifyItemInserted(0)
        binding.sessionList.smoothScrollToPosition(0)

        return binding.root
    }

    private fun subscribeUi(adapter: SessionAdapter) {
        viewModel.sessions.observe(viewLifecycleOwner) { result ->
            adapter.submitList(result)
        }
    }

    private fun navigateToRecord() {
        val direction = HistoryFragmentDirections.actionHistoryFragmentToRecordFragment()
        findNavController().navigate(direction)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PermissionsRequestCode.LOCATION -> when {
                grantResults.isEmpty() -> Log.d(TAG, "User interaction was cancelled")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> navigateToRecord()
                else -> {
                    Snackbar.make(
                        binding.root,
                        "Permission was denied, but is needed for core functionality.",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Settings") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    companion object {
        private val TAG = HistoryFragment::class.java.simpleName
    }
}
