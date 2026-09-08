package pt.ipp.estg.moveon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "race_table")
data class RaceEntity(
    @PrimaryKey(autoGenerate = true)
    val raceId: Long = 0,
    val raceName: String,
    val raceDescription: String = "",
    val raceType: String, // Corrida, Maratona, Ciclismo
    val raceDate: Long,
    val creatorEmail: String,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val isPublic: Boolean = true,
    val firebaseId: String? = null,
    val routeCoordinates: String? = null // Ex: "41.3667,-8.1944;41.3680,-8.1920;41.3700,-8.1900"
)