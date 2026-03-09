package com.attentiondiscapture.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.*
import com.attentiondiscapture.data.AppRepository
import com.attentiondiscapture.data.MonitoredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class Filter { ALL, MONITORED, UNMONITORED }

    private val repo = AppRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filter      = MutableStateFlow(Filter.ALL)

    val filter: StateFlow<Filter> = _filter

    val apps: StateFlow<List<MonitoredApp>> = repo.allApps
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.appName.contains(query, ignoreCase = true) }
        }
        .combine(_filter) { list, filter ->
            when (filter) {
                Filter.ALL          -> list
                Filter.MONITORED    -> list.filter { it.isEnabled }
                Filter.UNMONITORED  -> list.filter { !it.isEnabled }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val enabledCount: StateFlow<Int> = repo.enabledApps
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch(Dispatchers.IO) { repo.syncInstalledApps() }
    }

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setFilter(f: Filter)     { _filter.value = f }

    fun toggleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repo.setEnabled(packageName, enabled) }
    }

    fun setDelay(packageName: String, delay: Float) {
        viewModelScope.launch { repo.setDelay(packageName, delay) }
    }

    fun getAppIcon(packageName: String) = try {
        getApplication<Application>().packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) { null }
}
