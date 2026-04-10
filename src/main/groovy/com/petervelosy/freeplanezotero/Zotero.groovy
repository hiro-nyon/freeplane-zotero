package com.petervelosy.freeplanezotero

import org.apache.commons.text.StringEscapeUtils

import javax.swing.JOptionPane
import java.awt.Desktop
import java.net.URI

import static Constants.*

class Zotero {

    private final def ui
    private final def logger
    private final def controller
    private final def map
    private final CitationSchema citationSchema
    private final NodeMetadataService nodeMetadataService
    private final MigrationService migrationService
    private final ReviewTemplateService reviewTemplateService
    private final ExportService exportService
    private final ZoteroClient zoteroClient

    Zotero(ui, logger, controller, map) {
        this.ui = ui
        this.logger = logger
        this.controller = controller
        this.map = map
        this.citationSchema = new CitationSchema()
        this.nodeMetadataService = new NodeMetadataService(citationSchema, logger)
        this.migrationService = new MigrationService(citationSchema, nodeMetadataService, logger)
        this.reviewTemplateService = new ReviewTemplateService(citationSchema, nodeMetadataService)
        this.exportService = new ExportService(citationSchema)
        this.zoteroClient = new ZoteroClient(logger)
    }

    void attachCitation(node) {
        withErrorDialogs {
            migrateIfNeeded()
            ensureDocument()
            executeProtocol("addEditCitation", node) { [node] }
        }
    }

    void refreshSelectedNode(node) {
        withErrorDialogs {
            migrateIfNeeded()
            ensureDocument()
            executeProtocol("refresh", node) { [node] }
        }
    }

    void refreshSelectedSubtree(node) {
        withErrorDialogs {
            migrateIfNeeded()
            ensureDocument()
            executeProtocol("refresh", node) { collectSubtree(node).findAll { citationSchema.getFieldCode(it) } }
        }
    }

    void refreshWholeMap(node) {
        withErrorDialogs {
            migrateIfNeeded()
            ensureDocument()
            executeProtocol("refresh", node) { controller.findAll().findAll { citationSchema.getFieldCode(it) } }
        }
    }

    void removeCitation(node) {
        withErrorDialogs {
            migrateIfNeeded()
            nodeMetadataService.clearCitation(node)
        }
    }

    void convertToPaperCard(node) {
        withErrorDialogs {
            migrateIfNeeded()
            nodeMetadataService.convertToPaperCard(node)
        }
    }

    void createLiteratureReviewTemplate(node) {
        withErrorDialogs {
            migrateIfNeeded()
            reviewTemplateService.createTemplate(node)
            showInfo("Literature review template sections were created on the current branch.")
        }
    }

