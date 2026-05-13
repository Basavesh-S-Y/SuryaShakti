package com.suryashakti.solarmonitor.data

import android.content.Context
import androidx.room.*

class Converters {
    @TypeConverter
    fun fromWeather(value: String): WeatherCondition =
        WeatherCondition.valueOf(value)

    @TypeConverter
    fun toWeather(weather: WeatherCondition): String = weather.name
}

@Database(entities = [EnergyLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun energyLogDao(): EnergyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "surya_shakti_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
