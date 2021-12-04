package com.playgrounds.mapfollower.model.location

import android.content.Context
import com.playgrounds.mapfollower.model.room.HistoryRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Event saver saves all events to persistence
 */
class LocationEventSaver(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val historyRepo = HistoryRepo(context.applicationContext)

    fun store(time: Long, latitude: Double, longitude: Double, type: Int) {
        scope.launch(Dispatchers.IO) {
            historyRepo.save(time, longitude, latitude, type)
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
