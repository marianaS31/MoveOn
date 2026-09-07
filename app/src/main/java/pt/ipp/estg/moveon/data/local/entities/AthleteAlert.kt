package pt.ipp.estg.moveon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athlete_alerts")
data class AthleteAlert(
    @PrimaryKey val id: String = "",
    val raceId: String = "",
    val reporterId: String = "",
    val athleteNumber: Int = 0,
    val alertType: String = "PASSAGE", // START, PASSAGE, FINISH
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)