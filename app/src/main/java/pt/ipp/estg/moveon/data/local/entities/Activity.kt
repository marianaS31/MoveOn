package pt.ipp.estg.moveon.data.local.entities
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "activity_table")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val activityId: Long = 0,

    // Requisito: Tipo de atividade (Caminhada, Corrida, etc) [cite: 19-22]
    val activityType: String,

    // Tempos para calcular duração
    val startTime: Long,
    val endTime: Long? = null,

    // Requisito: Estatísticas Finais [cite: 34]
    val distanceMeters: Float = 0f,
    val avgSpeed: Float = 0f,
    val elevationGain: Double = 0.0,

    // Requisito: Meteorologia do local [cite: 33]
    val weatherTemp: String? = null, // Ex: "15ºC, Chuva"

    // Requisito: Foto opcional [cite: 35]
    val photoUri: String? = null,

    // Requisito: Público ou Privado [cite: 36]
    val isPublic: Boolean = false,

    // ID remoto para sincronizar com Firebase mais tarde [cite: 54]
    val firebaseId: String? = null
)