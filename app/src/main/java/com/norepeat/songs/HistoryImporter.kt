package com.norepeat.songs

import android.content.Context
import android.net.Uri
import android.text.Html
import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

/**
 * Reads "watch-history" from a Google Takeout export (the .zip itself, or the
 * watch-history.json / watch-history.html file inside it).
 */
object HistoryImporter {
    private val VIDEO_ID = Regex("""[?&]v=([A-Za-z0-9_-]{11})""")
    private val HTML_LINK = Regex("""watch\?v=([A-Za-z0-9_-]{11})[^"]*">([^<]+)</a>""")

    fun read(context: Context, uri: Uri): List<Song> {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the file.")
        BufferedInputStream(input).use { stream ->
            stream.mark(2)
            val isZip = stream.read() == 'P'.code && stream.read() == 'K'.code
            stream.reset()
            return if (isZip) readZip(stream) else readFile(stream)
        }
    }

    private fun readZip(stream: InputStream): List<Song> {
        val zip = ZipInputStream(stream)
        val songs = LinkedHashMap<String, Song>()
        var foundFile = false
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.lowercase()
            if (!entry.isDirectory && "watch-history" in name && (name.endsWith(".json") || name.endsWith(".html"))) {
                foundFile = true
                // Don't close the per-file reader: that would close the whole zip.
                readFile(BufferedInputStream(zip)).forEach { songs.putIfAbsent(it.id, it) }
            }
        }
        if (!foundFile) {
            throw IllegalStateException("No watch-history file found in this zip. Make sure \"history\" was included in the Takeout export.")
        }
        return songs.values.toList()
    }

    private fun readFile(stream: BufferedInputStream): List<Song> {
        stream.mark(4096)
        val start = ByteArray(4096)
        val length = stream.read(start).coerceAtLeast(0)
        stream.reset()
        val firstChar = String(start, 0, length, Charsets.UTF_8).trimStart('﻿', ' ', '\n', '\r', '\t').firstOrNull()
        return if (firstChar == '[') readJson(stream) else readHtml(stream)
    }

    private fun readJson(stream: InputStream): List<Song> {
        val songs = LinkedHashMap<String, Song>()
        val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
        reader.beginArray()
        while (reader.hasNext()) {
            var title: String? = null
            var url: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                when {
                    name == "title" && reader.peek() == JsonToken.STRING -> title = reader.nextString()
                    name == "titleUrl" && reader.peek() == JsonToken.STRING -> url = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            val id = url?.let { VIDEO_ID.find(it)?.groupValues?.get(1) }
            if (id != null && title != null) songs.putIfAbsent(id, Song(id, title.removePrefix("Watched ")))
        }
        reader.endArray()
        return songs.values.toList()
    }

    private fun readHtml(stream: InputStream): List<Song> {
        val songs = LinkedHashMap<String, Song>()
        val reader = InputStreamReader(stream, Charsets.UTF_8)
        val buffer = CharArray(256 * 1024)
        var carry = ""
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            // Keep the end of the previous chunk so links cut in half are still found.
            val text = carry + String(buffer, 0, read)
            for (match in HTML_LINK.findAll(text)) {
                val id = match.groupValues[1]
                val title = Html.fromHtml(match.groupValues[2], Html.FROM_HTML_MODE_LEGACY).toString()
                songs.putIfAbsent(id, Song(id, title))
            }
            carry = text.takeLast(2048)
        }
        return songs.values.toList()
    }
}
