// @ExecutionModes({on_single_node="//main_menu/extras/Zotero"})

import com.petervelosy.freeplanezotero.Constants
import com.petervelosy.freeplanezotero.Zotero

import javax.swing.JOptionPane

def chooseField() {
    def options = ["year", "status", "type"] as Object[]
    JOptionPane.showInputDialog(null, "Filter by:", "Zotero Filter", JOptionPane.QUESTION_MESSAGE, null, options, options[0])?.toString()
}

def chooseValue(String field) {
    if (field == "status") {
        return JOptionPane.showInputDialog(null, "Read status:", "Zotero Filter", JOptionPane.QUESTION_MESSAGE, null,
            Constants.READ_STATUSES as Object[], Constants.READ_STATUSES.first())?.toString()
    }
    if (field == "type") {
        return JOptionPane.showInputDialog(null, "Node type:", "Zotero Filter", JOptionPane.QUESTION_MESSAGE, null,
            Constants.NODE_TYPES as Object[], Constants.NODE_TYPES.first())?.toString()
    }
    JOptionPane.showInputDialog(null, "Publication year:", "Zotero Filter", JOptionPane.QUESTION_MESSAGE)
}

def field = chooseField()
if (field) {
    def value = chooseValue(field)
    if (value) {
        new Zotero(ui, logger, c, map).filterNodes(field, value)
    }
}
