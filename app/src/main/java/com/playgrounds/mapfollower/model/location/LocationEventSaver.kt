package com.playgrounds.mapfollower.model.location

import android.content.Context
import com.playgrounds.mapfollower.model.room.GeofenceHistoryEntity
import com.playgrounds.mapfollower.model.room.HistoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationEventSaver(appContext: Context) {
    private val database = HistoryDatabase.getInstance(appContext)
    private val scope = CoroutineScope(Dispatchers.Main)

    fun store(time: Long, latitude: Double, longitude: Double, type: Int) {
        val entity = GeofenceHistoryEntity(0, time, latitude, longitude, type)
        scope.launch(Dispatchers.IO) {
            database.getHistoryDao().insert(entity)
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
