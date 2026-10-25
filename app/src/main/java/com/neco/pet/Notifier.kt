package com.neco.pet

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CH = "neco"
    const val ID = 1

    private fun broadcast(c: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        c, code, Intent(c, ActionReceiver::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE
    )

    fun post(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        nm.createNotificationChannel(
            NotificationChannel(CH, "Neco", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        val s = Pet.state(c)
        val m = Pet.mood(c)
        val stats = "Food ${s.food.toInt()}%  •  Joy ${s.joy.toInt()}%"
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_paw)
            .setLargeIcon(BitmapFactory.decodeResource(c.resources, m.sprite))
            .setContentTitle(m.text)
            .setContentText(stats)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setDeleteIntent(broadcast(c, "refresh", 9)) // swiped away (Android 14+)? repost it
            .addAction(0, "Feed", broadcast(c, "feed", 1))
            .addAction(0, "Play", broadcast(c, "play", 2))
            .addAction(0, "Pet", broadcast(c, "pet", 3))
            .build()
        nm.notify(ID, n)
    }
}
