package com.petervelosy.freeplanezotero

import static Constants.*

class MigrationService {

    private final CitationSchema schema
    private final NodeMetadataService metadataService
    private final def logger

    MigrationService(CitationSchema schema, NodeMetadataService metadataService, logger) {
        this.schema = schema
        this.metadataService = metadataService
        this.logger = logger
    }

    boolean migrateIfNeeded(controller, map) {
        def nodes = controller.findAll()
        def legacyNodes = nodes.findAll { node ->
            schema.hasLegacyCitation(node) && !schema.nodeUsesCurrentSchema(node)
        }

        if (schema.mapUsesCurrentSchema(map) && legacyNodes.isEmpty()) {
            return false
        }

        logger?.info("Migrating ${legacyNodes.size()} legacy Zotero node(s) to schema ${SCHEMA_VERSION}.")
        legacyNodes.each { migrateLegacyNode(it) }
        nodes.each { metadataService.initializeNode(it) }
        schema.markMapSchemaVersion(map)
        true
    }

    void migrateLegacyNode(node) {
        def rawCode = schema.getNodeAttr(node, NODE_ATTRIBUTE_CITATIONS)
        def parsedText = schema.parseLegacyNodeText(node.text?.toString())
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE)) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, parsedText.title)
        }
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_CITATION_TEXT_CACHE) && parsedText.citation) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_CITATION_TEXT_CACHE, parsedText.citation)
        }
        if (rawCode && !schema.getNodeAttr(node, NODE_ATTRIBUTE_CSL_CODE_RAW)) {
            metadataService.refreshFromFieldCode(node, rawCode)
        } else {
            metadataService.refreshPresentation(node)
        }
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE) && rawCode) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE, "paper")
        }
        schema.markNodeSchemaVersion(node)
    }
}
