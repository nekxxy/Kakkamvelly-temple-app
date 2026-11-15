package page.kakkamvellytemple.app.data.local

/** Platform local persistence for the content bundle (demo edits survive restart).
 *  Purely on-device; no network. */
expect object LocalContentCache {
    fun load(): String?
    fun save(json: String)
    fun clear()
}
