package com.playgrounds.mapfollower.model.room

import android.content.Context
import kotlinx.coroutines.flow.Flow

class HistoryRepo(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { HistoryDatabase.getInstance(appContext) }

    fun subscribe(): Flow<List<GeofenceHistoryEntity>> = database.getHistoryDao().getAll()
}
