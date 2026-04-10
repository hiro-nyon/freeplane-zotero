package com.petervelosy.freeplanezotero

import static Constants.*

class ReviewTemplateService {

    private final CitationSchema schema
    private final NodeMetadataService metadataService

    ReviewTemplateService(CitationSchema schema, NodeMetadataService metadataService) {
        this.schema = schema
        this.metadataService = metadataService
    }

    void createTemplate(targetNode) {
        def templateRoot = resolveTemplateRoot(targetNode)
        ensureChild(templateRoot, "Research Question", "note")
        ensureChild(templateRoot, "Topics", "topic")
        ensureChild(templateRoot, "Methods", "method")
        ensureChild(templateRoot, "Claims", "claim")
        ensureChild(templateRoot, "Papers", "paper")
        ensureChild(templateRoot, "Reading Queue", "paper", "queued")
        ensureChild(templateRoot, "Synthesis", "note")
        metadataService.refreshPresentation(templateRoot)
    }

    private Object resolveTemplateRoot(targetNode) {
        if (targetNode.root && !targetNode.children) {
            schema.setNodeAttr(targetNode, NODE_ATTRIBUTE_TITLE_CACHE, targetNode.text?.toString() ?: "Literature Review")
            return targetNode
        }
        targetNode.createChild("Literature Review")
    }

    private void ensureChild(parent, String title, String nodeType, String readStatus = null) {
        def existing = parent.children.find { it.text?.toString() == title }
        def child = existing ?: parent.createChild(title)
        schema.setNodeAttr(child, NODE_ATTRIBUTE_TITLE_CACHE, title)
        schema.setNodeAttr(child, NODE_ATTRIBUTE_NODE_TYPE, nodeType)
        schema.setNodeAttr(child, NODE_ATTRIBUTE_READ_STATUS, readStatus)
        metadataService.refreshPresentation(child)
    }
}
