package com.rugplayer.app.ui.transfer

import android.content.Context
import com.rugplayer.app.data.model.VideoItem
import com.rugplayer.app.data.repository.VideoRepository
import com.rugplayer.app.transfer.LocalFileClient
import com.rugplayer.app.transfer.LocalFileServer
import com.rugplayer.app.transfer.findLocalIpAddress
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val videos: List<VideoItem> = emptyList(),
    val sendingVideo: VideoItem? = null,
    val sendUrl: String? = null,
    val clientsServed: Int = 0,
    val receiveUrlInput: String = "",
    val isReceiving: Boolean = false,
    val receiveProgress: Float = 0f,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
)

class TransferViewModel(
    private val appContext: Context,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var server: LocalFileServer? = null
    private var receiveJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(videos = videoRepository.queryVideosOnce()) }
        }
    }

    fun startSending(video: VideoItem) {
        stopSending()
        val fileServer = LocalFileServer(appContext.contentResolver)
        server = fileServer
        val port = fileServer.start(
            scope = viewModelScope,
            uri = video.uri,
            fileName = video.displayName,
            fileSizeBytes = video.sizeBytes,
            onClientConnected = { _uiState.update { it.copy(clientsServed = it.clientsServed + 1) } },
        )
        val ip = findLocalIpAddress()
        val url = if (ip != null) "http://$ip:$port/${video.displayName}" else null
        _uiState.update {
            it.copy(
                sendingVideo = video,
                sendUrl = url,
                clientsServed = 0,
                statusMessage = if (url == null) "Couldn't find a Wi-Fi address. Connect to Wi-Fi and try again." else null,
                statusIsError = url == null,
            )
        }
    }

    fun stopSending() {
        server?.stop()
        server = null
        _uiState.update { it.copy(sendingVideo = null, sendUrl = null, clientsServed = 0) }
    }

    fun setReceiveUrlInput(value: String) {
        _uiState.update { it.copy(receiveUrlInput = value) }
    }

    fun startReceiving() {
        val url = _uiState.value.receiveUrlInput.trim()
        if (url.isEmpty() || receiveJob?.isActive == true) return

        receiveJob = viewModelScope.launch {
            _uiState.update { it.copy(isReceiving = true, receiveProgress = 0f, statusMessage = null) }
            val result = LocalFileClient.download(appContext, url) { progress ->
                _uiState.update { it.copy(receiveProgress = progress.fraction) }
            }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isReceiving = false,
                        receiveProgress = 1f,
                        statusMessage = "Saved to your library",
                        statusIsError = false,
                        videos = videoRepository.queryVideosOnce(),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isReceiving = false,
                        statusMessage = error.message ?: "Transfer failed",
                        statusIsError = true,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        server?.stop()
        super.onCleared()
    }
}
