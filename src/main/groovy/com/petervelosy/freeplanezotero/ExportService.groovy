package com.petervelosy.freeplanezotero

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import static Constants.*

class ExportService {

    private final CitationSchema schema

    ExportService(CitationSchema schema) {
        this.schema = schema
    }

    String renderMarkdownOutline(rootNode) {
        def lines = []
        renderMarkdownRecursive(rootNode, 0, lines)
        lines.join(System.lineSeparator())
    }

    String renderCsv(Collection nodes) {
        def rows = [["title", "authors", "year", "item_keys", "node_type", "read_status", "relation_role", "node_path", "zotero_link"]]
        nodes.each { node ->
            rows << [
                schema.getNodeAttr(node, NODE_ATTRIBUTE_TITLE_CACHE) ?: schema.getRenderedTitle(node),
                schema.getNodeAttr(node, NODE_ATTRIBUTE_AUTHOR_CACHE) ?: "",
                schema.getNodeAttr(node, NODE_ATTRIBUTE_YEAR_CACHE) ?: "",
                schema.getNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS) ?: "",
                schema.getNodeAttr(node, NODE_ATTRIBUTE_NODE_TYPE) ?: "",
                schema.getNodeAttr(node, NODE_ATTRIBUTE_READ_STATUS) ?: "",
                schema.getNodeAttr(node, NODE_ATTRIBUTE_RELATION_ROLE) ?: "",
                buildNodePath(node),
                schema.generateLocalZoteroLink(schema.parseItemKeys(schema.getNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS)))
            ]
        }
        rows.collect { row ->
            row.collect { escapeCsv(it?.toString() ?: "") }.join(",")
        }.join(System.lineSeparator())
    }

    String renderItemKeys(Collection nodes) {
        nodes.collectMany { node ->
            schema.parseItemKeys(schema.getNodeAttr(node, NODE_ATTRIBUTE_ITEM_KEYS))
        }.findAll { it }.unique().join(System.lineSeparator())
    }

    File writeExport(ui, String baseFileName, String extension, String content) {
        def chooser = new JFileChooser()
        chooser.dialogTitle = "Export ${extension.toUpperCase()}"
        chooser.fileFilter = new FileNameExtensionFilter("${extension.toUpperCase()} files", extension)
        chooser.selectedFile = new File("${sanitizeFileName(baseFileName)}.${extension}")

        def result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) {
            copyToClipboard(content)
            return null
        }

        def target = chooser.selectedFile
        if (!target.name.toLowerCase().endsWith(".${extension}".toLowerCase())) {
            target = new File(target.parentFile ?: new File("."), "${target.name}.${extension}")
        }
        Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8))
        copyToClipboard(content)
        target
    }

    private void renderMarkdownRecursive(node, int depth, List<String> lines) {
        def indent = "  " * depth
        lines << "${indent}- ${schema.renderNodeText(node)}"
        node.children.each { child ->
            renderMarkdownRecursive(child, depth + 1, lines)
        }
    }

    private String buildNodePath(node) {
        def nodes = []
        def current = node
        while (current != null) {
            nodes << (schema.getRenderedTitle(current) ?: current.text?.toString() ?: "")
            current = current.parent
        }
        nodes.reverse().join(" / ")
    }

    private String escapeCsv(String value) {
        "\"${value.replace('"', '""')}\""
    }

    private void copyToClipboard(String content) {
        Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(content), null)
    }

    private String sanitizeFileName(String value) {
        (value ?: "zotero-export").replaceAll(/[^A-Za-z0-9._-]+/, "-")
    }
}
