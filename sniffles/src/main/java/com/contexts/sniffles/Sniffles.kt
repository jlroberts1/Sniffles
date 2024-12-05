package com.contexts.sniffles

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin

class Sniffles private constructor(private val application: Application) {
    private val interceptor = SnifflesInterceptor()

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == "android.permission.POST_NOTIFICATIONS") {
                checkAndShowNotification()
            }
        }
    }

    init {
        createNotificationChannel()
        registerPermissionReceiver()
        checkAndShowNotification()
    }

    private fun registerPermissionReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(
                permissionReceiver,
                IntentFilter("android.permission.POST_NOTIFICATIONS"),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun checkAndShowNotification() {
        if (hasNotificationPermission()) {
            showPersistentNotification()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                application,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val TAG = "Sniffles"

        @Volatile
        private var instance: Sniffles? = null

        fun init(application: Application) {
            instance ?: synchronized(this) {
                instance ?: Sniffles(application).also { instance = it }
                try {
                    GlobalContext.get()
                    loadKoinModules(appModule)
                } catch (e: IllegalStateException) {
                    startKoin {
                        modules(appModule)
                    }
                }
            }
        }

        fun getInstance(): Sniffles =
            instance ?: throw IllegalStateException("Sniffles must be initialized first")

        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "Sniffles"
    }

    fun getInterceptor() = interceptor

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sniffles Network Debugger"
            val descriptionText = "Shows when network debugging is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showPersistentNotification() {
        val intent = Intent(application, DebugMenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            application,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(application, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sniffles Debugger Active")
            .setContentText("Tap to open debug menu")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(application).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e("Sniffles", "Security exception while posting notification for Sniffles")
        }
    }
}