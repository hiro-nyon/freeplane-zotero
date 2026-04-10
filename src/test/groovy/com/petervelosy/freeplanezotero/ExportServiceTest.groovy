package com.petervelosy.freeplanezotero

import com.petervelosy.freeplanezotero.support.FakeMap
import com.petervelosy.freeplanezotero.support.FakeNode
import org.junit.Test

import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_AUTHOR_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_CITATION_TEXT_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_ITEM_KEYS
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_NODE_TYPE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_READ_STATUS
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_RELATION_ROLE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_TITLE_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_YEAR_CACHE
import static org.junit.Assert.assertTrue

class ExportServiceTest {

    @Test
    void rendersMarkdownCsvAndItemKeys() {
        def schema = new CitationSchema()
        def exportService = new ExportService(schema)
        def map = new FakeMap()
        def root = new FakeNode("Root", map, true)
        map.root = root
        def paper = root.createChild("Paper")
        paper[NODE_ATTRIBUTE_TITLE_CACHE] = "Deep Learning"
        paper[NODE_ATTRIBUTE_CITATION_TEXT_CACHE] = "LeCun, 2015"
        paper[NODE_ATTRIBUTE_AUTHOR_CACHE] = "LeCun, Yann"
        paper[NODE_ATTRIBUTE_YEAR_CACHE] = "2015"
        paper[NODE_ATTRIBUTE_ITEM_KEYS] = "ABC123"
        paper[NODE_ATTRIBUTE_NODE_TYPE] = "paper"
        paper[NODE_ATTRIBUTE_READ_STATUS] = "reviewed"
        paper[NODE_ATTRIBUTE_RELATION_ROLE] = "supports"

        def markdown = exportService.renderMarkdownOutline(root)
        def csv = exportService.renderCsv([paper])
        def itemKeys = exportService.renderItemKeys([paper])

        assertTrue(markdown.contains("- Root"))
        assertTrue(markdown.contains("Deep Learning [LeCun, 2015]"))
        assertTrue(csv.contains("\"Deep Learning\""))
        assertTrue(csv.contains("\"supports\""))
        assertTrue(itemKeys.contains("ABC123"))
    }
}
