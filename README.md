# freeplane-zotero

A [Freeplane](https://www.freeplane.org) add-on for attaching, organizing, refreshing, styling, and exporting Zotero-backed literature notes inside a Freeplane mind map.

## Features
- Keep the current Zotero HTTP Citing Protocol integration as the source of citation editing
- Store normalized citation metadata on nodes instead of relying on parsing `title [citation]`
- Migrate legacy maps that only contain `zotero_citations`
- Refresh a selected node, subtree, or the whole map from Zotero
- Convert nodes into paper cards with cached title / author / year metadata
- Apply direct formatting by node type and read status
- Filter or highlight the map by year, read status, or node type
- Export a selected subtree to Markdown, selected papers to CSV, or selected item keys to text
- Open linked items directly in Zotero from Freeplane

![Freeplane-Zotero in action](screenshot.png)

## How to install

- Install Freeplane and Zotero if not installed yet
- Build or [download](https://github.com/petervelosy/freeplane-zotero/releases) the add-on
- Double click the generated `.addon.mm` file to install it
- Alternatively, open `Tools > Add-ons` in Freeplane, browse for the `.addon.mm` file, and install it

## How to build
- Download and install Freeplane Developer Tools from [this page](https://www.freeplane.org/wiki/index.php/Add-ons_(install)#Developer_Tools)
- Install a Java runtime that can run Gradle
- Clone this repository using Git
- Execute the following commands:

```
export FREEPLANE_DIR='...' # Your Freeplane installation directory, e.g. /usr/share/freeplane
export FREEPLANE_USER_DIR='...' # Your Freeplane user settings directory without the version number suffix. (Freeplane, Tools/Open user directory)
cd freeplane-zotero
./gradlew packageAddon
```

- The add-on installation file will be located at `build/addon/freeplane-zotero-[version].addon.mm`
- Use the file with the version number in its name. The plain `src/addon/Freeplane-Zotero.mm` file is only the add-on definition and is not directly installable.

## Usage

### Core commands
- `Attach citation to selected node`
- `Refresh selected node`
- `Refresh selected subtree`
- `Refresh whole map`
- `Convert selected node to paper card`
- `Open linked item in Zotero`
- `Create literature review template`

### Literature metadata
The add-on stores canonical metadata in node attributes:

- `zotero_item_keys`
- `zotero_csl_code_raw`
- `zotero_citation_text_cache`
- `zotero_title_cache`
- `zotero_author_cache`
- `zotero_year_cache`
- `zotero_node_type`
- `zotero_read_status`
- `zotero_relation_role`
- `zotero_schema_version`

Map-level metadata is stored in:

- `zotero_document_id`
- `zotero_document_data`
- `zotero_schema_version`

Visible node text is rendered from cached metadata as `Title [citation]`. Internal state does not depend on parsing the visible node text after migration.

### Node types and statuses
Supported node types:

- `paper`
- `topic`
- `claim`
- `method`
- `note`

Supported read statuses:

- `unread`
- `queued`
- `reading`
- `reviewed`

## Migration

- Existing maps with only `zotero_citations` are migrated automatically the first time you attach a citation or run a refresh/filter/export command
- Migration preserves the existing visible citation text while moving the raw CSL code into normalized attributes
- The legacy `zotero_citations` attribute is kept for compatibility in this release, but runtime behavior reads canonical metadata first

## Zotero transaction recovery

- The add-on performs a connector health check before starting protocol commands
- If Zotero returns `503` / `Integration transaction is already in progress`, restart Zotero and retry the command
- Connection failures, malformed responses, timeouts, and cancelled protocol dialogs are surfaced as explicit user-facing errors

## Export behavior

- Markdown, CSV, and item-key exports prompt for a save location
- If you cancel the save dialog, the generated content is still copied to the clipboard

## Testing

- Unit tests cover CSL parsing, rendering, migration, export serialization, and protocol failure handling
- Protocol tests use a stub local HTTP server and do not require a running Zotero instance
- Manual acceptance checks still require a real Freeplane + Zotero environment

## License

This add-on remains GPL-3.0 compatible. See [LICENSE](LICENSE).
