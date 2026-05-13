package com.suryashakti.solarmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.suryashakti.solarmonitor.data.SyncState
import com.suryashakti.solarmonitor.repository.EnergyRepository
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.CloudSyncManager
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val energyRepository = EnergyRepository(application)

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    private val _lastSyncTime = MutableLiveData<Long>(0L)
    val lastSyncTime: LiveData<Long> = _lastSyncTime

    // Mirror of local logs count for the sync UI
    val allLogs = energyRepository.allLogs

    fun startSync() {
        val uid = AuthManager.currentUser?.uid
        if (uid == null) {
            _syncState.value = SyncState.NotLoggedIn
            return
        }

        _syncState.value = SyncState.Syncing

        viewModelScope.launch {
            try {
                // Collect local logs synchronously from the DB
                val localLogs = allLogs.value ?: emptyList()

                val result = CloudSyncManager.sync(
                    uid       = uid,
                    localLogs = localLogs,
                    onSaveLocal = { log -> energyRepository.insertLog(log) }
                )

                val now = System.currentTimeMillis()
                _lastSyncTime.postValue(now)
                _syncState.postValue(SyncState.Success(result.uploaded, result.downloaded))

            } catch (e: Exception) {
                _syncState.postValue(
                    SyncState.Error(e.message ?: "Sync failed. Check connection.")
                )
            }
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}

class SyncViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SyncViewModel(app) as T
}
