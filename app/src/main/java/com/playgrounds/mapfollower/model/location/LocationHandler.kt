package com.playgrounds.mapfollower.model.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import com.google.android.gms.location.*
import com.playgrounds.mapfollower.MapFollowerService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

/**
 * Handles all that's related to location. Geofenceing, locations, etc.
 */
class LocationHandler private constructor(context: Context, private val configuration: Configuration = Configuration()) {
    data class Configuration(val radius: Double = 300.0)
    data class LocationReport(val location: Location, val geoFenceAccuracyMeters: Double)

    private val appContext = context.applicationContext
    private lateinit var client: FusedLocationProviderClient
    private val mutableLocationsFlow = MutableSharedFlow<Location>(extraBufferCapacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val databaseSaver = LocationEventSaver(appContext)
    private val permissionsClearForLocations = CompletableDeferred<Boolean>()

    val locationsFlow: Flow<LocationReport> = mutableLocationsFlow.map { value: Location -> LocationReport(value, configuration.radius) }

    suspend fun awaitLocationsPermissions() = permissionsClearForLocations.await()
    fun reportPermissionsReady() = permissionsClearForLocations.complete(true)

    suspend fun startWatchingLocations(): Boolean {
        client = LocationServices.getFusedLocationProviderClient(appContext)
        val timeout = 10 * DateUtils.SECOND_IN_MILLIS
        val location = obtainSingleLocation(timeout, client)
        mutableLocationsFlow.tryEmit(location)
        setGeofence(location)

        return true
    }

    /**
     * Handles an intent and returns true if consumed the intent and handled it.
     */
    fun onGeofenceEvent(intent: Intent): Boolean {
        val event = GeofencingEvent.fromIntent(intent)
        when (val type = event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                val location = event.triggeringLocation
                Log.v(LOC_LOG_TAG, "Exiting Geofence by ${location.latitude}, ${location.longitude}")
                mutableLocationsFlow.tryEmit(location)
                databaseSaver.store(location.time, location.latitude, location.longitude, type)
                setGeofence(location)
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                val location = event.triggeringLocation
                Log.v(LOC_LOG_TAG, "Entering Geofence by ${location.latitude}, ${location.longitude}")
                databaseSaver.store(location.time, location.latitude, location.longitude, type)
            }
            -1 -> return false
        }


        return true
    }

    fun destroy() {
        databaseSaver.shutdown()
        LocationServices.getGeofencingClient(appContext).removeGeofences(createPendingIntent())
    }

    @Suppress("SameParameterValue", "MissingPermission")
    private suspend fun obtainSingleLocation(timeout: Long, client: FusedLocationProviderClient): Location {
        val deferred = CompletableDeferred<Location>()
        val request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).setMaxWaitTime(timeout)
            .setFastestInterval(0)
            .setNumUpdates(1)

        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.locations.lastOrNull() ?: return
                mutableLocationsFlow.tryEmit(lastLocation)
                deferred.complete(lastLocation)
            }
        }, Looper.getMainLooper()).addOnFailureListener {
            Log.v(LOC_LOG_TAG, "Failed to get location $it")
        }.addOnCompleteListener {
            client.lastLocation.addOnCompleteListener {
                val location = it.result
                deferred.complete(location)
                mutableLocationsFlow.tryEmit(location)
            }
        }
        return deferred.await()
    }

    @SuppressLint("MissingPermission")
    private fun setGeofence(around: Location, radius: Float = configuration.radius.toFloat()) {
        val request = GeofencingRequest.Builder()
            .addGeofence(
                Geofence.Builder()
                    .setCircularRegion(around.latitude, around.longitude, radius)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setRequestId(GEOFENCE_REQUEST_ID)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            ).build()

        val pendingIntent = createPendingIntent()
        LocationServices.getGeofencingClient(appContext).addGeofences(request, pendingIntent)
    }

    private fun createPendingIntent(): PendingIntent {
        val geoFenceRequestCode = GEOFENCE_ID_CODE
        val intent = Intent(appContext, MapFollowerService::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(appContext, geoFenceRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getService(appContext, geoFenceRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    companion object {
        const val GEOFENCE_ID_CODE = 10
        const val GEOFENCE_REQUEST_ID = "Self geofence"
        const val LOC_LOG_TAG = "Location"

        @Synchronized
        fun get(context: Context) = instance ?: LocationHandler(context).also { instance = it }
        private var instance: LocationHandler? = null
    }
}
