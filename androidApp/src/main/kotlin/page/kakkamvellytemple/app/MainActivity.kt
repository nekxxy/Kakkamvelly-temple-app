package page.kakkamvellytemple.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import page.kakkamvellytemple.app.presentation.ui.KVTApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KVTApp(
                onOpenMaps  = { url -> openUri(url) },
                onCall      = { phone -> openUri("tel:$phone") },
                onWhatsApp  = { number -> openUri("https://wa.me/$number") }
            )
        }
    }

    private fun openUri(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}
