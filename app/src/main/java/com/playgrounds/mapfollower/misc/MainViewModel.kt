package com.playgrounds.mapfollower.misc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playgrounds.mapfollower.model.location.LocationWrapper
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val locationWrapper = LocationWrapper.get(app.applicationContext)

    fun setupGeofence() {
        viewModelScope.launch {
            locationWrapper.startWatchingLocations()
        }
    }

    fun shutdown() {
        locationWrapper.destroy()
    }
}
