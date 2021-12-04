package com.playgrounds.mapfollower.model.room

import android.content.Context
import android.text.format.DateUtils
import kotlinx.coroutines.flow.Flow

class HistoryRepo(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { HistoryDatabase.getInstance(appContext) }
    private val dao by lazy { database.getHistoryDao() }
    private var lastCleared = 0L

    fun subscribe(): Flow<List<GeofenceHistoryEntity>> = database.getHistoryDao().getAll()
    suspend fun save(time: Long, longitude: Double, latitude: Double, type: Int) {
        val entity = GeofenceHistoryEntity(0, time, latitude, longitude, type)
        dao.insert(entity)
        val now = System.currentTimeMillis()
        if (now - lastCleared > DateUtils.HOUR_IN_MILLIS) {
            lastCleared = now
            dao.trimTable(300)
        }
    }
}
