package com.norepeat.songs

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A search result with its length, so we can skip shorts and long mixes. */
data class Video(val song: Song, val seconds: Int)

data class SearchPage(val videos: List<Video>, val nextPageToken: String?)

class YouTubeWebException(val code: Int, message: String) : Exception(message)

/**
 * Talks to YouTube the same way the youtube.com website does (no Google account,
 * no API key, no daily limit). Unofficial: if YouTube changes its website this
 * may need updating.
 */
class YouTubeWeb(private val prefs: SharedPreferences, private val country: String) {

    /** One page (about 20) of videos for [query]; pass [pageToken] to get the next page. */
    fun search(query: String, pageToken: String?): SearchPage {
        val body = JSONObject()
        if (pageToken != null) body.put("continuation", pageToken)
        else body.put("query", query).put("params", VIDEOS_ONLY)
        return SearchParser.parse(post("search", body))
    }

    /**
     * Asks YouTube to turn up to 50 songs into a temporary playlist and returns
     * a link that plays it (https://www.youtube.com/watch?v=…&list=TLGG…).
     */
    fun temporaryPlaylist(videoIds: List<String>): String {
        val url = "$BASE/watch_videos?video_ids=" + videoIds.take(MAX_PLAYLIST).joinToString(",")
        val connection = open(url)
        try {
            connection.instanceFollowRedirects = false
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            if (code in 300..399 && location != null && "list=" in location) {
                return if (location.startsWith("/")) BASE + location else location
            }
            throw YouTubeWebException(code, "YouTube didn't make the playlist (HTTP $code). Please try again.")
        } finally {
            connection.disconnect()
        }
    }

    private fun post(endpoint: String, body: JSONObject): JSONObject {
        try {
            return postOnce(endpoint, body, clientVersion())
        } catch (e: YouTubeWebException) {
            // An outdated website version is the usual reason YouTube says no (it answers 400/404);
            // refresh it once.
            if (e.code !in 400..499) throw e
            val fresh = refreshClientVersion() ?: throw e
            return postOnce(endpoint, body, fresh)
        }
    }

    private fun postOnce(endpoint: String, body: JSONObject, version: String): JSONObject {
        body.put(
            "context",
            JSONObject().put(
                "client",
                JSONObject()
                    .put("clientName", "WEB")
                    .put("clientVersion", version)
                    .put("hl", "en")
                    .put("gl", country),
            ),
        )
        val connection = open("$BASE/youtubei/v1/$endpoint?prettyPrint=false")
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-YouTube-Client-Name", "1")
            connection.setRequestProperty("X-YouTube-Client-Version", version)
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw YouTubeWebException(code, "YouTube answered with an error (HTTP $code). Please try again.")
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun clientVersion(): String = prefs.getString(KEY_VERSION, null) ?: DEFAULT_VERSION

    /** Reads the current website version from the YouTube home page. */
    private fun refreshClientVersion(): String? {
        val connection = open("$BASE/?hl=en")
        val version = try {
            if (connection.responseCode !in 200..299) return null
            val page = connection.inputStream.bufferedReader().use { it.readText() }
            VERSION_IN_PAGE.find(page)?.groupValues?.get(1)
        } finally {
            connection.disconnect()
        }
        if (version == null || version == clientVersion()) return null
        prefs.edit { putString(KEY_VERSION, version) }
        return version
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept-Language", "en")
        }

    companion object {
        const val MAX_PLAYLIST = 50
        private const val BASE = "https://www.youtube.com"
        private const val VIDEOS_ONLY = "EgIQAQ==" // search filter: Type = Video
        private const val DEFAULT_VERSION = "2.20261001.01.00"
        private const val KEY_VERSION = "web_client_version"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
        private val VERSION_IN_PAGE = Regex(""""INNERTUBE_CLIENT_VERSION":"([^"]+)"""")
    }
}

/** Pulls the videos and the next-page token out of a YouTube search response. */
object SearchParser {
    fun parse(response: JSONObject): SearchPage {
        val videos = LinkedHashMap<String, Video>()
        var nextPageToken: String? = null
        walk(response) { key, value ->
            if (key == "videoRenderer") {
                toVideo(value)?.let { videos.putIfAbsent(it.song.id, it) }
            } else if (key == "continuationItemRenderer" && nextPageToken == null) {
                nextPageToken = value.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                    ?.optString("token")
                    ?.ifEmpty { null }
            }
        }
        return SearchPage(videos.values.toList(), nextPageToken)
    }

    /** "3:45" -> 225, "1:02:03" -> 3723; 0 when missing (live streams, premieres). */
    fun seconds(length: String): Int =
        length.split(':').fold(0) { total, part -> total * 60 + (part.trim().toIntOrNull() ?: return 0) }

    private fun toVideo(renderer: JSONObject): Video? {
        val id = renderer.optString("videoId")
        if (id.length != 11) return null
        val title = renderer.optJSONObject("title")?.let { title ->
            title.optString("simpleText").ifEmpty {
                val runs = title.optJSONArray("runs") ?: JSONArray()
                (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
            }
        }.orEmpty()
        if (title.isEmpty()) return null
        val length = renderer.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
        return Video(Song(id, title), seconds(length))
    }

    /** Visits every object in the tree (but doesn't look inside a video's own details). */
    private fun walk(node: Any?, visit: (String, JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> for (key in node.keys()) {
                val child = node.get(key)
                if (child is JSONObject) visit(key, child)
                if (key != "videoRenderer") walk(child, visit)
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.get(i), visit)
        }
    }
}
