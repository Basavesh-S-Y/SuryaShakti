package com.suryashakti.solarmonitor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.data.WeatherCondition

/**
 * Generates context-aware prosumer tips based on today's energy data.
 * Simulates what a GenAI assistant would suggest.
 */
class TipViewModel : ViewModel() {

    private val _tip = MutableLiveData<ProsumerTip>()
    val tip: LiveData<ProsumerTip> = _tip

    fun generateTip(log: EnergyLog?) {
        _tip.value = computeTip(log)
    }

    private fun computeTip(log: EnergyLog?): ProsumerTip {
        if (log == null) {
            return ProsumerTip(
                icon = "📝",
                title = "Start Logging!",
                message = "Add your first energy entry to see personalized tips.",
                priority = TipPriority.INFO
            )
        }

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        // Peak sun window — highest priority tip
        if (hour in 10..15 && log.generatedKwh > log.consumedKwh * 0.5) {
            return ProsumerTip(
                icon = "⚡",
                title = "Peak Sun Window Active!",
                message = "You're generating ${String.format("%.2f", log.generatedKwh)} kWh. " +
                        "Run your water pump, washing machine, or EV charger NOW for free energy.",
                priority = TipPriority.HIGH
            )
        }

        // Over-generation — export scenario
        if (log.exportedKwh > 0.5) {
            return ProsumerTip(
                icon = "🔋",
                title = "Great — Exporting ${String.format("%.2f", log.exportedKwh)} kWh!",
                message = "You're a Prosumer! Earning ₹${String.format("%.2f", log.moneyEarnedRupees)} " +
                        "from grid export today. Consider adding battery storage to store instead.",
                priority = TipPriority.SUCCESS
            )
        }

        // Heavy grid import
        if (log.importedKwh > 5.0) {
            return ProsumerTip(
                icon = "🌤️",
                title = "High Grid Import Detected",
                message = "You consumed ${String.format("%.2f", log.importedKwh)} kWh from the grid. " +
                        "Shift heavy loads (geyser, AC) to morning peak-sun hours to reduce this.",
                priority = TipPriority.WARNING
            )
        }

        // Low generation (cloudy day)
        if (log.weatherCondition == WeatherCondition.CLOUDY || log.weatherCondition == WeatherCondition.RAINY) {
            return ProsumerTip(
                icon = "☁️",
                title = "Low Generation Day",
                message = "Cloud cover reduces output by ${String.format("%.0f", (1 - log.weatherCondition.generationFactor) * 100)}%. " +
                        "Postpone heavy appliances to the next sunny day to maximise savings.",
                priority = TipPriority.INFO
            )
        }

        // Good independence score
        if (log.independenceScore >= 80) {
            return ProsumerTip(
                icon = "🌟",
                title = "Excellent Independence Score: ${log.independenceScore}%",
                message = "Your solar covered ${String.format("%.0f", log.solarCoveragePercent)}% of today's " +
                        "consumption, saving ₹${String.format("%.2f", log.moneySavedRupees)}. Keep it up!",
                priority = TipPriority.SUCCESS
            )
        }

        // Evening — plan for tomorrow
        if (hour >= 18) {
            return ProsumerTip(
                icon = "🌙",
                title = "Evening Energy Tip",
                message = "Pre-cool your home or charge devices now using stored solar energy. " +
                        "Tomorrow's generation forecast: ${log.weatherCondition.label}.",
                priority = TipPriority.INFO
            )
        }

        // Default tip — CO2 impact
        return ProsumerTip(
            icon = "🌿",
            title = "Green Impact Today",
            message = "Your solar panels prevented ${String.format("%.2f", log.generatedKwh * 0.82)} kg of CO₂. " +
                    "That's equivalent to planting ${String.format("%.1f", log.generatedKwh * 0.82 / 21.7)} trees!",
            priority = TipPriority.INFO
        )
    }
}

data class ProsumerTip(
    val icon: String,
    val title: String,
    val message: String,
    val priority: TipPriority
)

enum class TipPriority { HIGH, WARNING, SUCCESS, INFO }
