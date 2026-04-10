package com.petervelosy.freeplanezotero

import java.net.URI

import static Constants.*

class NodeMetadataService {

    private final CitationSchema schema
    private final def logger

    NodeMetadataService(CitationSchema schema, logger) {
        this.schema = schema
        this.logger = logger
    }

    void initializeNode(node) {
        schema.markNodeSchemaVersion(node)
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE)) {
            def defaultType = schema.hasAnyCitation(node) ? "paper" : "note"
            schema.setNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE, defaultType)
        }
        if (schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE) == "paper" &&
            !schema.getNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS)) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS, "unread")
        }
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE)) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, schema.parseLegacyNodeText(node.text?.toString()).title)
        }
    }

    void refreshFromFieldCode(node, String fieldCode) {
        initializeNode(node)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CSL_CODE_RAW, fieldCode)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CITATIONS, fieldCode)

        def currentTitle = schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE) ?: schema.parseLegacyNodeText(node.text?.toString()).title
        def csl = schema.parseCslFieldCode(fieldCode)
        if (csl) {
            def itemKeys = schema.extractItemKeysFromCsl(csl)
            schema.setNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS, schema.joinItemKeys(itemKeys))
            schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, schema.extractTitleCache(csl, currentTitle))
            schema.setNodeAttr(node, NODE_ATTRIBUTE_AUTHOR_CACHE, schema.extractAuthorCache(csl))
            schema.setNodeAttr(node, NODE_ATTRIBUTE_YEAR_CACHE, schema.extractYearCache(csl))
            schema.setNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE, "paper")
        } else {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, currentTitle)
        }
        refreshPresentation(node)
    }

    void updateCitationText(node, String citationText) {
        initializeNode(node)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CITATION_TEXT_CACHE, citationText)
        refreshPresentation(node)
    }

    void clearCitation(node) {
        initializeNode(node)
        def currentTitle = schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE) ?: schema.parseLegacyNodeText(node.text?.toString()).title
        schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, currentTitle)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CSL_CODE_RAW, null)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CITATIONS, null)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_CITATION_TEXT_CACHE, null)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS, null)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_AUTHOR_CACHE, null)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_YEAR_CACHE, null)
        removeZoteroLink(node)
        refreshPresentation(node)
    }

    void convertToPaperCard(node) {
        initializeNode(node)
        schema.setNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE, "paper")
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS)) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS, "unread")
        }
        if (!schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE)) {
            schema.setNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE, schema.parseLegacyNodeText(node.text?.toString()).title)
        }
        refreshPresentation(node)
    }

    void refreshPresentation(node) {
        initializeNode(node)
        node.text = schema.renderNodeText(node)
        syncZoteroLink(node)
        applyDirectFormatting(node)
    }

    void applyDirectFormatting(node) {
        initializeNode(node)
        def nodeType = schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE) ?: "note"
        def readStatus = schema.getNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS) ?: ""
        def palette = paletteFor(nodeType)
        def accent = accentFor(readStatus)

        node.style.backgroundColorCode = palette.background
        node.style.textColorCode = accent.text
        node.style.border.usesEdgeColor = false
        node.style.border.colorCode = accent.border
        node.style.maxNodeWidth = "14 cm"

        replaceManagedIcons(node, readStatus)
    }

    void highlightMatches(node, String field, String value) {
        clearHighlightMarkers(node)
        traverse(node) { current ->
            def isMatch = matches(current, field, value)
            schema.setNodeAttr(current, NODE_ATTRIBUTE_HIGHLIGHT_MARKER, isMatch ? "true" : null)
            applyDirectFormatting(current)
        }
    }

    void filterMatches(rootNode, String field, String value) {
        clearFilterMarkers(rootNode)
        applyFilterRecursive(rootNode, field, value)
    }

    void clearFiltersAndHighlights(rootNode) {
        traverse(rootNode) { current ->
            schema.setNodeAttr(current, NODE_ATTRIBUTE_FILTER_MARKER, null)
            schema.setNodeAttr(current, NODE_ATTRIBUTE_HIGHLIGHT_MARKER, null)
            current.folded = false
            applyDirectFormatting(current)
        }
    }

    boolean matches(node, String field, String value) {
        def normalizedValue = value?.trim()?.toLowerCase()
        if (!normalizedValue) {
            return false
        }
        switch (field) {
            case "year":
                return (schema.getNodeAttr(node, NODE_ATTRIBUTE_YEAR_CACHE) ?: "").toLowerCase() == normalizedValue
            case "status":
                return (schema.getNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS) ?: "").toLowerCase() == normalizedValue
            case "type":
                return (schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE) ?: "").toLowerCase() == normalizedValue
            default:
                return false
        }
    }

    private void syncZoteroLink(node) {
        def itemKeys = schema.parseItemKeys(schema.getNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS))
        def linkText = schema.generateLocalZoteroLink(itemKeys)
        if (linkText) {
            node.link.setUri(new URI(linkText))
        } else {
            removeZoteroLink(node)
        }
    }

    private void removeZoteroLink(node) {
        if (schema.isZoteroLink(node.link?.uri?.toString())) {
            node.link.remove()
        }
    }

    private void replaceManagedIcons(node, String readStatus) {
        [
            "helpButton",
            "bookmark",
            "hourglass",
            "button_ok",
            "button_cancel"
        ].each { node.icons.remove(it) }

        def statusIcon = iconFor(readStatus)
        if (statusIcon) {
            node.icons.add(statusIcon)
        }
        if (schema.getNodeAttr(node, NODE_ATTRIBUTE_HIGHLIGHT_MARKER)) {
            node.icons.add("bookmark")
        }
    }

    private Map paletteFor(String nodeType) {
        switch (nodeType) {
            case "paper":
                return [background: "#e8f1fb"]
            case "topic":
                return [background: "#e8f5e9"]
            case "claim":
                return [background: "#fff3e0"]
            case "method":
                return [background: "#f3e5f5"]
            default:
                return [background: "#f4f4f4"]
        }
    }

    private Map accentFor(String readStatus) {
        switch (readStatus) {
            case "queued":
                return [text: "#145a9e", border: "#4f8fba"]
            case "reading":
                return [text: "#9a5b00", border: "#d89216"]
            case "reviewed":
                return [text: "#166534", border: "#2f855a"]
            default:
                return [text: "#333333", border: "#6b7280"]
        }
    }

    private String iconFor(String readStatus) {
        switch (readStatus) {
            case "queued":
                return "bookmark"
            case "reading":
                return "hourglass"
            case "reviewed":
                return "button_ok"
            default:
                return "helpButton"
        }
    }

    private boolean applyFilterRecursive(node, String field, String value) {
        def matchHere = matches(node, field, value)
        def descendantMatch = false
        node.children.each { child ->
            descendantMatch = applyFilterRecursive(child, field, value) || descendantMatch
        }
        def visible = node.root || matchHere || descendantMatch
        node.folded = !visible
        schema.setNodeAttr(node, NODE_ATTRIBUTE_FILTER_MARKER, visible ? null : "true")
        schema.setNodeAttr(node, NODE_ATTRIBUTE_HIGHLIGHT_MARKER, matchHere ? "true" : null)
        applyDirectFormatting(node)
        visible
    }

    private void clearFilterMarkers(node) {
        traverse(node) { current ->
            schema.setNodeAttr(current, NODE_ATTRIBUTE_FILTER_MARKER, null)
            current.folded = false
        }
    }

    private void clearHighlightMarkers(node) {
        traverse(node) { current ->
            schema.setNodeAttr(current, NODE_ATTRIBUTE_HIGHLIGHT_MARKER, null)
        }
    }

    private void traverse(node, Closure<?> visitor) {
        visitor(node)
        node.children.each { child ->
            traverse(child, visitor)
        }
    }
}
