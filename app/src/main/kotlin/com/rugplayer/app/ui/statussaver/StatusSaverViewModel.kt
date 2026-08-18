package com.rugplayer.app.ui.statussaver

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rugplayer.app.data.prefs.SettingsRepository
import com.rugplayer.app.data.repository.StatusItem
import com.rugplayer.app.data.repository.StatusSaverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatusSaverUiState(
    val treeUri: Uri? = null,
    val statuses: List<StatusItem> = emptyList(),
    val savedUris: Set<Uri> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class StatusSaverViewModel(
    private val appContext: Context,
    private val repository: StatusSaverRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusSaverUiState())
    val uiState: StateFlow<StatusSaverUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUri = settingsRepository.settings.first().statusSaverTreeUri?.let(Uri::parse)
            if (savedUri != null) {
                _uiState.update { it.copy(treeUri = savedUri) }
                refresh(savedUri)
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        viewModelScope.launch { settingsRepository.setStatusSaverTreeUri(uri.toString()) }
        _uiState.update { it.copy(treeUri = uri) }
        refresh(uri)
    }

    fun refresh(treeUri: Uri? = _uiState.value.treeUri) {
        if (treeUri == null) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val statuses = runCatching { repository.listStatuses(treeUri) }
            statuses.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, statuses = list) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't read that folder")
                }
            }
        }
    }

    fun save(item: StatusItem) {
        viewModelScope.launch {
            repository.saveStatus(item).onSuccess {
                _uiState.update { it.copy(savedUris = it.savedUris + item.uri) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Couldn't save that file") }
            }
        }
    }

    fun saveAll() {
        val toSave = _uiState.value.statuses.filter { it.uri !in _uiState.value.savedUris }
        toSave.forEach(::save)
    }
}
