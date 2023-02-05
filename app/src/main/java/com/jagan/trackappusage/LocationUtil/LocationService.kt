package com.jagan.trackappusage.LocationUtil

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationServices
import com.jagan.trackappusage.R
import com.jagan.trackappusage.Util.LastLocation
import com.jagan.trackappusage.Util.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*


class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }



    override fun onCreate() {
        super.onCreate()

        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.applogo)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        locationClient
            .getLocationUpdates(10L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                val gcd = Geocoder(this, Locale.getDefault())
                val addresses: List<Address> = gcd.getFromLocation(lat.toDouble(), long.toDouble(), 1)

                val loc = addresses[0].locality.toString()
                var lastLoc = LastLocation(loc)
                var lastLoc2 = LastLocation("")
                Log.d("DEBUG","Came her LocationClient ")

                val shareObj = SharedViewModel()
                shareObj.getLastLocation(applicationContext, lastLoc,userLastLocation = {
                    Log.d("DEBUG","--> $it $loc")
                    if(it!=loc){
                        lastLoc2 = LastLocation(loc)
                        shareObj.addLastLocation(applicationContext,lastLoc2)
                    }else{
                        Toast.makeText(applicationContext,"Yes it is equals",Toast.LENGTH_SHORT).show()
                    }
                }
                )

                Log.d("DEBUG","Came3 her")


                val updatedNotification = notification.setContentText(
                    if (addresses.isNotEmpty())
                        "Location: ($lat, $long) "+addresses[0].locality.toString()+" "+lastLoc2.location
                    else {
                        "Location: ($lat, $long)"
                    }
                )




                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}