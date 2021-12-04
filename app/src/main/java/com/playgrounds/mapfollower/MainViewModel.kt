package com.playgrounds.mapfollower

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
