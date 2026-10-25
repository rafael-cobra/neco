package com.neco.pet

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.random.Random

/** Animated Neco. Only ticks while the app is on screen, so it costs nothing in the background. */
class PetView(c: Context) : View(c) {
    var mood = Mood.IDLE
        set(v) { field = v; invalidate() }
    private var temp = 0
    private var tempUntil = 0L
    private var treat: Bitmap? = null
    private var treatStart = 0L
    private val hearts = ArrayList<FloatArray>()
    private var frame = 0
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val red = Paint().apply { color = Color.rgb(220, 50, 80); isAntiAlias = false }
    private val heart = listOf(1 to 0, 3 to 0, 0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 1, 1 to 2, 2 to 2, 3 to 2, 2 to 3)

    private val tick = object : Runnable {
        override fun run() { frame++; invalidate(); postDelayed(this, 125) }
    }

    override fun onWindowVisibilityChanged(v: Int) {
        super.onWindowVisibilityChanged(v)
        removeCallbacks(tick)
        if (v == VISIBLE) post(tick)
    }

    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    fun eat(icon: Bitmap?) {
        val now = SystemClock.uptimeMillis()
        temp = R.drawable.neco_eating; tempUntil = now + 1600
        treat = icon; treatStart = now
    }

    fun play() { temp = R.drawable.neco_playing; tempUntil = SystemClock.uptimeMillis() + 2000 }

    fun petted() {
        temp = R.drawable.neco_happy; tempUntil = SystemClock.uptimeMillis() + 1500
        repeat(3) { hearts.add(floatArrayOf(Random.nextInt(12, 32).toFloat(), 10f + it * 3f)) }
    }

    override fun onDraw(cv: Canvas) {
        val now = SystemClock.uptimeMillis()
        val s = minOf(width, height) / 48f
        cv.save(); cv.scale(s, s)
        Sprites.drawBed(cv, context)

        var res = mood.sprite
        var dx = 0
        var dy = 0
        if (now < tempUntil) {
            res = temp
            dy = if (frame % 2 == 0) -1 else 0
        } else {
            when (mood) {
                Mood.IDLE -> { dy = if ((frame / 5) % 2 == 0) 0 else 1; if (frame % 28 < 2) res = R.drawable.neco_blink }
                Mood.HAPPY -> dy = intArrayOf(0, -2, -3, -2)[frame % 4]
                Mood.SLEEPING -> dy = if ((frame / 8) % 2 == 0) 0 else 1
                Mood.HUNGRY -> if (frame % 16 < 6) dx = if (frame % 2 == 0) -1 else 1
                Mood.LONELY -> dy = if ((frame / 10) % 2 == 0) 0 else 1
            }
        }
        cv.save(); cv.translate(dx.toFloat(), dy.toFloat())
        Sprites.drawDog(cv, context, res)
        cv.restore()

        treat?.let { b ->
            val t = (now - treatStart) / 700f
            if (t < 1f) cv.drawBitmap(b, 16f, -4f + t * 20f, paint) else treat = null
        }

        val iter = hearts.iterator()
        while (iter.hasNext()) {
            val h = iter.next()
            h[1] -= 0.8f
            if (h[1] < -5f) { iter.remove(); continue }
            for ((px, py) in heart) {
                val x = h[0].toInt() + px
                val y = h[1].toInt() + py
                cv.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, red)
            }
        }
        cv.restore()
    }
}
