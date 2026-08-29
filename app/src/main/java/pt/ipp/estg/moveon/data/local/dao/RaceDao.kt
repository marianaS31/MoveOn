package pt.ipp.estg.moveon.data.local.dao

import androidx.room.*
import pt.ipp.estg.moveon.data.local.entities.RaceEntity
import kotlinx.coroutines.flow.Flow

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


}