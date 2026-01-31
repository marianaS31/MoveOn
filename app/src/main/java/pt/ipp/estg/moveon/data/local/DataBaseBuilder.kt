package pt.ipp.estg.moveon.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipp.estg.moveon.data.local.dao.ActivityDao
import pt.ipp.estg.moveon.data.local.entities.LocationPointEntity
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity

@Database(entities = [ActivityEntity::class, LocationPointEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}