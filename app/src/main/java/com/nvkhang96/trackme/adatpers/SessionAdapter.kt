package com.nvkhang96.trackme.adatpers

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import com.nvkhang96.trackme.R
import com.nvkhang96.trackme.data.Session
import com.nvkhang96.trackme.databinding.ListItemSessionBinding

class SessionAdapter : ListAdapter<Session, RecyclerView.ViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ListItemSessionBinding>(
            inflater,
            R.layout.list_item_session,
            parent,
            false
        )

        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val session = getItem(position)

        (holder as SessionViewHolder).bind(session)
    }

    inner class SessionViewHolder(
        private val binding: ListItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root), OnMapReadyCallback {

        private lateinit var map: GoogleMap
        private lateinit var route: String

        init {
            binding.map.onCreate(null)
            binding.map.getMapAsync(this@SessionViewHolder)
        }

        fun bind(item: Session) {
            binding.session = item
            binding.executePendingBindings()

            route = item.route
        }

        override fun onMapReady(googleMap: GoogleMap?) {
            MapsInitializer.initialize(binding.root.context)
            map = googleMap ?: return

            setMapRoute(route)
        }

        private fun setMapRoute(route: String) {
            if (!this::map.isInitialized || !this::route.isInitialized) {
                return
            }

            map.setOnMapClickListener { }

            val listLatLng = PolyUtil.decode(route)
            if (listLatLng.isEmpty()) {
                return
            }

            map.addMarker(MarkerOptions().position(listLatLng.first()))

            val endLocationMarkerIcon =
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
            map.addMarker(
                MarkerOptions().position(listLatLng.last()).icon(endLocationMarkerIcon)
            )

            map.addPolyline(PolylineOptions().addAll(listLatLng).color(Color.RED))

            val bound = LatLngBounds.Builder()
                .include(listLatLng[0])
                .include(listLatLng[listLatLng.size - 1])
                .build()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(bound.center, 15f))
        }
    }
}

private class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
    override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
        return oldItem.sessionId == newItem.sessionId
    }

    override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
        return oldItem == newItem
    }
}
