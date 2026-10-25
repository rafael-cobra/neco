package com.neco.pet

import android.content.*
import androidx.work.*
import java.util.concurrent.TimeUnit

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        when (i.action) {
            "feed" -> Pet.feed(c)
            "play" -> Pet.play(c)
            "pet" -> Pet.pet(c)
        }
        Notifier.post(c)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Notifier.post(c)
        UpdateWorker.schedule(c)
    }
}

class UpdateWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result { Notifier.post(applicationContext); return Result.success() }
    companion object {
        fun schedule(c: Context) {
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                "neco", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UpdateWorker>(30, TimeUnit.MINUTES).build()
            )
        }
    }
}
