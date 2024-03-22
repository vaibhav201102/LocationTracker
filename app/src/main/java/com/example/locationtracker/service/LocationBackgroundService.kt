package com.example.locationtracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.locationtracker.R
import com.example.locationtracker.db.LocationDatabaseHelper
import com.google.android.gms.location.*

@Suppress("DEPRECATION")
class LocationBackgroundService : Service() {

    //region Variables

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var databaseHelper: LocationDatabaseHelper

    //endregion Variables

    //region Override Methods

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        // Initialize the database helper
        databaseHelper = LocationDatabaseHelper(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
        createLocationCallback()

        // Acquire a wake lock to keep the CPU running even when the screen is off
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationForegroundService:WakeLock")
        wakeLock.acquire()

        startForegroundService()
        requestLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the wake lock when the service is destroyed
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        // Stop receiving location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Close the database connection when the service is destroyed
        databaseHelper.close()

    }

    //endregion Override Methods

    //region Location Methods
    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 10000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    p0.lastLocation?.let { location ->
                        if (isAppInForeground(applicationContext)) {
                            // Application is in the foreground, handle location data as required
                            Log.d(
                                "LocationForegroundService",
                                "Location Update (Foreground): ${location.latitude}, ${location.longitude}"
                            )
                            Log.d("DeviceID", getDeviceId(this@LocationBackgroundService))
                            saveLocationToDatabase(
                                getDeviceId(this@LocationBackgroundService),
                                location.latitude,
                                location.longitude
                            )

                        } else {
                            // Application is in the background or closed, handle location data accordingly
                            Log.d(
                                "LocationForegroundService",
                                "Location Update (Background/Closed): ${location.latitude}, ${location.longitude}"
                            )
                            Log.d("DeviceID", getDeviceId(this@LocationBackgroundService))
                            saveLocationToDatabase(
                                getDeviceId(this@LocationBackgroundService),
                                location.latitude,
                                location.longitude
                            )

                        }

                    }
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    //endregion Location Methods

    //region Notification Methods
    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = createNotificationChannel("location_service", "Location Service")
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    //Checks Application is in foreground state or in Background State
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName
        val appProcesses = activityManager.runningAppProcesses ?: return false

        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) return true
        }
        return false
    }

    //endregion Notification Methods

    //region Database Methods
    private fun saveLocationToDatabase(deviceId: String, latitude: Double, longitude: Double) {
        // Save location data to the database
        databaseHelper.insertLocation(deviceId, latitude, longitude)
    }

    //endregion Database Methods

    //region Device ID Methods
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    //endregion Device ID Methods

}





