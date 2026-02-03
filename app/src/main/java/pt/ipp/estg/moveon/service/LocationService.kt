package pt.ipp.estg.moveon.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import pt.ipp.estg.moveon.R
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.local.entities.LocationPointEntity

class LocationService : Service() {

    // Cliente de GPS da Google
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Base de Dados e Corrotinas
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    // ID da atividade
    private var currentActivityId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Inicializar DB e GPS
        database = AppDatabase.getDatabase(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    saveLocationPoint(location)
                }
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Receber o ID da atividade
                currentActivityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1L)
                if (currentActivityId != -1L) {
                    startForegroundService()
                    startLocationUpdates()
                }
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForegroundService()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // As permissões são pedidas na UI
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L // Intervalo de 4 segundos
        ).setMinUpdateIntervalMillis(2000L).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveLocationPoint(location: Location) {
        // Gravar na DB
        serviceScope.launch {
            val point = LocationPointEntity(
                activityOwnerId = currentActivityId,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                timestamp = System.currentTimeMillis()
            )
            database.activityDao().insertLocationPoint(point)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("MoveOn a Gravar")
            .setContentText("A registar o teu percurso...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "MoveOn GPS",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ACTIVITY_ID = "EXTRA_ACTIVITY_ID"
    }
}