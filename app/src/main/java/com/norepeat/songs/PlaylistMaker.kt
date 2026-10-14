package com.norepeat.songs

import android.content.Context
import androidx.core.content.edit
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MadePlaylist(val url: String, val title: String, val songCount: Int, val note: String?)

/**
 * Finds songs you have not heard yet and turns them into a temporary YouTube playlist.
 */
class PlaylistMaker(
    context: Context,
    private val web: YouTubeWeb,
    private val played: PlayedStore,
    private val progress: (String) -> Unit,
) {
    private val pageTokens = context.getSharedPreferences("page_tokens", Context.MODE_PRIVATE)

    fun make(artists: List<String>, genres: List<String>, size: Int): MadePlaylist {
        val queries = (artists.map { "$it song" } + genres).distinctBy { it.lowercase() }
        require(queries.isNotEmpty()) { "Add at least one artist or genre." }

        val songs = findNewSongs(queries, size.coerceAtMost(YouTubeWeb.MAX_PLAYLIST)).shuffled()
        if (songs.isEmpty()) {
            throw IllegalStateException("No new songs found. Try adding more artists or genres.")
        }

        progress("Making the playlist…")
        val url = web.temporaryPlaylist(songs.map { it.id })
        played.addAll(songs)

        val title = "New songs · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
        val note = if (songs.size < size) {
            "Found ${songs.size} new songs (you asked for $size). Add more artists or genres for more."
        } else null
        return MadePlaylist(url, title, songs.size, note)
    }

    private fun findNewSongs(queries: List<String>, size: Int): List<Song> {
        val sources = queries.map { QuerySource(it) }
        val picked = LinkedHashMap<String, Song>()
        val pickedMemory = SongMemory()
        var pages = 0

        while (picked.size < size && pages < MAX_PAGES) {
            var searched = false
            for (source in sources) {
                if (picked.size >= size || pages >= MAX_PAGES) break
                if (source.finished || source.waiting.isNotEmpty()) continue

                progress("Searching: ${source.query} (found ${picked.size} / $size)")
                source.fetchNextPage()
                pages++
                searched = true
            }

            // Take songs in turns from each artist/genre so the playlist is mixed.
            var tookOne = true
            while (picked.size < size && tookOne) {
                tookOne = false
                for (source in sources) {
                    if (picked.size >= size) break
                    while (source.waiting.isNotEmpty()) {
                        val song = source.waiting.removeFirst()
                        if (song.id in picked || pickedMemory.matches(song.title)) continue
                        picked[song.id] = song
                        pickedMemory.add(song.title)
                        tookOne = true
                        break
                    }
                }
            }

            if (!searched) break
        }
        return picked.values.toList()
    }

    /**
     * Search results for one artist/genre. Each time it reads page 1 first (for new
     * releases), then jumps to where it stopped last time so it keeps finding fresh songs.
     */
    private inner class QuerySource(val query: String) {
        val waiting = ArrayDeque<Song>()
        var finished = false
            private set

        private val key = query.lowercase()
        private var readFirstPage = false
        private var nextToken: String? = null
        private var afterFirstPage: String? = null // used if the saved position stopped working

        fun fetchNextPage() {
            val page = if (!readFirstPage) {
                readFirstPage = true
                web.search(query, null).also { first ->
                    val saved = pageTokens.getString(key, null)
                    nextToken = saved ?: first.nextPageToken
                    afterFirstPage = if (saved != null) first.nextPageToken else null
                }
            } else {
                val token = nextToken ?: return
                val result = try {
                    web.search(query, token)
                } catch (e: IOException) {
                    if (afterFirstPage == null) throw e else null
                } catch (e: YouTubeWebException) {
                    if (afterFirstPage == null) throw e else null
                }
                if (result == null || result.videos.isEmpty()) {
                    val fallback = afterFirstPage
                    if (fallback != null && token != fallback) {
                        // The saved position expired: continue right after page 1 instead.
                        afterFirstPage = null
                        nextToken = fallback
                        pageTokens.edit { remove(key) }
                        return fetchNextPage()
                    }
                }
                afterFirstPage = null
                nextToken = result?.nextPageToken
                pageTokens.edit {
                    if (nextToken != null) putString(key, nextToken) else remove(key)
                }
                result
            }

            if (nextToken == null) finished = true
            page?.videos
                ?.filter { isRealSong(it, query) && !played.isPlayed(it.song) }
                ?.forEach { waiting.addLast(it.song) }
        }
    }

    /** Skips shorts, long jukeboxes, live streams and slowed/reverb style re-uploads. */
    private fun isRealSong(video: Video, query: String): Boolean {
        if (video.seconds !in MIN_SECONDS..MAX_SECONDS) return false
        val title = video.song.title.lowercase()
        val asked = query.lowercase()
        return JUNK_WORDS.none { it in title && it !in asked }
    }

    private companion object {
        const val MAX_PAGES = 40
        const val MIN_SECONDS = 90
        const val MAX_SECONDS = 9 * 60
        val JUNK_WORDS = listOf(
            "slowed", "reverb", "sped up", "lofi", "lo-fi", "8d", "nightcore", "jukebox",
            "nonstop", "non stop", "non-stop", "mashup", "status", "#shorts", "ringtone",
            "karaoke", "teaser", "reaction", "interview", "podcast", "tutorial", "whatsapp",
        )
    }
}
