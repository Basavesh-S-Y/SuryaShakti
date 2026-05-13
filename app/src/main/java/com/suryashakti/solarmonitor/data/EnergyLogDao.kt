package com.suryashakti.solarmonitor.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EnergyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EnergyLog): Long

    @Update
    suspend fun updateLog(log: EnergyLog)

    @Delete
    suspend fun deleteLog(log: EnergyLog)

    @Query("SELECT * FROM energy_logs ORDER BY dateMillis DESC")
    fun getAllLogs(): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 30")
    fun getLast30Logs(): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs WHERE dateMillis = :dateMillis LIMIT 1")
    suspend fun getLogByDate(dateMillis: Long): EnergyLog?

    @Query("SELECT * FROM energy_logs WHERE dateMillis >= :fromMillis ORDER BY dateMillis ASC")
    fun getLogsFrom(fromMillis: Long): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestLog(): LiveData<EnergyLog?>

    @Query("SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatestLogSync(): EnergyLog?

    @Query("""
        SELECT 
            SUM(generatedKwh) as totalGenerated,
            SUM(consumedKwh) as totalConsumed
        FROM energy_logs 
        WHERE dateMillis >= :fromMillis
    """)
    suspend fun getSummaryFrom(fromMillis: Long): EnergySummary?

    @Query("SELECT COUNT(*) FROM energy_logs")
    suspend fun getLogCount(): Int
}

data class EnergySummary(
    val totalGenerated: Double,
    val totalConsumed: Double
)
