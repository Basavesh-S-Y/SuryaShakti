package com.suryashakti.solarmonitor.ui.log

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.adapter.EnergyLogAdapter
import com.suryashakti.solarmonitor.data.WeatherCondition
import com.suryashakti.solarmonitor.databinding.FragmentLogEntryBinding
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModel
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModelFactory
import com.suryashakti.solarmonitor.viewmodel.SaveStatus
import java.util.Calendar

class LogEntryFragment : Fragment() {

    private var _binding: FragmentLogEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EnergyViewModel by activityViewModels {
        EnergyViewModelFactory(requireActivity().application)
    }

    private var selectedDateMillis = DateUtils.todayMidnightMillis()
    private var selectedWeather = WeatherCondition.SUNNY
    private lateinit var adapter: EnergyLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePicker()
        setupWeatherChips()
        setupSimulateButton()
        setupSaveButton()
        setupRecentLogs()
        observeViewModel()
        animateForm()
    }

    private fun setupDatePicker() {
        binding.tvSelectedDate.text = DateUtils.toDisplayString(selectedDateMillis)
        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDateMillis
            DatePickerDialog(
                requireContext(),
                R.style.DatePickerTheme,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    selectedDateMillis = DateUtils.toMidnightMillis(cal.timeInMillis)
                    binding.tvSelectedDate.text = DateUtils.toDisplayString(selectedDateMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupWeatherChips() {
        val weatherMap = mapOf(
            binding.chipSunny to WeatherCondition.SUNNY,
            binding.chipPartlyCloudy to WeatherCondition.PARTLY_CLOUDY,
            binding.chipCloudy to WeatherCondition.CLOUDY,
            binding.chipRainy to WeatherCondition.RAINY
        )

        binding.chipSunny.isChecked = true

        weatherMap.forEach { (chip, weather) ->
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedWeather = weather
                    viewModel.setWeather(weather)
                }
            }
        }
    }

    private fun setupSimulateButton() {
        binding.btnSimulate.setOnClickListener {
            val capacityStr = binding.etPanelCapacity.text.toString()
            val capacity = capacityStr.toDoubleOrNull() ?: 3.0
            viewModel.simulateGeneration(selectedWeather, capacity)

            // Animate button
            binding.btnSimulate.animate()
                .scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction {
                    binding.btnSimulate.animate()
                        .scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
        }

        viewModel.simulatedGeneration.observe(viewLifecycleOwner) { gen ->
            if (gen > 0) {
                binding.etGenerated.setText("%.2f".format(gen))
                binding.tvSimulationHint.visibility = View.VISIBLE
                binding.tvSimulationHint.text = "🤖 Simulated for ${selectedWeather.label}"
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val genStr = binding.etGenerated.text.toString()
            val conStr = binding.etConsumed.text.toString()
            val rateStr = binding.etUnitRate.text.toString()
            val exportRateStr = binding.etExportRate.text.toString()
            val capacityStr = binding.etPanelCapacity.text.toString()
            val notes = binding.etNotes.text.toString()

            if (genStr.isEmpty()) {
                binding.etGenerated.error = "Enter kWh generated"
                return@setOnClickListener
            }
            if (conStr.isEmpty()) {
                binding.etConsumed.error = "Enter kWh consumed"
                return@setOnClickListener
            }

            val gen = genStr.toDoubleOrNull() ?: run {
                binding.etGenerated.error = "Invalid number"
                return@setOnClickListener
            }
            val con = conStr.toDoubleOrNull() ?: run {
                binding.etConsumed.error = "Invalid number"
                return@setOnClickListener
            }

            if (gen < 0 || con < 0) {
                Toast.makeText(requireContext(), "Values cannot be negative", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rate = rateStr.toDoubleOrNull() ?: 8.0
            val exportRate = exportRateStr.toDoubleOrNull() ?: 4.0
            val capacity = capacityStr.toDoubleOrNull() ?: 3.0

            viewModel.saveLog(
                dateMillis = selectedDateMillis,
                generatedKwh = gen,
                consumedKwh = con,
                weather = selectedWeather,
                perUnitRate = rate,
                exportRate = exportRate,
                panelCapacity = capacity,
                notes = notes
            )

            // Save button animation
            binding.btnSave.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction {
                    binding.btnSave.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
        }
    }

    private fun setupRecentLogs() {
        adapter = EnergyLogAdapter { log ->
            // Delete on long press
            viewModel.deleteLog(log)
            Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
        }
        binding.rvRecentLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentLogs.adapter = adapter

        viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs.take(10))
        }
    }

    private fun observeViewModel() {
        viewModel.saveStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is SaveStatus.Saved -> {
                    showSuccessToast("✅ Log saved successfully!")
                    clearForm()
                    viewModel.resetSaveStatus()
                }
                is SaveStatus.Updated -> {
                    showSuccessToast("🔄 Log updated for this date!")
                    clearForm()
                    viewModel.resetSaveStatus()
                }
                is SaveStatus.Error -> {
                    Toast.makeText(requireContext(), "❌ Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetSaveStatus()
                }
                else -> {}
            }
        }
    }

    private fun clearForm() {
        binding.etGenerated.setText("")
        binding.etConsumed.setText("")
        binding.etNotes.setText("")
        binding.tvSimulationHint.visibility = View.GONE
    }

    private fun showSuccessToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun animateForm() {
        val views = listOf(
            binding.cardDate, binding.cardWeather, binding.cardGeneration,
            binding.cardConsumption, binding.cardSettings, binding.btnSave
        )
        views.forEachIndexed { index, v ->
            v.alpha = 0f
            v.translationX = if (index % 2 == 0) -80f else 80f
            v.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(450)
                .setStartDelay(80L * index)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
