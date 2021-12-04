package com.playgrounds.mapfollower

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.playgrounds.mapfollower.model.location.LocationHandler

// Job service is not sufficient, we do need realtime
class MapFollowerService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(1, createNotification())
        if (intent != null) {
            LocationHandler.get(this).onGeofenceEvent(intent)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_SHOW_LOCATIONS)
        val flagUpdateCurrent = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_CLICK_REQUEST_ID, intent, flagUpdateCurrent)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.map_tracker_notification_title))
            .setContentText(getString(R.string.app_tracker_notification_text))
            .setSmallIcon(R.drawable.ic_baseline_map_24)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "FgChannel"
        private const val NOTIFICATION_CLICK_REQUEST_ID = 30
    }

}
