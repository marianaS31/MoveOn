package pt.ipp.estg.moveon.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipp.estg.moveon.data.local.dao.ActivityDao
import pt.ipp.estg.moveon.data.local.dao.RaceDao
import pt.ipp.estg.moveon.data.local.entities.LocationPointEntity
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity
import pt.ipp.estg.moveon.data.local.entities.RaceEntity

@Database(entities = [ActivityEntity::class, LocationPointEntity::class, RaceEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun raceDao(): RaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moveon_database"
                )
                    .fallbackToDestructiveMigration()/// Apaga a base de dados e cria outra se o schema mudar
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}