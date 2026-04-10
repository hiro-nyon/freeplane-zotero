package com.petervelosy.freeplanezotero

class ZoteroIntegrationException extends Exception {
    final String category

    ZoteroIntegrationException(String message) {
        super(message)
        this.category = "failed"
    }

    ZoteroIntegrationException(String message, String category) {
        super(message)
        this.category = category
    }

    ZoteroIntegrationException(String message, Throwable cause) {
        super(message, cause)
        this.category = "failed"
    }

    ZoteroIntegrationException(String message, String category, Throwable cause) {
        super(message, cause)
        this.category = category
    }
}
