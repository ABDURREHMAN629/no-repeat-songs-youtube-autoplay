package com.norepeat.songs

/**
 * Turns a YouTube title into a set of "meaningful" words, so different uploads of the
 * same song look alike:
 *   "Softly (Official Video) | Karan Aujla"  -> {softly, karan, aujla}
 *   "Karan Aujla - Softly [Lyrics]"          -> {karan, aujla, softly}
 */
object SongKey {
    // Every bracket escaped: Android's regex engine (ICU) rejects a bare "}" that desktop Java allows.
    private val BRACKETS = Regex("""\(.*?\)|\[.*?\]|\{.*?\}""")
    private val SEPARATORS = Regex("""[^\p{L}\p{M}\p{N}]+""")
    private val YEAR = Regex("""(19|20)\d\d""")
    private val NOISE = setOf(
        "official", "video", "videos", "music", "audio", "lyrical", "lyrics", "lyric",
        "full", "song", "songs", "hd", "4k", "hq", "new", "latest", "mv", "visualizer",
        "visualiser", "ft", "feat", "featuring", "x", "the", "by", "and", "with", "prod",
        "presents", "watched", "punjabi", "hindi", "version",
    )

    fun of(title: String): Set<String> =
        BRACKETS.replace(title.lowercase(), " ")
            .split(SEPARATORS)
            .filter { it.isNotEmpty() && it !in NOISE && !YEAR.matches(it) }
            .toSet()
}

/**
 * Remembers song titles and answers "have I heard this song already?", even when the
 * title is written differently. Two titles count as the same song when their word sets
 * are equal, or when one set (of at least 2 words) is fully inside the other.
 */
class SongMemory {
    private val keys = ArrayList<Set<String>>()
    private val index = HashMap<String, MutableList<Int>>()

    fun add(title: String) {
        val key = SongKey.of(title)
        if (key.isEmpty()) return
        val position = keys.size
        keys.add(key)
        for (word in key) index.getOrPut(word) { ArrayList() }.add(position)
    }

    fun matches(title: String): Boolean {
        val key = SongKey.of(title)
        if (key.isEmpty()) return false
        val checked = HashSet<Int>()
        for (word in key) {
            for (position in index[word] ?: continue) {
                if (!checked.add(position)) continue
                if (sameSong(key, keys[position])) return true
            }
        }
        return false
    }

    fun clear() {
        keys.clear()
        index.clear()
    }

    private fun sameSong(a: Set<String>, b: Set<String>): Boolean {
        if (a == b) return true
        val (small, big) = if (a.size <= b.size) a to b else b to a
        return small.size >= 2 && big.containsAll(small)
    }
}
