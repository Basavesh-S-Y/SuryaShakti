package com.suryashakti.solarmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.suryashakti.solarmonitor.data.ForecastState
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.data.SolarForecast
import com.suryashakti.solarmonitor.repository.ForecastRepository
import kotlinx.coroutines.launch

class ForecastViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForecastRepository(application)

    private val _forecastState = MutableLiveData<ForecastState>(ForecastState.Loading)
    val forecastState: LiveData<ForecastState> = _forecastState

    val forecast: LiveData<SolarForecast?> = forecastState.map { state ->
        if (state is ForecastState.Success) state.forecast else null
    }

    init {
        // Only load if panel location has been set
        if (PanelLocationManager.isLocationSet(application)) {
            loadForecast()
        } else {
            _forecastState.value = ForecastState.Error(
                message = "Solar panel location not set",
                isPermissionError = false
            )
        }
    }

    fun loadForecast(force: Boolean = false) {
        _forecastState.value = ForecastState.Loading
        viewModelScope.launch {
            repository.getForecast(force).fold(
                onSuccess  = { _forecastState.value = ForecastState.Success(it) },
                onFailure  = { _forecastState.value = ForecastState.Error(it.message ?: "Forecast failed") }
            )
        }
    }

    fun refresh() = loadForecast(force = true)

    /** Called when user returns from PanelLocationActivity after resetting location */
    fun onPanelLocationUpdated() = loadForecast(force = true)

    // Kept for API compatibility — not used since we use panel location, not GPS
    fun onPermissionGranted() = loadForecast(force = true)
}

class ForecastViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForecastViewModel::class.java))
            return ForecastViewModel(application) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
