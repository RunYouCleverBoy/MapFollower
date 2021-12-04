package com.playgrounds.mapfollower.misc

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

/**
 * Request manager - encapsulates all the ugly logic of Google permission checking
 */
object PermissionsRequestManager {
    private fun missingPermissions(activity: AppCompatActivity): List<String> {
        val permissionsGross =
            when {
                // On Android R you should not request background with the rest. We are going to address that further on
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION)
                // On Q you must request all of them to show the right dialog
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION)
                // Before Q there was no special background location
                else -> listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            }

        val permissionsToRequest = permissionsGross.filter {
            activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        return permissionsToRequest
    }

    fun permissionsToRequest(activity: AppCompatActivity): List<String> {
        val missingPermissions = missingPermissions(activity)
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // On Android R you should not request background with the rest. We are going to address that further on
            if (missingPermissions.containsJust(ACCESS_BACKGROUND_LOCATION)) {
                listOf(ACCESS_BACKGROUND_LOCATION)
            } else {
                missingPermissions.minus(ACCESS_BACKGROUND_LOCATION)
            }
        } else missingPermissions
    }

    fun onReturningPermissionsCall(activity: AppCompatActivity): MissingPermissionType {
        val missingPermissions = missingPermissions(activity)
        return when {
            missingPermissions.isEmpty() -> MissingPermissionType.NONE
            ACCESS_FINE_LOCATION in missingPermissions -> MissingPermissionType.LOCATION
            ACCESS_BACKGROUND_LOCATION in missingPermissions -> MissingPermissionType.BACKGROUND_LOCATION
            // If we have fine location - We can skip coarse. It's there for Android bureaucratic reasons anyway
            ACCESS_COARSE_LOCATION in missingPermissions -> MissingPermissionType.NONE
            else -> MissingPermissionType.LOCATION
        }
    }

    enum class MissingPermissionType {
        LOCATION, BACKGROUND_LOCATION, NONE
    }

    private fun <T> List<T>.containsJust(item: T) = this == listOf(item)
}