    void openLinkedItemInZotero(node) {
        withErrorDialogs {
            migrateIfNeeded()
            def itemKeys = citationSchema.parseItemKeys(citationSchema.getNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS))
            def link = citationSchema.generateLocalZoteroLink(itemKeys)
            if (!link) {
                throw new ZoteroIntegrationException("The selected node does not have linked Zotero item keys.")
            }
            node.link.setUri(new URI(link))
            if (Desktop.isDesktopSupported()) {
                Desktop.desktop.browse(new URI(link))
            } else {
                showInfo("Zotero link copied to the node, but this platform does not support launching it automatically.")
            }
        }
    }

    void checkHealth() {
        withErrorDialogs {
            def result = zoteroClient.healthCheck()
            showInfo("Zotero connector is reachable (HTTP ${result.statusCode}).")
        }
    }

    void filterNodes(String field, String value) {
        withErrorDialogs {
            migrateIfNeeded()
            nodeMetadataService.filterMatches(map.root, field, value)
            showInfo("Filtered the map by ${field} = ${value}. Use \"Clear visual filters/highlights\" to reset the view.")
        }
    }

    void highlightNodes(String field, String value) {
        withErrorDialogs {
            migrateIfNeeded()
            nodeMetadataService.highlightMatches(map.root, field, value)
            showInfo("Highlighted nodes with ${field} = ${value}.")
        }
    }

    void clearVisualMarkers() {
        withErrorDialogs {
            migrateIfNeeded()
            nodeMetadataService.clearFiltersAndHighlights(map.root)
        }
    }

    void exportSelectedSubtreeToMarkdown(node) {
        withErrorDialogs {
            migrateIfNeeded()
            def content = exportService.renderMarkdownOutline(node)
            def file = exportService.writeExport(ui, "literature-outline", "md", content)
            announceExport(file, "Markdown outline copied to the clipboard.")
        }
    }

    void exportSelectedPapersToCsv() {
        withErrorDialogs {
            migrateIfNeeded()
            def selectedPaperNodes = selectedNodes().findAll { selected ->
                (citationSchema.getNodeAttr(selected, NODE_ATTRIBUTE_NODE_TYPE) ?: "note") == "paper"
            }
            def content = exportService.renderCsv(selectedPaperNodes)
            def file = exportService.writeExport(ui, "selected-papers", "csv", content)
            announceExport(file, "CSV copied to the clipboard.")
        }
    }

    void exportSelectedItemKeys() {
        withErrorDialogs {
            migrateIfNeeded()
            def content = exportService.renderItemKeys(selectedNodes())
            def file = exportService.writeExport(ui, "selected-item-keys", "txt", content)
            announceExport(file, "Item keys copied to the clipboard.")
        }
    }

    String getDocumentProperty(String key, node) {
        citationSchema.getMapStorage(node.mindMap, key)
    }

    def propertiesToObj(properties) {
        def result = [:]
        properties.keySet().each {
            result[it] = properties[it]
        }
        result
    }

    private void executeProtocol(String command, selectedNode, Closure<Collection> scopeSupplier) {
        zoteroClient.executeSession(command, citationSchema.ensureDocumentId(map)) { response ->
            handleProtocolCommand(response, selectedNode, scopeSupplier)
        }
        scopeSupplier.call().each { nodeMetadataService.refreshPresentation(it) }
    }

    private Map handleProtocolCommand(response, selectedNode, Closure<Collection> scopeSupplier) {
        switch (response.command) {
            case "Application.getActiveDocument":
                return [payload: [documentID: citationSchema.ensureDocumentId(map), outputFormat: "html", supportedNotes: []]]
            case "Document.getDocumentData":
                return [payload: [dataString: citationSchema.ensureDocumentData(map)]]
            case "Document.setDocumentData":
                citationSchema.setMapStorage(map, STORAGE_KEY_DOCUMENT_DATA, response.arguments[1])
                return [payload: null]
            case "Document.cursorInField":
                return [payload: citationSchema.buildFieldDescriptor(selectedNode)]
            case "Document.canInsertField":
                return [payload: true]
            case "Document.insertField":
                nodeMetadataService.refreshFromFieldCode(selectedNode, "{}")
                return [payload: citationSchema.buildFieldDescriptor(selectedNode)]
            case "Document.getFields":
                def fields = scopeSupplier.call().collect { citationSchema.buildFieldDescriptor(it) }.findAll { it }
                return [payload: fields]
            case "Document.displayAlert":
                return [payload: showZoteroDialog(response.arguments)]
            case "Document.activate":
                return [payload: null]
            case "Document.complete":
                return [terminal: true, status: "completed"]
            case "Field.select":
                def fieldNode = map.node(response.arguments[1])
                if (fieldNode != null) {
                    controller.select(fieldNode)
                }
                return [payload: null]
            case "Field.delete":
                nodeMetadataService.clearCitation(map.node(response.arguments[1]))
                return [payload: null]
            case "Field.removeCode":
                nodeMetadataService.clearCitation(map.node(response.arguments[1]))
                return [payload: null]
            case "Field.setCode":
                nodeMetadataService.refreshFromFieldCode(map.node(response.arguments[1]), response.arguments[2]?.toString())
                return [payload: null]
            case "Field.getText":
                return [payload: citationSchema.getCitationDisplayText(map.node(response.arguments[1]))]
            case "Field.setText":
                def newCitationText = response.arguments[2]?.toString() ?: ""
                if (response.arguments[3]) {
                    newCitationText = StringEscapeUtils.unescapeHtml4(newCitationText)
                }
                nodeMetadataService.updateCitationText(map.node(response.arguments[1]), newCitationText)
                return [payload: null]
            default:
                throw new ZoteroIntegrationException("Zotero sent the unsupported protocol command '${response.command}'.")
        }
    }

    private int showZoteroDialog(arguments) {
        def message = arguments[1]?.toString() ?: ""
        def icon = zoteroToJavaIconId(arguments[2] as Integer)
        def buttons = arguments[3] as Integer
        switch (buttons) {
            case ZOTERO_DIALOG_BUTTONS_OK_CANCEL:
                return JOptionPane.showConfirmDialog(null, message, "Zotero", JOptionPane.OK_CANCEL_OPTION, icon) == JOptionPane.OK_OPTION ? 1 : 0
            case ZOTERO_DIALOG_BUTTONS_YES_NO:
                return JOptionPane.showConfirmDialog(null, message, "Zotero", JOptionPane.YES_NO_OPTION, icon) == JOptionPane.YES_OPTION ? 1 : 0
            case ZOTERO_DIALOG_BUTTONS_YES_NO_CANCEL:
                def result = JOptionPane.showConfirmDialog(null, message, "Zotero", JOptionPane.YES_NO_CANCEL_OPTION, icon)
                switch (result) {
                    case JOptionPane.YES_OPTION:
                        return 2
                    case JOptionPane.NO_OPTION:
                        return 1
                    default:
                        return 0
                }
            default:
                JOptionPane.showMessageDialog(null, message, "Zotero", icon)
                return 1
        }
    }

    private int zoteroToJavaIconId(int iconId) {
        switch (iconId) {
            case ZOTERO_DIALOG_ICON_STOP:
                return JOptionPane.ERROR_MESSAGE
            case ZOTERO_DIALOG_ICON_CAUTION:
                return JOptionPane.WARNING_MESSAGE
            default:
                return JOptionPane.INFORMATION_MESSAGE
        }
    }

    private void ensureDocument() {
        citationSchema.ensureDocumentId(map)
        citationSchema.ensureDocumentData(map)
        citationSchema.markMapSchemaVersion(map)
    }

    private void migrateIfNeeded() {
        migrationService.migrateIfNeeded(controller, map)
    }

    private List collectSubtree(node) {
        def result = [node]
        node.children.each { child ->
            result.addAll(collectSubtree(child))
        }
        result
    }

    private Collection selectedNodes() {
        def selecteds = controller.selecteds
        selecteds instanceof Collection ? selecteds : [controller.selected]
    }

    private void announceExport(File exportedFile, String clipboardFallbackMessage) {
        if (exportedFile) {
            showInfo("Exported to ${exportedFile.absolutePath} and copied the contents to the clipboard.")
        } else {
            showInfo(clipboardFallbackMessage)
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(null, message, "Freeplane Zotero", JOptionPane.INFORMATION_MESSAGE)
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Freeplane Zotero", JOptionPane.ERROR_MESSAGE)
    }

    private void withErrorDialogs(Closure<?> work) {
        try {
            work.call()
        } catch (ZoteroIntegrationException e) {
            logger?.warn(e.message, e)
            showError(e.message)
        } catch (ApiException e) {
            logger?.warn(e.message, e)
            showError("Zotero returned an unexpected HTTP response: ${e.message}")
        } catch (Exception e) {
            logger?.warn(e.message, e)
            showError("Unexpected error: ${e.message}")
        }
    }
}
