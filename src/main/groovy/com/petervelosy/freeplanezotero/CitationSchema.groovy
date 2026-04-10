package com.petervelosy.freeplanezotero

import groovy.json.JsonSlurper

import static Constants.*

class CitationSchema {

    private final JsonSlurper jsonSlurper = new JsonSlurper()

    String getNodeAttr(node, String key) {
        node[key]?.toString()
    }

    void setNodeAttr(node, String key, Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            node.putAt(key, null)
        } else {
            node.putAt(key, value.toString())
        }
    }

    String getMapStorage(map, String key) {
        map.storage[key]?.toString()
    }

    void setMapStorage(map, String key, Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            map.storage.remove(key)
        } else {
            map.storage[key] = value.toString()
        }
    }

    void markMapSchemaVersion(map) {
        setMapStorage(map, STORAGE_KEY_SCHEMA_VERSION, SCHEMA_VERSION)
    }

    void markNodeSchemaVersion(node) {
        setNodeAttr(node, NODE_ATTRIBUTE_SCHEMA_VERSION, SCHEMA_VERSION)
    }

    boolean mapUsesCurrentSchema(map) {
        getMapStorage(map, STORAGE_KEY_SCHEMA_VERSION) == SCHEMA_VERSION
    }

    boolean nodeUsesCurrentSchema(node) {
        getNodeAttr(node, NODE_ATTRIBUTE_SCHEMA_VERSION) == SCHEMA_VERSION
    }

    boolean hasLegacyCitation(node) {
        getNodeAttr(node, NODE_ATTRIBUTE_CITATIONS) != null
    }

    boolean hasCanonicalCitation(node) {
        [
            NODE_ATTRIBUTE_CSL_CODE_RAW,
            NODE_ATTRIBUTE_CITATION_TEXT_CACHE,
            NODE_ATTRIBUTE_ITEM_KEYS
        ].any { getNodeAttr(node, it) }
    }

    boolean hasAnyCitation(node) {
        hasCanonicalCitation(node) || hasLegacyCitation(node)
    }

    String getDocumentId(map) {
        getMapStorage(map, STORAGE_KEY_DOCUMENT_ID)
    }

    String ensureDocumentId(map) {
        def existing = getDocumentId(map)
        if (existing) {
            return existing
        }
        def created = UUID.randomUUID().toString()
        setMapStorage(map, STORAGE_KEY_DOCUMENT_ID, created)
        if (getMapStorage(map, STORAGE_KEY_DOCUMENT_DATA) == null) {
            setMapStorage(map, STORAGE_KEY_DOCUMENT_DATA, "")
        }
        created
    }

    String ensureDocumentData(map) {
        def existing = getMapStorage(map, STORAGE_KEY_DOCUMENT_DATA)
        if (existing != null) {
            return existing
        }
        setMapStorage(map, STORAGE_KEY_DOCUMENT_DATA, "")
        ""
    }

    Map parseLegacyNodeText(String nodeText) {
        def raw = nodeText ?: ""
        def matcher = raw =~ /([^\[\]]+?)(?:\s+\[(.*)\])?$/
        if (matcher.matches()) {
            return [
                title   : matcher[0][1]?.trim() ?: raw,
                citation: matcher[0][2]?.trim() ?: ""
            ]
        }
        [title: raw, citation: ""]
    }

    Object parseCslFieldCode(String fieldCode) {
        if (!fieldCode?.startsWith(FIELD_CODE_PREFIX_CSL)) {
            return null
        }
        def jsonPartStr = fieldCode.substring(FIELD_CODE_PREFIX_CSL.length())
        jsonSlurper.parseText(jsonPartStr)
    }

    List<String> extractItemKeysFromCsl(csl) {
        if (!csl?.citationItems) {
            return []
        }
        csl.citationItems.collectMany { item ->
            (item?.uris ?: []).collect { uri ->
                uri?.toString()?.tokenize("/")?.last()
            }
        }.findAll { it }.unique()
    }

    String extractTitleCache(csl, String fallback = null) {
        def titles = csl?.citationItems?.collect { item ->
            item?.itemData?.title ?: item?.itemData?.shortTitle
        }?.findAll { it }?.unique()
        if (titles) {
            return titles.join("; ")
        }
        fallback
    }

    String extractAuthorCache(csl) {
        def authors = csl?.citationItems?.collect { item ->
            formatCreators(item?.itemData)
        }?.findAll { it }?.unique()
        authors ? authors.join(" ; ") : null
    }

    String extractYearCache(csl) {
        def years = csl?.citationItems?.collect { item ->
            extractIssuedYear(item?.itemData)
        }?.findAll { it }?.unique()
        years ? years.join(", ") : null
    }

    String getRenderedTitle(node) {
        getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE) ?: parseLegacyNodeText(node.text?.toString()).title
    }

    String getCitationDisplayText(node) {
        getNodeAttr(node, NODE_ATTRIBUTE_CITATION_TEXT_CACHE) ?: ""
    }

    String renderNodeText(node) {
        def title = getRenderedTitle(node) ?: ""
        def citationText = getCitationDisplayText(node)
        citationText ? "${title} [${citationText}]" : title
    }

    String getFieldCode(node) {
        getNodeAttr(node, NODE_ATTRIBUTE_CSL_CODE_RAW) ?: getNodeAttr(node, NODE_ATTRIBUTE_CITATIONS)
    }

    Map buildFieldDescriptor(node) {
        def fieldCode = getFieldCode(node)
        if (!fieldCode) {
            return null
        }
        [
            text     : getCitationDisplayText(node),
            code     : fieldCode,
            id       : node.id,
            noteIndex: 0
        ]
    }

    List<String> parseItemKeys(String itemKeysText) {
        if (!itemKeysText) {
            return []
        }
        itemKeysText.split(/\s*,\s*/).findAll { it }
    }

    String joinItemKeys(Collection<String> itemKeys) {
        itemKeys?.findAll { it }?.unique()?.join(",")
    }

    String generateLocalZoteroLink(Collection<String> itemKeys) {
        def keys = itemKeys?.findAll { it }?.unique()
        if (!keys) {
            return null
        }
        "zotero://select/library/items?itemKey=${keys.join(',')}"
    }

    boolean isZoteroLink(linkText) {
        linkText?.toString()?.startsWith("zotero://")
    }

    private String formatCreators(itemData) {
        def creators = itemData?.author ?: itemData?.authors ?: itemData?.creator ?: itemData?.creators
        if (!(creators instanceof Collection)) {
            return null
        }
        def names = creators.collect { creator ->
            creator?.literal ?: [creator?.family, creator?.given].findAll { it }?.join(", ")
        }.findAll { it }
        names ? names.join("; ") : null
    }

    private String extractIssuedYear(itemData) {
        def dateParts = itemData?.issued?.get("date-parts")
        if (dateParts instanceof Collection && dateParts && dateParts[0] instanceof Collection && dateParts[0]) {
            return dateParts[0][0]?.toString()
        }
        def raw = itemData?.issued?.raw ?: itemData?.issued?.literal ?: itemData?.date
        def matcher = raw?.toString() =~ /\b(1[6-9]\d{2}|20\d{2}|21\d{2})\b/
        matcher?.find() ? matcher.group(1) : null
    }
}
