package com.playgrounds.mapfollower

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

class LocationWrapper private constructor(context: Context, private val configuration: Configuration = Configuration()) {
    data class Configuration(val radius: Double = 300.0)
    data class LocationReport(val location: Location, val geoFenceAccuracyMeters: Double)

    private lateinit var client: FusedLocationProviderClient
    private val mutableLocationsFlow = MutableSharedFlow<Location>(extraBufferCapacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val appContext = context.applicationContext

    val locationsFlow: Flow<LocationReport> = mutableLocationsFlow.map { value: Location -> LocationReport(value, configuration.radius) }

    suspend fun startWatchingLocations(): Boolean {
        client = LocationServices.getFusedLocationProviderClient(appContext)
        val timeout = 10 * DateUtils.SECOND_IN_MILLIS
        val locationPromise = obtainSingleLocationAsync(timeout, client)

        val location = withTimeoutOrNull(timeout) { locationPromise.await() }
        return if (location != null) {
            mutableLocationsFlow.emit(location)
            setGeofence(location)
            true
        } else false
    }

    /**
     * Handles an intent and returns true if consumed the intent and handled it.
     */
    fun onGeofenceEvent(intent: Intent): Boolean {
        with(GeofencingEvent.fromIntent(intent)) {
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    val location = triggeringLocation
                    Log.v(LOC_LOG_TAG, "Exiting Geofence by ${location.latitude}, ${location.longitude}")
                    mutableLocationsFlow.tryEmit(location)
                    setGeofence(location)
                }
                -1 -> return false
            }
        }

        return true
    }

    fun destroy() {
        LocationServices.getGeofencingClient(appContext).removeGeofences(createPendingIntent())
    }

    @Suppress("SameParameterValue", "MissingPermission")
    private fun obtainSingleLocationAsync(timeout: Long, client: FusedLocationProviderClient): CompletableDeferred<Location> {
        val deferred = CompletableDeferred<Location>()
        val request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).setMaxWaitTime(timeout).setNumUpdates(1)

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
        return deferred
    }

    @SuppressLint("MissingPermission")
    private fun setGeofence(around: Location, radius: Float = configuration.radius.toFloat()) {
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_EXIT)
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
        fun get(context: Context) = instance ?: LocationWrapper(context).also { instance = it }
        private var instance: LocationWrapper? = null
    }
}
