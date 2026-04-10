package com.petervelosy.freeplanezotero

import com.petervelosy.freeplanezotero.support.FakeController
import com.petervelosy.freeplanezotero.support.FakeMap
import com.petervelosy.freeplanezotero.support.FakeNode
import org.junit.Test

import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_CITATION_TEXT_CACHE
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_CSL_CODE_RAW
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_ITEM_KEYS
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_SCHEMA_VERSION
import static com.petervelosy.freeplanezotero.Constants.NODE_ATTRIBUTE_TITLE_CACHE
import static com.petervelosy.freeplanezotero.Constants.STORAGE_KEY_SCHEMA_VERSION
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class MigrationServiceTest {

    @Test
    void migratesLegacyNodesWithoutLosingVisibleCitation() {
        def map = new FakeMap()
        def root = new FakeNode("Root", map, true)
        map.root = root
        def node = root.createChild("Deep Learning [LeCun, 2015]")
        node["zotero_citations"] = sampleFieldCode()

        def controller = new FakeController(map)
        def schema = new CitationSchema()
        def metadata = new NodeMetadataService(schema, null)
        def migration = new MigrationService(schema, metadata, null)

        assertTrue(migration.migrateIfNeeded(controller, map))
        assertEquals("2", map.storage[STORAGE_KEY_SCHEMA_VERSION])
        assertEquals("2", node[NODE_ATTRIBUTE_SCHEMA_VERSION])
        assertEquals(sampleFieldCode(), node[NODE_ATTRIBUTE_CSL_CODE_RAW])
        assertEquals("ABC123", node[NODE_ATTRIBUTE_ITEM_KEYS])
        assertEquals("Deep Learning", node[NODE_ATTRIBUTE_TITLE_CACHE])
        assertEquals("LeCun, 2015", node[NODE_ATTRIBUTE_CITATION_TEXT_CACHE])
        assertEquals("Deep Learning [LeCun, 2015]", node.text)
    }

    private static String sampleFieldCode() {
        'ITEM CSL_CITATION {"citationItems":[{"uris":["http://zotero.org/users/local/items/ABC123"],"itemData":{"title":"Deep Learning","author":[{"family":"LeCun","given":"Yann"}],"issued":{"date-parts":[[2015]]}}}],"properties":{}}'
    }
}
