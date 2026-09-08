package pt.ipp.estg.moveon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athlete_alerts")
data class AthleteAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val raceId: String = "",
    val athleteNumber: Int = 0,
    val reporterId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val alertType: String = "Passagem",
    val photoUri: String? = null // Caminho da imagem local
)