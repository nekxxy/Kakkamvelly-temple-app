package page.kakkamvellytemple.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import page.kakkamvellytemple.app.presentation.ui.KVTApp
import page.kakkamvellytemple.app.update.AppUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            KVTApp(
                onOpenMaps  = { url -> openUri(url) },
                onCall      = { phone -> openUri("tel:$phone") },
                onWhatsApp  = { number -> openUri("https://wa.me/$number") },
                onOpenUrl   = { url -> openUri(url) },
                // Silent background download + one-tap install
                onUpdate    = { AppUpdater.downloadAndInstall(this) }
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun openUri(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}
