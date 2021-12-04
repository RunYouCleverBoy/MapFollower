package com.playgrounds.mapfollower.map.ui

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.playgrounds.mapfollower.R
import com.playgrounds.mapfollower.map.viewmodel.MapFragmentViewModel
import com.playgrounds.mapfollower.misc.MainViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MapsFragment : Fragment() {
    private val mapDeferred = CompletableDeferred<GoogleMap>()
    private lateinit var viewModel: MapFragmentViewModel
    private var accuracyCircle: Circle? = null
    private var historyMarker: Marker? = null
    private val activityViewModel: MainViewModel by activityViewModels()

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        googleMap.isMyLocationEnabled = true
        mapDeferred.complete(googleMap)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("ReplaceGetOrSet")
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
            .get(MapFragmentViewModel::class.java)

        lifecycleScope.launch {
            // From Example snippets. Map Fragment. Map itself is async
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            viewModel.awaitPermissions()
            mapFragment?.getMapAsync(callback)

            val map = getMap()
            launch {
                listenToLocationChanges(map)
            }

            launch {
                listenToClicks(map)
            }
        }
    }

    private suspend fun listenToLocationChanges(map: GoogleMap) {
        viewModel.locationsFlow.collect { report ->
            val location = report.location
            if (historyMarker != null) {
                map.flyTo(location.toLatLng)
            }
            map.drawCircleAt(location, report.geoFenceAccuracyMeters)
        }
    }

    private suspend fun listenToClicks(map: GoogleMap) {
        activityViewModel.selectedEvent.collect { latLng ->
            map.drawHistoryMarkAt(latLng)
            if (latLng != null) {
                map.flyTo(latLng)
            }
        }
    }

    private fun GoogleMap.drawHistoryMarkAt(latLng: LatLng?) {
        historyMarker?.remove()
        historyMarker = null
        if (latLng != null) {
            historyMarker = addMarker(MarkerOptions().position(latLng).title(getString(R.string.traversed_item)))
        }
    }

    private fun GoogleMap.flyTo(latLng: LatLng) {
        animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private suspend fun getMap() = mapDeferred.await()

    private val Location.toLatLng get() = LatLng(latitude, longitude)
    private fun GoogleMap.drawCircleAt(location: Location, geoFenceAccuracyMeters: Double) {
        accuracyCircle?.remove()
        val context = requireContext()
        val circleOptions = CircleOptions().center(location.toLatLng).radius(geoFenceAccuracyMeters)
            .fillColor(ContextCompat.getColor(context, R.color.geofence_body))
            .strokeColor(ContextCompat.getColor(context, R.color.geofence_stroke))
            .strokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics))

        accuracyCircle = addCircle(circleOptions)
    }
}
