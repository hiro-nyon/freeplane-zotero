package com.petervelosy.freeplanezotero.support

class FakeMap {
    final Map storage = [:]
    FakeNode root

    FakeNode node(String id) {
        findById(root, id)
    }

    private FakeNode findById(FakeNode current, String id) {
        if (current == null) {
            return null
        }
        if (current.id == id) {
            return current
        }
        current.children.collect { findById(it, id) }.find { it != null }
    }
}

class FakeNode {
    static int nextId = 1

    final String id
    String text
    boolean root
    boolean folded = false
    FakeNode parent
    FakeMap mindMap
    final List<FakeNode> children = []
    final Map attributes = [:]
    final FakeLink link = new FakeLink()
    final FakeIcons icons = new FakeIcons()
    final FakeStyle style = new FakeStyle()

    FakeNode(String text, FakeMap map, boolean root = false) {
        this.id = "N${nextId++}"
        this.text = text
        this.mindMap = map
        this.root = root
    }

    Object getAt(String key) {
        attributes[key]
    }

    void putAt(String key, Object value) {
        if (value == null) {
            attributes.remove(key)
        } else {
            attributes[key] = value
        }
    }

    FakeNode createChild(String childText) {
        def child = new FakeNode(childText, mindMap, false)
        child.parent = this
        children << child
        child
    }
}

class FakeLink {
    URI uri

    void setUri(URI uri) {
        this.uri = uri
    }

    void remove() {
        this.uri = null
    }
}

class FakeIcons {
    final List<String> values = []

    void add(String icon) {
        if (!values.contains(icon)) {
            values << icon
        }
    }

    void remove(String icon) {
        values.removeAll { it == icon }
    }
}

class FakeStyle {
    String backgroundColorCode
    String textColorCode
    String maxNodeWidth
    final FakeBorder border = new FakeBorder()
}

class FakeBorder {
    String colorCode
}

class FakeController {
    final FakeMap map
    List<FakeNode> selecteds = []
    FakeNode selected

    FakeController(FakeMap map) {
        this.map = map
    }

    List<FakeNode> findAll() {
        def nodes = []
        collect(map.root, nodes)
        nodes
    }

    void select(FakeNode node) {
        this.selected = node
        this.selecteds = [node]
    }

    private void collect(FakeNode node, List<FakeNode> nodes) {
        if (node == null) {
            return
        }
        nodes << node
        node.children.each { child ->
            collect(child, nodes)
        }
    }
}
