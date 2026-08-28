package pt.ipp.estg.moveon.data.local.entities
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "activity_table")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val activityId: Long = 0,
    val activityType: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Float = 0f,
    val speed: Float = 0f,
    val totalElevation: Double = 0.0,
    val photoUri: String? = null,
    val isPublic: Boolean = false,
    val firebaseId: String? = null,
    val temperature: Double? = null,
    val weatherIcon: String? = null,
    val weatherDescription: String? = null
)