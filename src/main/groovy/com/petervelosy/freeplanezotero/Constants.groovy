package com.petervelosy.freeplanezotero

import okhttp3.MediaType

class Constants {

    public static final String ZOTERO_CONNECTOR_URL = "http://127.0.0.1:23119/connector"
    public static final String EXEC_COMMAND_ENDPOINT = "/document/execCommand"
    public static final String RESPOND_ENDPOINT = "/document/respond"
    public static final String HEALTHCHECK_ENDPOINT = "/ping"

    public static final String STORAGE_KEY_DOCUMENT_ID = "zotero_document_id"
    public static final String STORAGE_KEY_DOCUMENT_DATA = "zotero_document_data"
    public static final String STORAGE_KEY_SCHEMA_VERSION = "zotero_schema_version"

    public static final String NODE_ATTRIBUTE_CITATIONS = "zotero_citations"
    public static final String NODE_ATTRIBUTE_ITEM_KEYS = "zotero_item_keys"
    public static final String NODE_ATTRIBUTE_CSL_CODE_RAW = "zotero_csl_code_raw"
    public static final String NODE_ATTRIBUTE_CITATION_TEXT_CACHE = "zotero_citation_text_cache"
    public static final String NODE_ATTRIBUTE_TITLE_CACHE = "zotero_title_cache"
    public static final String NODE_ATTRIBUTE_AUTHOR_CACHE = "zotero_author_cache"
    public static final String NODE_ATTRIBUTE_YEAR_CACHE = "zotero_year_cache"
    public static final String NODE_ATTRIBUTE_NODE_TYPE = "zotero_node_type"
    public static final String NODE_ATTRIBUTE_READ_STATUS = "zotero_read_status"
    public static final String NODE_ATTRIBUTE_RELATION_ROLE = "zotero_relation_role"
    public static final String NODE_ATTRIBUTE_SCHEMA_VERSION = "zotero_schema_version"
    public static final String NODE_ATTRIBUTE_FILTER_MARKER = "zotero_filter_hidden"
    public static final String NODE_ATTRIBUTE_HIGHLIGHT_MARKER = "zotero_highlight_marker"

    public static final String FIELD_CODE_PREFIX_CSL = "ITEM CSL_CITATION "
    public static final String SCHEMA_VERSION = "2"

    public static final int ZOTERO_DIALOG_ICON_STOP = 0
    public static final int ZOTERO_DIALOG_ICON_NOTICE = 1
    public static final int ZOTERO_DIALOG_ICON_CAUTION = 2

    public static final int ZOTERO_DIALOG_BUTTONS_OK = 0
    public static final int ZOTERO_DIALOG_BUTTONS_OK_CANCEL = 1
    public static final int ZOTERO_DIALOG_BUTTONS_YES_NO = 2
    public static final int ZOTERO_DIALOG_BUTTONS_YES_NO_CANCEL = 3

    public static final int CONNECT_TIMEOUT_SECONDS = 5
    public static final int READ_TIMEOUT_SECONDS = 60

    public static final List<String> NODE_TYPES = [
        "paper",
        "topic",
        "claim",
        "method",
        "note"
    ].asImmutable()

    public static final List<String> READ_STATUSES = [
        "unread",
        "queued",
        "reading",
        "reviewed"
    ].asImmutable()

    public static final List<String> RELATION_ROLES = [
        "supports",
        "contrasts",
        "extends",
        "questions",
        "cites"
    ].asImmutable()

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8")
}
