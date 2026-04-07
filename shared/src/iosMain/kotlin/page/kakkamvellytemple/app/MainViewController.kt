package page.kakkamvellytemple.app

import androidx.compose.ui.window.ComposeUIViewController
import page.kakkamvellytemple.app.presentation.ui.KVTApp

fun MainViewController() = ComposeUIViewController {
    KVTApp(
        onOpenMaps = { url -> /* iOS handles via UIApplication.openURL */ },
        onCall     = { phone -> /* iOS handles via tel: URL scheme */ },
        onWhatsApp = { number -> /* iOS handles via wa.me URL */ }
    )
}
