package com.suryashakti.solarmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WeatherCondition(val label: String, val generationFactor: Float) {
    SUNNY("☀️ Sunny", 1.0f),
    PARTLY_CLOUDY("⛅ Partly Cloudy", 0.65f),
    CLOUDY("☁️ Cloudy", 0.30f),
    RAINY("🌧️ Rainy", 0.10f)
}

@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateMillis: Long,                        // Date as epoch millis (midnight)
    val generatedKwh: Double,                    // Solar energy generated
    val consumedKwh: Double,                     // Total energy consumed
    val weatherCondition: WeatherCondition,
    val perUnitRate: Double = 8.0,              // ₹ per kWh (grid rate)
    val exportRate: Double = 4.0,               // ₹ per kWh (exported to grid)
    val panelCapacityKw: Double = 3.0,          // Installed panel capacity
    val notes: String = ""
) {
    /** Net energy: positive = export to grid, negative = import from grid */
    val netEnergyKwh: Double get() = generatedKwh - consumedKwh

    /** kWh saved from grid (solar covering consumption) */
    val savedFromGridKwh: Double get() = minOf(generatedKwh, consumedKwh)

    /** kWh exported to grid (over-generation) */
    val exportedKwh: Double get() = maxOf(0.0, netEnergyKwh)

    /** kWh imported from grid (under-generation) */
    val importedKwh: Double get() = maxOf(0.0, -netEnergyKwh)

    /** Money saved by using solar instead of grid */
    val moneySavedRupees: Double get() = savedFromGridKwh * perUnitRate

    /** Money earned by exporting to grid */
    val moneyEarnedRupees: Double get() = exportedKwh * exportRate

    /** Total financial benefit */
    val totalBenefitRupees: Double get() = moneySavedRupees + moneyEarnedRupees

    /** Green Independence Score: 0–100 */
    val independenceScore: Int get() {
        if (consumedKwh <= 0) return 0
        return (minOf(generatedKwh / consumedKwh, 1.0) * 100).toInt()
    }

    /** Solar coverage % of total consumption */
    val solarCoveragePercent: Float get() {
        if (consumedKwh <= 0) return 0f
        return (minOf(generatedKwh, consumedKwh) / consumedKwh * 100).toFloat()
    }

    /** Panel efficiency % */
    val panelEfficiency: Float get() {
        if (panelCapacityKw <= 0) return 0f
        val expectedKwh = panelCapacityKw * 5.5 // 5.5 peak sun hours
        return (generatedKwh / expectedKwh * 100).toFloat().coerceIn(0f, 100f)
    }

    companion object {
        fun simulateGeneration(weather: WeatherCondition, capacityKw: Double): Double {
            val basePeakHours = 5.5
            val randomVariation = (0.85 + Math.random() * 0.30)
            return capacityKw * basePeakHours * weather.generationFactor * randomVariation
        }
    }
}
