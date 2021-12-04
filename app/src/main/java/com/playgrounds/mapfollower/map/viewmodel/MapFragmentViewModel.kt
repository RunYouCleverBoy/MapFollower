package com.playgrounds.mapfollower.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playgrounds.mapfollower.model.location.LocationHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * View model for fragment
 */
class MapFragmentViewModel(app: Application) : AndroidViewModel(app) {
    suspend fun awaitPermissions() {
        LocationHandler.get(getApplication()).awaitLocationsPermissions()
    }

    val locationsFlow = LocationHandler.get(app).locationsFlow.shareIn(viewModelScope, SharingStarted.WhileSubscribed())
}
