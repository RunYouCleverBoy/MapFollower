package com.playgrounds.mapfollower.history.model

/**
 * Item for history list
 */
data class HistoryItem(val key: Int, val lat: Double, val lon: Double, val timeStamp: Long, val crossingType: Int)
