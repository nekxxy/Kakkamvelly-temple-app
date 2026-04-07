package page.kakkamvellytemple.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import page.kakkamvellytemple.app.MainActivity

class KVTFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title   = message.notification?.title ?: message.data["title"] ?: "ക്ഷേത്ര അറിയിപ്പ്"
        val body    = message.notification?.body  ?: message.data["body"]  ?: ""
        val channel = message.data["channel"] ?: "festivals"

        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notifId = System.currentTimeMillis().toInt()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId, notification)
    }

    override fun onNewToken(token: String) {
        // Send token to your backend when implemented
    }
}
