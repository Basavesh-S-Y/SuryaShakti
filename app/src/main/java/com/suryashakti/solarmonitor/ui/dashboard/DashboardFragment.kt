package com.suryashakti.solarmonitor.ui.dashboard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.data.ForecastState
import com.suryashakti.solarmonitor.data.SolarForecast
import com.suryashakti.solarmonitor.databinding.FragmentDashboardBinding
import com.suryashakti.solarmonitor.ui.main.MainActivity
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.util.EnergyUtils
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModel
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModelFactory
import com.suryashakti.solarmonitor.viewmodel.ForecastViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val energyViewModel: EnergyViewModel by activityViewModels {
        EnergyViewModelFactory(requireActivity().application)
    }

    // Shared instance hosted by MainActivity — avoids double network calls
    private val forecastViewModel: ForecastViewModel by lazy {
        (requireActivity() as MainActivity).forecastViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEnergyObservers()
        setupForecastObservers()
        animateCards()
        // Tap irradiance card to manually refresh
        binding.cardIrradiance.setOnClickListener { forecastViewModel.refresh() }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ENERGY LOG OBSERVERS
    // ══════════════════════════════════════════════════════════════════════

    private fun setupEnergyObservers() {
        energyViewModel.latestLog.observe(viewLifecycleOwner) { log ->
            if (log != null) updateEnergyUI(log) else showEmpty()
        }
        energyViewModel.reportStats.observe(viewLifecycleOwner) { stats ->
            binding.tv30DaySavings.text = "₹%.0f".format(stats.totalBenefit)
            binding.tv30DayCo2.text     = EnergyUtils.getCO2Label(stats.co2SavedKg)
            binding.tvDaysLogged.text   = "${stats.daysLogged}"
        }
    }

    private fun updateEnergyUI(log: EnergyLog) {
        
        binding.tvWeather.text = log.weatherCondition.label

        animateNumber(binding.tvGenerated, log.generatedKwh, "%.2f\nkWh Generated")
        animateNumber(binding.tvConsumed,  log.consumedKwh,  "%.2f\nkWh Consumed")

        val net = log.netEnergyKwh
        binding.tvNetEnergy.text = if (net >= 0) "+%.2f kWh".format(net) else "%.2f kWh".format(net)
        binding.tvNetLabel.text  = if (net >= 0) "🟢 Exporting to Grid" else "🔴 Importing from Grid"
        binding.tvNetEnergy.setTextColor(if (net >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt())

        binding.tvSavedToday.text   = "₹%.2f".format(log.moneySavedRupees)
        binding.tvExportEarned.text = "₹%.2f".format(log.moneyEarnedRupees)
        binding.tvTotalBenefit.text = "₹%.2f".format(log.totalBenefitRupees)

        val solarFrac  = (minOf(log.generatedKwh, log.consumedKwh) / maxOf(log.consumedKwh, 0.01)).toFloat()
        val exportFrac = if (log.exportedKwh > 0) (log.exportedKwh / maxOf(log.generatedKwh, 0.01)).toFloat() else 0f
        val gridFrac   = (log.importedKwh / maxOf(log.consumedKwh, 0.01)).toFloat()
        binding.donutChart.setValues(solarFrac, exportFrac, gridFrac,
            log.independenceScore, EnergyUtils.getScoreGrade(log.independenceScore).second)

        binding.tvEfficiency.text           = "%.0f%%".format(log.panelEfficiency)
        binding.progressEfficiency.progress = log.panelEfficiency.toInt()

        if (log.exportedKwh > 0) {
            binding.cardExport.visibility = View.VISIBLE
            binding.tvExportKwh.text    = "%.2f kWh".format(log.exportedKwh)
            binding.tvExportAmount.text = "₹%.2f earned".format(log.moneyEarnedRupees)
        } else {
            binding.cardExport.visibility = View.GONE
        }
    }

    private fun showEmpty() {
        
        binding.tvWeather.text   = "No data logged yet"
        binding.tvGenerated.text = "0.00\nkWh Generated"
        binding.tvConsumed.text  = "0.00\nkWh Consumed"
        binding.tvNetEnergy.text = "0.00 kWh"
        binding.tvNetLabel.text  = "⚡ Log your first entry!"
        binding.donutChart.setValues(0f, 0f, 0f, 0, "Start Logging! 📝")
        binding.cardExport.visibility = View.GONE
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORECAST / IRRADIANCE OBSERVERS
    // ══════════════════════════════════════════════════════════════════════

    private fun setupForecastObservers() {
        forecastViewModel.forecastState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ForecastState.Loading -> showIrradianceLoading()
                is ForecastState.Success -> showIrradianceData(state.forecast)
                is ForecastState.Error   -> showIrradianceError(state)
            }
        }
    }

    private fun showIrradianceLoading() {
        with(binding) {
            cardIrradiance.visibility    = View.VISIBLE
            tvPlaceName.text             = "Locating…"
            tvCoordinates.text           = ""
            tvFetchAge.text              = ""
            tvIrradianceValue.text       = "-- W/m²"
            tvIrradianceStatus.text      = "Fetching from Open-Meteo ⏳"
            tvIrradianceStatus.setTextColor(0xFFAAAAAA.toInt())
            progressIrradiance.progress  = 0
            tvIrradiancePeak.text        = "-- W/m²"
            tvPeakHourLabel.text         = "--:00"
            tvPeakWindow.text            = "--:-- – --:--"
            cardPeakAlert.visibility     = View.GONE
        }
    }

    private fun showIrradianceData(forecast: SolarForecast) {
        with(binding) {
            cardIrradiance.visibility = View.VISIBLE

            // ── Place name — animate slide-in if it just appeared ──────────
            val newPlace = forecast.locationDisplayLabel
            if (tvPlaceName.text != newPlace) {
                tvPlaceName.alpha = 0f
                tvPlaceName.text  = newPlace
                tvPlaceName.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            // ── Coordinates line ──────────────────────────────────────────
            tvCoordinates.text = forecast.coordinateLabel

            // ── Age badge ─────────────────────────────────────────────────
            tvFetchAge.text = if (forecast.ageMinutes < 1) "just now"
                              else "${forecast.ageMinutes}m ago"

            // ── Animate W/m² counter ──────────────────────────────────────
            ValueAnimator.ofFloat(0f, forecast.currentIrradiance.toFloat()).apply {
                duration = 1000
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    tvIrradianceValue.text = "%.0f W/m²".format(it.animatedValue as Float)
                }
                start()
            }

            // ── Status label ──────────────────────────────────────────────
            tvIrradianceStatus.text = forecast.statusLabel
            tvIrradianceStatus.setTextColor(forecast.statusColor)

            // ── Progress bar (colour changes based on level) ──────────────
            progressIrradiance.progress = forecast.currentIrradiancePercent
            val barColor = when {
                forecast.currentIrradiance >= SolarForecast.PEAK_THRESHOLD_WM2 -> 0xFF00E676.toInt()
                forecast.currentIrradiance >= 300                               -> 0xFFFFD700.toInt()
                else                                                            -> 0xFFAAAAAA.toInt()
            }
            progressIrradiance.setIndicatorColor(barColor)

            // ── Day peak ──────────────────────────────────────────────────
            tvIrradiancePeak.text  = "%.0f W/m²".format(forecast.peakIrradiance)
            tvPeakHourLabel.text   = "%02d:00".format(forecast.peakHour)

            // ── Best run-appliances window ────────────────────────────────
            tvPeakWindow.text = forecast.peakWindowLabel

            // ── Peak alert banner — shown ONLY when irradiance > 500 W/m² ─
            if (forecast.isPeakSun) {
                cardPeakAlert.visibility = View.VISIBLE
                cardPeakAlert.alpha      = 0f
                // Pulse animation: fade in + slight scale bounce
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(cardPeakAlert, View.ALPHA, 0f, 1f)
                            .apply { duration = 500 },
                        ObjectAnimator.ofFloat(cardPeakAlert, View.SCALE_X, 0.92f, 1f)
                            .apply { duration = 500; interpolator = OvershootInterpolator(1.5f) },
                        ObjectAnimator.ofFloat(cardPeakAlert, View.SCALE_Y, 0.92f, 1f)
                            .apply { duration = 500; interpolator = OvershootInterpolator(1.5f) }
                    )
                    start()
                }
                tvPeakMessage.text =
                    "⚡ PEAK SUN in $newPlace — %.0f W/m²!\nRun pump, washing machine or EV charger now for free solar energy.".format(
                        forecast.currentIrradiance
                    )
            } else {
                cardPeakAlert.visibility = View.GONE
            }
        }
    }

    private fun showIrradianceError(state: ForecastState.Error) {
        with(binding) {
            cardIrradiance.visibility    = View.VISIBLE
            cardPeakAlert.visibility     = View.GONE
            tvIrradianceValue.text       = "-- W/m²"
            tvIrradiancePeak.text        = "-- W/m²"
            tvPeakHourLabel.text         = ""
            tvPeakWindow.text            = "--:-- – --:--"
            tvFetchAge.text              = "Error"
            progressIrradiance.progress  = 0
            tvIrradianceStatus.setTextColor(0xFFFF5252.toInt())

            if (state.isPermissionError) {
                tvPlaceName.text         = "Location needed"
                tvCoordinates.text       = "Grant permission for live data"
                tvIrradianceStatus.text  = "📍 Tap to grant location permission"
            } else {
                tvPlaceName.text         = "Connection failed"
                tvCoordinates.text       = "Tap card to retry"
                tvIrradianceStatus.text  = "❌ ${state.message}"
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATION HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private fun animateCards() {
        listOf(
            binding.cardIrradiance, binding.cardDonut, binding.cardSavings,
            binding.cardEfficiency, binding.cardMonthly
        ).forEachIndexed { i, card ->
            card.alpha        = 0f
            card.translationY = 80f
            card.animate()
                .alpha(1f).translationY(0f)
                .setDuration(500).setStartDelay(80L * i)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateNumber(tv: android.widget.TextView, to: Double, fmt: String) {
        ValueAnimator.ofFloat(0f, to.toFloat()).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                val parts = fmt.split("\n")
                tv.text = when (parts.size) {
                    3    -> "%.2f\n${parts[1]}\n${parts[2]}".format(v)
                    2    -> "%.2f\n${parts[1]}".format(v)
                    else -> fmt.format(v)
                }
            }
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
