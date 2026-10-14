package com.norepeat.songs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMemoryTest {

    @Test
    fun keyDropsBracketsNoiseWordsAndYears() {
        assertEquals(
            setOf("softly", "karan", "aujla"),
            SongKey.of("Softly (Official Video) | Karan Aujla | Latest Punjabi Songs 2023"),
        )
    }

    @Test
    fun keyKeepsGurmukhiWordsWhole() {
        assertEquals(setOf("ਸੋਹਣਿਆ"), SongKey.of("ਸੋਹਣਿਆ (Official Video)"))
    }

    @Test
    fun sameSongWithDifferentUploadTitlesMatches() {
        val memory = SongMemory()
        memory.add("Softly (Official Video) | Karan Aujla | Ikky | Making Memories")
        assertTrue(memory.matches("Karan Aujla - Softly [Lyrics]"))
        assertTrue(memory.matches("SOFTLY - KARAN AUJLA (Official Audio)"))
    }

    @Test
    fun watchedPrefixFromTakeoutIsIgnored() {
        val memory = SongMemory()
        memory.add("Watched Lover (Official Music Video) Diljit Dosanjh")
        assertTrue(memory.matches("Diljit Dosanjh - Lover"))
    }

    @Test
    fun differentSongsBySameArtistDoNotMatch() {
        val memory = SongMemory()
        memory.add("Karan Aujla - Softly")
        assertFalse(memory.matches("Karan Aujla - Winning Speech"))
        assertFalse(memory.matches("Admirin You | Karan Aujla"))
    }

    @Test
    fun singleWordTitlesNeedExactMatch() {
        val memory = SongMemory()
        memory.add("Softly")
        assertTrue(memory.matches("Softly (Official Video)"))
        assertFalse(memory.matches("Softly - Some Other Artist"))
    }

    @Test
    fun clearForgetsEverything() {
        val memory = SongMemory()
        memory.add("Karan Aujla - Softly")
        memory.clear()
        assertFalse(memory.matches("Karan Aujla - Softly"))
    }
}
