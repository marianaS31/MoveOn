package pt.ipp.estg.moveon.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import pt.ipp.estg.moveon.R

class LocationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startService()
            ACTION_STOP -> stopService()
        }
        // Se o sistema matar o serviço, não o recria automaticamente (poupa bateria se não estiver a gravar)
        return START_NOT_STICKY
    }

    private fun startService() {
        // Criar a notificação para o utilizador saber que estamos a gravar
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("A Gravar Atividade")
            .setContentText("A tua localização está a ser registada...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Usa um ícone válido
            .setOngoing(true)
            .build()

        // Iniciar como Foreground Service (dá prioridade máxima ao processo)
        startForeground(1, notification)

        // TODO: Iniciar serviço de gravação de localização
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        // Criar canal de notificações (Obrigatório para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Localização Ativa",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}