package com.playgrounds.mapfollower.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofenceevents")
data class GeofenceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val timeStamp: Long,
    val lat: Double,
    val lon: Double,
    val crossingType: Int
)
