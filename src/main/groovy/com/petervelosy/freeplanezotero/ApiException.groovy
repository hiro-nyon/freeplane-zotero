package com.petervelosy.freeplanezotero

class ApiException extends Exception {
    final int statusCode
    final String responseBody

    ApiException(String message) {
        super(message)
        this.statusCode = -1
        this.responseBody = message
    }

    ApiException(int statusCode, String responseBody) {
        super("HTTP ${statusCode}: ${responseBody}")
        this.statusCode = statusCode
        this.responseBody = responseBody
    }
}
