package pt.ipp.estg.moveon.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityOwnerId"],
            onDelete = ForeignKey.CASCADE // Se apagar a corrida, apaga os pontos
        )
    ],
    indices = [Index("activityOwnerId")]
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true)
    val pointId: Long = 0,
    val activityOwnerId: Long, // Chave estrangeira

    // Requisito: Localização (lat/long) [cite: 30]
    val latitude: Double,
    val longitude: Double,

    // Requisito: Altitude e Velocidade em cada ponto [cite: 31, 32]
    val altitude: Double,
    val speed: Float,

    val timestamp: Long
)