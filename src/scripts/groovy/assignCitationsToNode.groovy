// @ExecutionModes({on_single_node="/node_popup/Zotero"})

import com.petervelosy.freeplanezotero.Zotero
import com.petervelosy.freeplanezotero.Constants

def zotero = new Zotero(ui, logger, c, map)

if (!zotero.getDocumentProperty(Constants.STORAGE_KEY_DOCUMENT_ID, node)) {
    menuUtils.executeMenuItems([
        'ShowSelectedAttributesAction'
    ])
}
zotero.attachCitation(node)
