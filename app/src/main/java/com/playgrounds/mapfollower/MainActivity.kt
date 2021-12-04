package com.playgrounds.mapfollower

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@Suppress("ReplaceGetOrSet")
class MainActivity : AppCompatActivity() {
    private val locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private val permissionsClearForLocations = CompletableDeferred<Boolean>()

    init {
        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when (PermissionsRequestManager.onReturningPermissionsCall(this)) {
                PermissionsRequestManager.MissingPermissionType.LOCATION ->
                    Toast.makeText(this, R.string.insufficientPermissions, Toast.LENGTH_LONG).show()
                PermissionsRequestManager.MissingPermissionType.BACKGROUND_LOCATION -> requestBackgroundLocation()
                PermissionsRequestManager.MissingPermissionType.NONE -> {
                    permissionsClearForLocations.complete(true)
                }
            }
        }
    }

    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(MainViewModel::class.java)
        val permissions = PermissionsRequestManager.permissionsToRequest(this)
        if (permissions.isNotEmpty()) {
            locationPermissionRequest.launch(permissions.toTypedArray())
        } else {
            permissionsClearForLocations.complete(true)
        }

        lifecycleScope.launch {
            permissionsClearForLocations.await()
            viewModel.setupGeofence()
        }
    }

    private fun requestBackgroundLocation() {
        // Check is not necessary. The permission manager does it already. It's just to shush the lint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
    }

    companion object {
        const val ACTION_SHOW_LOCATIONS = "com.playgrounds.mapfollower.showActions"
    }
}
