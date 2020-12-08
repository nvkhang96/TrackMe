package com.nvkhang96.trackme

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.nvkhang96.trackme.databinding.FragmentRecordBinding
import com.nvkhang96.trackme.services.LocationService
import com.nvkhang96.trackme.utilities.*
import com.nvkhang96.trackme.viewmodels.RecordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentRecordBinding
    private val recordViewModel: RecordViewModel by navGraphViewModels(R.id.nav_main) {
        defaultViewModelProviderFactory
    }

    private lateinit var map: GoogleMap

    private var locationServiceBound = false
    private var locationService: LocationService? = null
    private lateinit var locationBroadcastReceiver: BroadcastReceiver

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.service
            locationServiceBound = true

            if (locationPermissionApproved() && recordViewModel.isTracking()) {
                locationService?.subscribeToLocationUpdates() ?: Log.d(TAG, "Service Not Bound")
                recordViewModel.startTracking()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationService = null
            locationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            locationService?.myAppPreferences?.set(SharedPreferencesKey.TRACKING_LOCATION, false)
            recordViewModel.clearTrackingState()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentRecordBinding>(
            inflater,
            R.layout.fragment_record,
            container,
            false
        ).apply {
            viewModel = recordViewModel
            lifecycleOwner = viewLifecycleOwner

            recordViewModel.initializeTracking()

            pauseSession.setOnClickListener {
                locationService?.unsubscribeToLocationUpdates() ?: Log.d(TAG, "Service Not Bound")
                recordViewModel.pauseTracking()
            }

            resumeSession.setOnClickListener {
                if (locationPermissionApproved()) {
                    locationService?.subscribeToLocationUpdates() ?: Log.d(TAG, "Service Not Bound")
                    recordViewModel.startTracking()
                } else {
                    requestLocationPermission()
                }
            }

            stopSession.setOnClickListener {
                recordViewModel.stopTracking()
                findNavController().popBackStack()
            }
        }

        subscribeUi()
        return binding.root
    }

    private val endLatLngObserver = Observer<LatLng?> {
        if (it == null || !this::map.isInitialized) {
            return@Observer
        }

        map.animateCamera(CameraUpdateFactory.newLatLng(it))

        if (recordViewModel.route.size <= 1) {
            return@Observer
        }

        val previousLatLngIndex = recordViewModel.route.size - 2
        val previousLatLng = recordViewModel.route[previousLatLngIndex]
        map.addPolyline(PolylineOptions().add(previousLatLng, it).color(Color.RED))
    }

    private fun subscribeUi() {
        recordViewModel.startLatLng.observe(
            viewLifecycleOwner,
            {
                if (it == null || !this::map.isInitialized) {
                    return@observe
                }

                map.addMarker(MarkerOptions().position(it))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM.toFloat()))
            }
        )

        recordViewModel.endLatLng.observeForever(endLatLngObserver)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        locationBroadcastReceiver = LocationBroadcastReceiver()

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            locationBroadcastReceiver,
            IntentFilter(LocationService.ACTION_LOCATION_BROADCAST)
        )
    }

    override fun onStart() {
        super.onStart()

        with(requireActivity()) {
            bindService(
                Intent(this, LocationService::class.java),
                locationServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        if (locationServiceBound) {
            requireActivity().unbindService(locationServiceConnection)
            locationServiceBound = false
        }

        super.onStop()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(
            locationBroadcastReceiver
        )
        locationService?.stopForeground(true)
        locationService?.stopSelf()
        recordViewModel.endLatLng.removeObserver(endLatLngObserver)

        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return

        val checkLocationPermissionResult = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (checkLocationPermissionResult == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private inner class LocationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newLocation = intent.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)

            newLocation?.let {
                recordViewModel.receiveLocationUpdate(it)
            }
        }
    }

    companion object {
        private val TAG = RecordFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
    }
}
