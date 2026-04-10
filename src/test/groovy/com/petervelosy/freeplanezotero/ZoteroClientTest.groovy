package com.petervelosy.freeplanezotero

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Test

import java.nio.charset.StandardCharsets

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ZoteroClientTest {

    private HttpServer server

    @After
    void tearDown() {
        server?.stop(0)
    }

    @Test
    void completesNormalSessionAndCancelFlow() {
        def postedBodies = []
        server = HttpServer.create(new InetSocketAddress(0), 0)
        server.createContext("/connector/ping", jsonHandler(200, '{"status":"ok"}'))
        server.createContext("/connector/document/execCommand", jsonHandler(200, '{"command":"Document.displayAlert","arguments":[0,"Continue?",1,1]}'))
        server.createContext("/connector/document/respond", { HttpExchange exchange ->
            postedBodies << exchange.requestBody.text
            respond(exchange, 200, '{"command":"Document.complete"}')
        } as HttpHandler)
        server.start()

        def client = new ZoteroClient(null, connectorUrl(), 1, 1)
        def result = client.executeSession("refresh", "doc-1") { response ->
            if (response.command == "Document.displayAlert") {
                return [payload: 0]
            }
            [terminal: true, status: "completed"]
        }

        assertEquals("completed", result.status)
        assertTrue(postedBodies.first().contains("0"))
    }

    @Test(expected = ZoteroIntegrationException)
    void raisesLockedTransactionErrors() {
        server = HttpServer.create(new InetSocketAddress(0), 0)
        server.createContext("/connector/ping", jsonHandler(200, '{"status":"ok"}'))
        server.createContext("/connector/document/execCommand", jsonHandler(503, 'Integration transaction is already in progress'))
        server.start()

        new ZoteroClient(null, connectorUrl(), 1, 1).executeSession("refresh", "doc-1") { [terminal: true, status: "completed"] }
    }

    @Test(expected = ZoteroIntegrationException)
    void raisesMalformedJsonErrors() {
        server = HttpServer.create(new InetSocketAddress(0), 0)
        server.createContext("/connector/ping", jsonHandler(200, '{"status":"ok"}'))
        server.createContext("/connector/document/execCommand", jsonHandler(200, 'not-json'))
        server.start()

        new ZoteroClient(null, connectorUrl(), 1, 1).executeSession("refresh", "doc-1") { [terminal: true, status: "completed"] }
    }

    @Test(expected = ZoteroIntegrationException)
    void raisesTimeoutErrorsWhenRespondHangs() {
        server = HttpServer.create(new InetSocketAddress(0), 0)
        server.createContext("/connector/ping", jsonHandler(200, '{"status":"ok"}'))
        server.createContext("/connector/document/execCommand", jsonHandler(200, '{"command":"Document.getFields"}'))
        server.createContext("/connector/document/respond", { HttpExchange exchange ->
            Thread.sleep(1500)
            respond(exchange, 200, '{"command":"Document.complete"}')
        } as HttpHandler)
        server.start()

        new ZoteroClient(null, connectorUrl(), 1, 1).executeSession("refresh", "doc-1") { [payload: []] }
    }

    private HttpHandler jsonHandler(int statusCode, String body) {
        return { HttpExchange exchange ->
            respond(exchange, statusCode, body)
        } as HttpHandler
    }

    private void respond(HttpExchange exchange, int statusCode, String body) {
        def bytes = body.getBytes(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(statusCode, bytes.length)
        exchange.responseBody.withCloseable { output ->
            output.write(bytes)
        }
    }

    private String connectorUrl() {
        "http://127.0.0.1:${server.address.port}/connector"
    }
}
