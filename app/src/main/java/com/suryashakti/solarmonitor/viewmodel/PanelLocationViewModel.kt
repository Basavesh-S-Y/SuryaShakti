package com.suryashakti.solarmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.suryashakti.solarmonitor.data.LocationSearchResult
import com.suryashakti.solarmonitor.data.PanelLocation
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.util.LocationSearchService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle    : SearchState()
    object Searching : SearchState()
    data class Results(val items: List<LocationSearchResult>) : SearchState()
    object Empty   : SearchState()
    data class Error(val message: String) : SearchState()
}

class PanelLocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _savedLocation = MutableLiveData<PanelLocation?>(
        PanelLocationManager.getLocation(application)
    )
    val savedLocation: LiveData<PanelLocation?> = _savedLocation

    private var searchJob: Job? = null

    /** Debounced search — waits 400 ms after user stops typing */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }
        _searchState.value = SearchState.Searching
        searchJob = viewModelScope.launch {
            delay(400)   // debounce
            try {
                val results = LocationSearchService.search(query)
                _searchState.value = if (results.isEmpty())
                    SearchState.Empty
                else
                    SearchState.Results(results)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(
                    e.message ?: "Search failed. Check your connection."
                )
            }
        }
    }

    fun selectLocation(result: LocationSearchResult) {
        val loc = PanelLocation(
            latitude    = result.latitude,
            longitude   = result.longitude,
            displayName = result.displayName,
            shortName   = result.shortName,
            country     = result.country
        )
        PanelLocationManager.saveLocation(getApplication(), loc)
        _savedLocation.value = loc
        _searchState.value = SearchState.Idle
    }

    fun clearLocation() {
        PanelLocationManager.clearLocation(getApplication())
        _savedLocation.value = null
    }
}

class PanelLocationViewModelFactory(private val app: Application) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PanelLocationViewModel(app) as T
}
