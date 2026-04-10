package com.petervelosy.freeplanezotero

import groovy.json.JsonSlurper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

import static Constants.*

class ZoteroClient {

    private final def logger
    private final String connectorUrl
    private final JsonSlurper jsonSlurper = new JsonSlurper()
    private final OkHttpClient client

    ZoteroClient(logger, String connectorUrl = ZOTERO_CONNECTOR_URL, Integer connectTimeoutSeconds = CONNECT_TIMEOUT_SECONDS,
                 Integer readTimeoutSeconds = READ_TIMEOUT_SECONDS, OkHttpClient client = null) {
        this.logger = logger
        this.connectorUrl = connectorUrl
        this.client = client ?: new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    Map healthCheck() {
        try {
            def request = new Request.Builder()
                .url(connectorUrl + HEALTHCHECK_ENDPOINT)
                .get()
                .build()
            def response = client.newCall(request).execute()
            def body = response.body()?.string() ?: ""
            logger?.info("Zotero health check returned ${response.code()}: ${body}")
            [ok: true, statusCode: response.code(), body: body]
        } catch (ConnectException e) {
            throw new ZoteroIntegrationException("Unable to connect to the Zotero HTTP API. Please ensure Zotero is running.", "failed", e)
        } catch (SocketTimeoutException e) {
            throw new ZoteroIntegrationException("Timed out while contacting Zotero. Please ensure Zotero is responsive and try again.", "timed_out", e)
        }
    }

    Map executeSession(String command, String docId, Closure<Map> responder) {
        healthCheck()
        def response = postJson(connectorUrl + EXEC_COMMAND_ENDPOINT, [command: command, docId: docId], true)
        while (true) {
            validateProtocolResponse(response)
            def step = responder.call(response) ?: [:]
            if (step.terminal) {
                return step
            }
            response = postJson(connectorUrl + RESPOND_ENDPOINT, step.payload, false)
        }
    }

    private Object postJson(String url, Object bodyObject, boolean transactionStart) {
        try {
            logger?.info("Request to URL ${url}: ${bodyObject}")
            def json = groovy.json.JsonOutput.toJson(bodyObject)
            def body = RequestBody.create(JSON, json)
            def request = new Request.Builder()
                .url(url)
                .post(body)
                .build()
            Response response = client.newCall(request).execute()
            def responseBody = response.body()?.string() ?: ""
            logger?.info("Response (${response.code()}): ${responseBody}")
            if (!response.successful) {
                if (response.code() == 503 && responseBody.contains("Integration transaction is already in progress")) {
                    throw new ZoteroIntegrationException(
                        "Zotero reports that an earlier integration transaction is still in progress. Restart Zotero and try again.",
                        "locked"
                    )
                }
                throw new ApiException(response.code(), responseBody)
            }
            try {
                return jsonSlurper.parseText(responseBody)
            } catch (Exception e) {
                throw new ZoteroIntegrationException("Zotero returned malformed JSON while processing ${transactionStart ? 'the transaction start' : 'a transaction step'}.", "failed", e)
            }
        } catch (ConnectException e) {
            throw new ZoteroIntegrationException("Unable to connect to the Zotero HTTP API. Please ensure Zotero is running.", "failed", e)
        } catch (SocketTimeoutException e) {
            def category = transactionStart ? "timed_out" : "timed_out"
            throw new ZoteroIntegrationException("Timed out while waiting for Zotero to respond. The Zotero transaction may need to be restarted.", category, e)
        }
    }

    private void validateProtocolResponse(response) {
        if (!(response instanceof Map) || !response.command) {
            throw new ZoteroIntegrationException("Zotero returned an incomplete protocol response.", "failed")
        }
    }
}
