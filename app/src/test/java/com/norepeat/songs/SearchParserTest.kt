package com.norepeat.songs

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchParserTest {

    /** Same shape as a real first-page response from youtubei/v1/search. */
    private val firstPage = """
        {"contents":{"twoColumnSearchResultsRenderer":{"primaryContents":{"sectionListRenderer":{"contents":[
          {"itemSectionRenderer":{"contents":[
            {"videoRenderer":{"videoId":"cWMxCE2HTag",
              "title":{"runs":[{"text":"SOFTLY (Official Music Video) KARAN AUJLA | IKKY"}]},
              "lengthText":{"accessibility":{"accessibilityData":{"label":"2 minutes, 51 seconds"}},"simpleText":"2:51"},
              "menu":{"menuRenderer":{"items":[]}}}},
            {"videoRenderer":{"videoId":"liveStream1",
              "title":{"runs":[{"text":"Punjabi Radio "},{"text":"LIVE 24/7"}]},
              "badges":[{"metadataBadgeRenderer":{"label":"LIVE"}}]}},
            {"shelfRenderer":{"content":{"verticalListRenderer":{"items":[
              {"videoRenderer":{"videoId":"Pd1yGW_g2T8","title":{"simpleText":"Ashke (Music Video) Karan Aujla"},
                "lengthText":{"simpleText":"1:03:38"}}}
            ]}}}},
            {"reelShelfRenderer":{"items":[{"shortsLockupViewModel":{"entityId":"short"}}]}}
          ]}},
          {"continuationItemRenderer":{"trigger":"CONTINUATION_TRIGGER_ON_ITEM_SHOWN",
            "continuationEndpoint":{"continuationCommand":{"token":"PAGE2TOKEN","request":"CONTINUATION_REQUEST_TYPE_SEARCH"}}}}
        ]}}}},
        "header":{"searchHeaderRenderer":{"chipBar":{"chipCloudChipRenderer":{"navigationEndpoint":
          {"continuationCommand":{"token":"NOT_THE_NEXT_PAGE"}}}}}}}
    """.trimIndent()

    /** Same shape as a "load more" response. */
    private val lastPage = """
        {"onResponseReceivedCommands":[{"appendContinuationItemsAction":{"continuationItems":[
          {"itemSectionRenderer":{"contents":[
            {"videoRenderer":{"videoId":"XTp5jaRU3Ws","title":{"runs":[{"text":"WAVY (OFFICIAL VIDEO) KARAN AUJLA"}]},
              "lengthText":{"simpleText":"2:41"}}}
          ]}}
        ]}}]}
    """.trimIndent()

    @Test
    fun readsVideosFromFirstPageIncludingShelves() {
        val page = SearchParser.parse(JSONObject(firstPage))
        assertEquals(
            listOf(
                Video(Song("cWMxCE2HTag", "SOFTLY (Official Music Video) KARAN AUJLA | IKKY"), 171),
                Video(Song("liveStream1", "Punjabi Radio LIVE 24/7"), 0),
                Video(Song("Pd1yGW_g2T8", "Ashke (Music Video) Karan Aujla"), 3818),
            ),
            page.videos,
        )
    }

    @Test
    fun readsNextPageTokenOnlyFromContinuationItem() {
        assertEquals("PAGE2TOKEN", SearchParser.parse(JSONObject(firstPage)).nextPageToken)
    }

    @Test
    fun readsContinuationPageAndDetectsEnd() {
        val page = SearchParser.parse(JSONObject(lastPage))
        assertEquals(listOf("XTp5jaRU3Ws"), page.videos.map { it.song.id })
        assertNull(page.nextPageToken)
    }

    @Test
    fun parsesLengths() {
        assertEquals(225, SearchParser.seconds("3:45"))
        assertEquals(3723, SearchParser.seconds("1:02:03"))
        assertEquals(0, SearchParser.seconds(""))
        assertEquals(0, SearchParser.seconds("LIVE"))
    }
}
