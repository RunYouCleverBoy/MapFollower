package com.playgrounds.mapfollower

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MapsFragment : Fragment() {
    private val mapDeferred = CompletableDeferred<GoogleMap>()
    private lateinit var viewModel: MapFragmentViewModel
    private var accuracyCircle: Circle? = null
    private var currMarker: Marker? = null

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

        // From Example snippets. Map Fragment. Map itself is async
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        lifecycleScope.launch {
            val map = getMap()
            viewModel.locationsFlow.collect { report ->
                val location = report.location
                map.flyTo(location.toLatLng)
                map.drawCircleAt(location, report.geoFenceAccuracyMeters)
            }
        }
    }

    private fun GoogleMap.drawCurrMarkerAt(latLng: LatLng, title: String) {
        currMarker?.remove()
        currMarker = addMarker(MarkerOptions().position(latLng).title(title))
    }

    private fun GoogleMap.flyTo(latLng: LatLng) {
        animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private suspend fun getMap() = mapDeferred.await()

    private val Location.toLatLng get() = LatLng(latitude, longitude)
    private fun GoogleMap.drawCircleAt(location: Location, geoFenceAccuracyMeters: Double) {
        accuracyCircle?.remove()
        val circleOptions = CircleOptions().center(location.toLatLng).radius(geoFenceAccuracyMeters)
            .fillColor(ContextCompat.getColor(requireContext(), R.color.geofence_body))
            .strokeColor(ContextCompat.getColor(requireContext(), R.color.geofence_stroke))
            .strokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics))

        accuracyCircle = addCircle(circleOptions)
    }
}
