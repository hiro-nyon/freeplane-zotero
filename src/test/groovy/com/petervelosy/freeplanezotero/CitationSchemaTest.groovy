package com.petervelosy.freeplanezotero

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class CitationSchemaTest {

    @Test
    void parsesLegacyTextAndCslFieldCode() {
        def schema = new CitationSchema()
        def parsedLegacy = schema.parseLegacyNodeText("Deep Learning [LeCun, 2015]")
        assertEquals("Deep Learning", parsedLegacy.title)
        assertEquals("LeCun, 2015", parsedLegacy.citation)

        def csl = schema.parseCslFieldCode(sampleFieldCode())
        assertEquals(["ABC123", "DEF456"], schema.extractItemKeysFromCsl(csl))
        assertEquals("Deep Learning; Attention Is All You Need", schema.extractTitleCache(csl))
        assertTrue(schema.extractAuthorCache(csl).contains("LeCun"))
        assertEquals("2015, 2017", schema.extractYearCache(csl))
    }

    private static String sampleFieldCode() {
        'ITEM CSL_CITATION {"citationItems":[' +
            '{"uris":["http://zotero.org/users/local/items/ABC123"],"itemData":{"title":"Deep Learning","author":[{"family":"LeCun","given":"Yann"}],"issued":{"date-parts":[[2015]]}}},' +
            '{"uris":["http://zotero.org/users/local/items/DEF456"],"itemData":{"title":"Attention Is All You Need","author":[{"family":"Vaswani","given":"Ashish"}],"issued":{"date-parts":[[2017]]}}}' +
            '],"properties":{}}'
    }
}
