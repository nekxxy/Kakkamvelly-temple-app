package page.kakkamvellytemple.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import page.kakkamvellytemple.app.presentation.ui.KVTApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KVTApp(
                onOpenMaps  = { url -> openUri(url) },
                onCall      = { phone -> openUri("tel:$phone") },
                onWhatsApp  = { number -> openUri("https://wa.me/$number") },
                onOpenUrl   = { url -> openUri(url) }
            )
        }
    }
    private fun openUri(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}
