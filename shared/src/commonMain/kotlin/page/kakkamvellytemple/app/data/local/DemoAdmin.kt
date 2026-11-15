package page.kakkamvellytemple.app.data.local

/** Release-safety gate for the demo admin console and local override persistence.
 *  Set true only from debug builds (KVTApplication does this via BuildConfig.DEBUG).
 *  Default false: release/production builds never expose admin UI or local overrides. */
object DemoAdmin {
    var enabled: Boolean = false
}
