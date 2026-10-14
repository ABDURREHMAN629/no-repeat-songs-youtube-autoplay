package com.norepeat.songs

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class UiState(
    val working: Boolean = false,
    val status: String = "",
    val isError: Boolean = false,
    val rememberedCount: Int = 0,
    val playlist: MadePlaylist? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val played = PlayedStore(app)
    private val prefs = app.getSharedPreferences("last_playlist", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UiState(rememberedCount = played.count, playlist = loadLastPlaylist()))
    val state: StateFlow<UiState> = _state

    fun makePlaylist(artists: List<String>, genres: List<String>, size: Int) {
        runJob {
            val app = getApplication<Application>()
            val country = Locale.getDefault().country.ifEmpty { "US" }
            val web = YouTubeWeb(app.getSharedPreferences("youtube_web", Context.MODE_PRIVATE), country)
            val maker = PlaylistMaker(app, web, played) { message ->
                _state.update { it.copy(status = message, isError = false) }
            }
            val playlist = maker.make(artists, genres, size)
            saveLastPlaylist(playlist)
            _state.update {
                it.copy(
                    playlist = playlist,
                    status = playlist.note ?: "Done! ${playlist.songCount} new songs are ready.",
                )
            }
        }
    }

    fun importHistory(uri: Uri) {
        runJob {
            _state.update { it.copy(status = "Reading your history file…") }
            val songs = HistoryImporter.read(getApplication(), uri)
            val added = played.addAll(songs)
            _state.update {
                it.copy(status = "Imported ${"%,d".format(songs.size)} videos from your history ($added were new).")
            }
        }
    }

    fun forgetAll() {
        runJob {
            played.clear()
            getApplication<Application>().getSharedPreferences("page_tokens", Context.MODE_PRIVATE).edit { clear() }
            _state.update { it.copy(status = "Forgot all songs. Every song counts as new again.") }
        }
    }

    fun showError(message: String) {
        _state.update { it.copy(status = message, isError = true) }
    }

    private fun runJob(job: suspend () -> Unit) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, status = "", isError = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { job() }
            } catch (e: Exception) {
                _state.update { it.copy(status = friendlyMessage(e), isError = true) }
            } finally {
                _state.update { it.copy(working = false, rememberedCount = played.count) }
            }
        }
    }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is IOException -> "No internet connection, or YouTube didn't answer. Please try again."
        else -> e.message ?: e.toString()
    }

    private fun saveLastPlaylist(playlist: MadePlaylist) {
        prefs.edit {
            putString("url", playlist.url)
            putString("title", playlist.title)
            putInt("count", playlist.songCount)
        }
    }

    private fun loadLastPlaylist(): MadePlaylist? {
        val url = prefs.getString("url", null) ?: return null
        return MadePlaylist(url, prefs.getString("title", null).orEmpty(), prefs.getInt("count", 0), null)
    }
}
