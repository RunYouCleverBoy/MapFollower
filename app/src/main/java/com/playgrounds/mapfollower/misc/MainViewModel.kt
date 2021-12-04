package com.playgrounds.mapfollower.misc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.playgrounds.mapfollower.model.location.LocationHandler
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val locationWrapper = LocationHandler.get(app.applicationContext)
    val selectedEvent = MutableStateFlow<LatLng?>(null)

    suspend fun setupGeofence() {
        locationWrapper.awaitLocationsPermissions()
        locationWrapper.startWatchingLocations()
    }

    fun onLocationsOkay() {
        locationWrapper.reportPermissionsReady()
    }

    @Suppress("unused")
    fun shutdown() {
        locationWrapper.destroy()
    }
}
