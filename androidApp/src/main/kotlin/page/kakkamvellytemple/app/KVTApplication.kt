package page.kakkamvellytemple.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KVTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                "festivals", "Festival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Upcoming festival and Vishu reminders" })
            manager.createNotificationChannel(NotificationChannel(
                "annadhanam", "Annadhanam Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Monthly first-Sunday Annadhanam reminders" })
        }
    }
}
