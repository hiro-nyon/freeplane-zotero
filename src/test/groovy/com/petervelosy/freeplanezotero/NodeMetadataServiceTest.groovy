package com.petervelosy.freeplanezotero

import com.petervelosy.freeplanezotero.support.FakeMap
import com.petervelosy.freeplanezotero.support.FakeNode
import org.junit.Test

import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_CITATION_TEXT_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_ITEM_KEYS
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_NODE_TYPE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_READ_STATUS
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_TITLE_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_YEAR_CACHE
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class NodeMetadataServiceTest {

    @Test
    void appliesDeterministicFormattingAndFiltering() {
        def schema = new CitationSchema()
        def service = new NodeMetadataService(schema, null)
        def map = new FakeMap()
        def root = new FakeNode("Root", map, true)
        map.root = root
        def paper = root.createChild("Paper")
        paper[NODE_ATTRIBUTE_TITLE_CACHE] = "Deep Learning"
        paper[NODE_ATTRIBUTE_CITATION_TEXT_CACHE] = "LeCun, 2015"
        paper[NODE_ATTRIBUTE_ITEM_KEYS] = "ABC123"
        paper[NODE_ATTRIBUTE_NODE_TYPE] = "paper"
        paper[NODE_ATTRIBUTE_READ_STATUS] = "reading"
        paper[NODE_ATTRIBUTE_YEAR_CACHE] = "2015"

        service.refreshPresentation(paper)
        assertEquals("#e8f1fb", paper.style.backgroundColorCode)
        assertTrue(paper.icons.values.contains("hourglass"))

        service.highlightMatches(root, "year", "2015")
        assertTrue(paper.icons.values.contains("bookmark"))

        service.filterMatches(root, "type", "paper")
        assertEquals(false, paper.folded)
    }
}
