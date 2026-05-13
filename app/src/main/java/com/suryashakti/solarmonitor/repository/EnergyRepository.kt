package com.suryashakti.solarmonitor.repository

import android.content.Context
import com.suryashakti.solarmonitor.data.AppDatabase
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.data.EnergySummary
import java.util.Calendar

class EnergyRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).energyLogDao()

    val allLogs = dao.getAllLogs()
    val last30Logs = dao.getLast30Logs()
    val latestLog = dao.getLatestLog()

    suspend fun insertLog(log: EnergyLog): Long = dao.insertLog(log)

    suspend fun updateLog(log: EnergyLog) = dao.updateLog(log)

    suspend fun deleteLog(log: EnergyLog) = dao.deleteLog(log)

    suspend fun getLogByDate(dateMillis: Long): EnergyLog? = dao.getLogByDate(dateMillis)

    fun getLogsFrom(fromMillis: Long) = dao.getLogsFrom(fromMillis)

    suspend fun getSummaryFrom(fromMillis: Long): EnergySummary? = dao.getSummaryFrom(fromMillis)

    suspend fun get30DaySummary(): EnergySummary? {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return dao.getSummaryFrom(cal.timeInMillis)
    }

    suspend fun getLatestLogSync() = dao.getLatestLogSync()

    suspend fun getLogCount() = dao.getLogCount()

    suspend fun seedDemoData() {
        if (dao.getLogCount() > 0) return
        val weatherCycle = listOf(
            com.suryashakti.solarmonitor.data.WeatherCondition.SUNNY,
            com.suryashakti.solarmonitor.data.WeatherCondition.SUNNY,
            com.suryashakti.solarmonitor.data.WeatherCondition.PARTLY_CLOUDY,
            com.suryashakti.solarmonitor.data.WeatherCondition.SUNNY,
            com.suryashakti.solarmonitor.data.WeatherCondition.CLOUDY,
            com.suryashakti.solarmonitor.data.WeatherCondition.SUNNY,
            com.suryashakti.solarmonitor.data.WeatherCondition.PARTLY_CLOUDY
        )
        val cal = Calendar.getInstance()
        for (i in 30 downTo 1) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val weather = weatherCycle[i % weatherCycle.size]
            val gen = EnergyLog.simulateGeneration(weather, 3.0)
            val consumption = 8.0 + Math.random() * 6.0 // 8–14 kWh typical household
            dao.insertLog(
                EnergyLog(
                    dateMillis = cal.timeInMillis,
                    generatedKwh = String.format("%.2f", gen).toDouble(),
                    consumedKwh = String.format("%.2f", consumption).toDouble(),
                    weatherCondition = weather,
                    perUnitRate = 8.0,
                    exportRate = 4.0,
                    panelCapacityKw = 3.0
                )
            )
        }
    }
}
