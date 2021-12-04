package com.playgrounds.mapfollower.history.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.playgrounds.mapfollower.history.model.HistoryItem
import com.playgrounds.mapfollower.model.room.HistoryRepo
import kotlinx.coroutines.flow.map

/**
 * View model for history list
 */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository by lazy { HistoryRepo(app) }
    fun getDataFlow() = repository.subscribe().map { rows ->
        rows.map {
            HistoryItem(it.id, it.lat, it.lon, it.timeStamp, it.crossingType)
        }
    }
}
