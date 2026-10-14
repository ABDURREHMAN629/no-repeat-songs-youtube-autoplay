package com.norepeat.songs

import android.content.Context
import java.io.File

/** A song (YouTube video) the app knows about. */
data class Song(val id: String, val title: String)

/**
 * Every song you have already heard, saved on the phone in a small text file
 * (one "videoId<TAB>title" per line).
 */
class PlayedStore(context: Context) {
    private val file = File(context.filesDir, "played.tsv")
    private val ids = HashSet<String>()
    private val memory = SongMemory()

    init {
        if (file.exists()) {
            file.forEachLine { line ->
                val tab = line.indexOf('\t')
                if (tab > 0) remember(Song(line.substring(0, tab), line.substring(tab + 1)))
            }
        }
    }

    val count: Int
        @Synchronized get() = ids.size

    @Synchronized
    fun isPlayed(song: Song): Boolean = song.id in ids || memory.matches(song.title)

    /** Saves the songs that are new. Returns how many were new. */
    @Synchronized
    fun addAll(songs: Collection<Song>): Int {
        val fresh = songs.filter { it.id !in ids }.distinctBy { it.id }
        if (fresh.isEmpty()) return 0
        file.appendText(fresh.joinToString(separator = "") { "${it.id}\t${clean(it.title)}\n" })
        fresh.forEach(::remember)
        return fresh.size
    }

    @Synchronized
    fun clear() {
        file.delete()
        ids.clear()
        memory.clear()
    }

    private fun remember(song: Song) {
        if (ids.add(song.id)) memory.add(song.title)
    }

    private fun clean(title: String) = title.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}
