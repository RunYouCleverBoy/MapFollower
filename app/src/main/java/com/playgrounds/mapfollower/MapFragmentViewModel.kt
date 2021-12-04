package com.playgrounds.mapfollower

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class MapFragmentViewModel(app: Application) : AndroidViewModel(app) {
    val locationsFlow = LocationWrapper.get(app).locationsFlow.shareIn(viewModelScope, SharingStarted.WhileSubscribed())
}
