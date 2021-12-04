package com.playgrounds.mapfollower.model.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GeofenceHistoryEntity)

    @Query("SELECT * FROM geofenceevents ORDER BY timeStamp DESC LIMIT 300")
    fun getAll(): Flow<List<GeofenceHistoryEntity>>
}
