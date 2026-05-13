package com.suryashakti.solarmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.data.WeatherCondition
import com.suryashakti.solarmonitor.repository.EnergyRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class EnergyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnergyRepository(application)

    val allLogs = repository.allLogs
    val last30Logs = repository.last30Logs
    val latestLog = repository.latestLog

    private val _saveStatus = MutableLiveData<SaveStatus>()
    val saveStatus: LiveData<SaveStatus> = _saveStatus

    private val _selectedWeather = MutableLiveData(WeatherCondition.SUNNY)
    val selectedWeather: LiveData<WeatherCondition> = _selectedWeather

    private val _simulatedGeneration = MutableLiveData(0.0)
    val simulatedGeneration: LiveData<Double> = _simulatedGeneration

    // 30-day report computed from logs
    val reportStats: LiveData<ReportStats> = last30Logs.map { logs ->
        computeReportStats(logs)
    }

    init {
        viewModelScope.launch {
            repository.seedDemoData()
        }
    }

    fun setWeather(weather: WeatherCondition) {
        _selectedWeather.value = weather
        simulateGeneration(weather)
    }

    fun simulateGeneration(weather: WeatherCondition, capacityKw: Double = 3.0) {
        val generated = EnergyLog.simulateGeneration(weather, capacityKw)
        _simulatedGeneration.value = String.format("%.2f", generated).toDouble()
    }

    fun saveLog(
        dateMillis: Long,
        generatedKwh: Double,
        consumedKwh: Double,
        weather: WeatherCondition,
        perUnitRate: Double,
        exportRate: Double,
        panelCapacity: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val midnight = toMidnight(dateMillis)
                val existing = repository.getLogByDate(midnight)
                val log = EnergyLog(
                    id = existing?.id ?: 0,
                    dateMillis = midnight,
                    generatedKwh = generatedKwh,
                    consumedKwh = consumedKwh,
                    weatherCondition = weather,
                    perUnitRate = perUnitRate,
                    exportRate = exportRate,
                    panelCapacityKw = panelCapacity,
                    notes = notes
                )
                if (existing != null) {
                    repository.updateLog(log)
                    _saveStatus.postValue(SaveStatus.Updated)
                } else {
                    repository.insertLog(log)
                    _saveStatus.postValue(SaveStatus.Saved)
                }
            } catch (e: Exception) {
                _saveStatus.postValue(SaveStatus.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteLog(log: EnergyLog) {
        viewModelScope.launch { repository.deleteLog(log) }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    private fun toMidnight(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun computeReportStats(logs: List<EnergyLog>): ReportStats {
        if (logs.isEmpty()) return ReportStats()
        val totalGen = logs.sumOf { it.generatedKwh }
        val totalCon = logs.sumOf { it.consumedKwh }
        val totalSaved = logs.sumOf { it.moneySavedRupees }
        val totalEarned = logs.sumOf { it.moneyEarnedRupees }
        val totalExport = logs.sumOf { it.exportedKwh }
        val totalImport = logs.sumOf { it.importedKwh }
        val avgScore = logs.map { it.independenceScore }.average().toInt()
        val co2Saved = totalGen * 0.82 // kg CO2 per kWh (India grid factor)
        return ReportStats(
            totalGeneratedKwh = totalGen,
            totalConsumedKwh = totalCon,
            totalMoneySaved = totalSaved,
            totalMoneyEarned = totalEarned,
            totalExportedKwh = totalExport,
            totalImportedKwh = totalImport,
            avgIndependenceScore = avgScore,
            co2SavedKg = co2Saved,
            daysLogged = logs.size
        )
    }
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saved : SaveStatus()
    object Updated : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}

data class ReportStats(
    val totalGeneratedKwh: Double = 0.0,
    val totalConsumedKwh: Double = 0.0,
    val totalMoneySaved: Double = 0.0,
    val totalMoneyEarned: Double = 0.0,
    val totalExportedKwh: Double = 0.0,
    val totalImportedKwh: Double = 0.0,
    val avgIndependenceScore: Int = 0,
    val co2SavedKg: Double = 0.0,
    val daysLogged: Int = 0
) {
    val totalBenefit: Double get() = totalMoneySaved + totalMoneyEarned
    val solarCoveragePercent: Float get() {
        if (totalConsumedKwh <= 0) return 0f
        return (minOf(totalGeneratedKwh, totalConsumedKwh) / totalConsumedKwh * 100).toFloat()
    }
}

class EnergyViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnergyViewModel::class.java)) {
            return EnergyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
