package pt.ipp.estg.moveon.data.local.dao

import androidx.room.*
import pt.ipp.estg.moveon.data.local.entities.RaceEntity
import kotlinx.coroutines.flow.Flow
import pt.ipp.estg.moveon.data.local.entities.AthleteAlert

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRace(race: RaceEntity): Long

    @Update
    suspend fun updateRace(race: RaceEntity)

    @Delete
    suspend fun deleteRace(race: RaceEntity)

    @Query("SELECT * FROM race_table WHERE raceId = :raceId")
    fun getRaceById(raceId: Long): Flow<RaceEntity?>

    @Query("SELECT * FROM race_table ORDER BY raceDate ASC")
    fun getAllRaces(): Flow<List<RaceEntity>>

    @Query("SELECT * FROM race_table WHERE creatorEmail = :email ORDER BY raceDate ASC")
    fun getRacesCreatedByUser(email: String): Flow<List<RaceEntity>>

    @Query("SELECT * FROM athlete_alerts WHERE raceId = :raceId ORDER BY timestamp DESC")
    fun getAlertsForRace(raceId: String): Flow<List<AthleteAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AthleteAlert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AthleteAlert>)


}