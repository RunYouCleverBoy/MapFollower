package com.playgrounds.mapfollower.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class GeofenceHistoryEntity(@PrimaryKey val id: Int, val timeStamp: Long, val lat: Double, val lon: Double, val crossingType: Int)
