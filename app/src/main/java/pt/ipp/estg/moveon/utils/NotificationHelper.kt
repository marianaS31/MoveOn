package pt.ipp.estg.moveon.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import pt.ipp.estg.moveon.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "moveon_athlete_alerts"
        const val CHANNEL_NAME = "Alertas de Atletas"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações em tempo real sobre passagens de atletas nas provas"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showPassageNotification(raceName: String, athleteNumber: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Passagem Detetada - $raceName")
            .setContentText("O Atleta #$athleteNumber acabou de passar num ponto de controlo!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(athleteNumber, notification)
    }
}