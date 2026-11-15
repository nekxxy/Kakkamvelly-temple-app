package page.kakkamvellytemple.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import page.kakkamvellytemple.app.data.remote.ContentSync

class KVTApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Demo admin + local content overrides exist only in debug builds, never in release.
        page.kakkamvellytemple.app.data.local.DemoAdmin.enabled = BuildConfig.DEBUG
        if (BuildConfig.DEBUG) {
            page.kakkamvellytemple.app.data.local.LocalContentCache.init(this)
            page.kakkamvellytemple.app.data.repository.ContentStore.loadLocal()
        }
        createNotificationChannels()
        subscribeTopics()
        // Connected-backend credentials (blank until configured; inert while
        // CONNECTED_SETUP_APPROVED is false).
        ContentSync.configureSupabase(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        // Fetch latest temple content (poojas, contacts, events, rates) in the background.
        // Falls back to packaged defaults when offline or unchanged.
        appScope.launch { ContentSync.refresh() }
    }

    private fun subscribeTopics() {
        // Inert until notification setup is approved: no real FCM subscriptions yet.
        if (!ContentSync.NOTIFICATION_SETUP_APPROVED) return
        for (topic in listOf("all", "festivals", "annadhanam", "announcements")) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                "announcements", "Temple Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "General announcements from the temple committee" })
            manager.createNotificationChannel(NotificationChannel(
                "festivals", "Festival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Upcoming festival and Vishu reminders" })
            manager.createNotificationChannel(NotificationChannel(
                "annadhanam", "Annadhanam Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Monthly first-Sunday Annadhanam reminders" })
            manager.createNotificationChannel(NotificationChannel(
                "events", "Event Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Special pooja and event updates" })
        }
    }
}
