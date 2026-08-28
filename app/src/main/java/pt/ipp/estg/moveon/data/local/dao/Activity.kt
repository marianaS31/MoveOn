package pt.ipp.estg.moveon.data.local.dao

import androidx.room.*
import pt.ipp.estg.moveon.data.local.entities.LocationPointEntity
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

// Classe auxiliar para retorno da relação 1:N
data class ActivityWithPoints(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "activityId",
        entityColumn = "activityOwnerId"
    )
    val points: List<LocationPointEntity>
)

@Dao
interface ActivityDao {

    // Inserir nova corrida e retornar o ID gerado
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    // Atualizar corrida (ex: quando terminas e gravas a distancia final)

    @Query("UPDATE activity_table SET distanceMeters = :distance, totalElevation = :elevation, speed = :speed, endTime = :end, temperature = :temp, weatherDescription = :desc WHERE activityId = :id")
    suspend fun updateActivityStats(id: Long, distance: Float, elevation: Double, speed: Float, end: Long, temp: Double?, desc: String?)


    // Inserir um ponto de GPS (usado pelo Serviço em background)
    @Insert
    suspend fun insertLocationPoint(point: LocationPointEntity)

    // Obter todas as corridas para a lista do histórico [cite: 30, 41]
    @Query("SELECT * FROM activity_table ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    // Obter detalhes completos de uma corrida específica
    @Transaction
    @Query("SELECT * FROM activity_table WHERE activityId = :key")
    suspend fun getActivityWithPoints(key: Long): ActivityWithPoints?

    // Obter estatísticas totais para Leaderboard local
    @Query("SELECT SUM(distanceMeters) FROM activity_table")
    fun getTotalDistance(): Flow<Float?>

    @Query("SELECT * FROM location_points WHERE activityOwnerId = :activityId ORDER BY timestamp ASC")
    fun getActivityPoints(activityId: Long): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM activity_table WHERE activityId = :activityId")
    fun getActivityById(activityId: Long): Flow<ActivityEntity>
}