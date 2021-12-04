package com.playgrounds.mapfollower.history.model

data class HistoryItem(val key: Int, val lat: Double, val lon: Double, val timeStamp: Long, val crossingType: Int)
