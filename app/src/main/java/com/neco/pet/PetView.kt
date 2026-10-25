package com.neco.pet

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
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
    private var playToy: String? = null
    private var playStart = 0L
    private val white = Paint().apply { color = Color.WHITE; isAntiAlias = false }
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

    fun play(toyId: String?) {
        val now = SystemClock.uptimeMillis()
        temp = R.drawable.neco_playing; tempUntil = now + 2000
        playToy = toyId; playStart = now
    }

    private fun toyPos(id: String, t: Float): Pair<Float, Float> = when (id) {
        "toy_ball" -> Pair(36f - 28f * t, 30f - abs(sin(t * 9.42f)) * 18f)
        "toy_bone" -> Pair(16f + sin(t * 40f) * 1.5f, 27f)
        "toy_frisbee" -> Pair(-12f + t * 62f, 6f - sin(t * 3.14f) * 3f)
        "toy_rope" -> Pair(30f + sin(t * 36f) * 3f, 34f)
        "toy_duck" -> Pair(4f, 31f + sin(t * 12f))
        else -> Pair(24f + 18f * sin(t * 9f), 36f + 6f * sin(t * 13f))
    }

    fun petted() {
        temp = R.drawable.neco_happy; tempUntil = SystemClock.uptimeMillis() + 1500
        repeat(3) { hearts.add(floatArrayOf(Random.nextInt(12, 32).toFloat(), 10f + it * 3f)) }
    }

    override fun onDraw(cv: Canvas) {
        val now = SystemClock.uptimeMillis()
        val s = minOf(width, height) / 48f
        val active = now < tempUntil
        val toy = playToy
        var res = mood.sprite
        var dx = 0
        var dy = 0
        if (active) {
            res = temp
            dy = if (toy == "toy_frisbee" && temp == R.drawable.neco_playing) intArrayOf(0, -3, -4, -3)[frame % 4]
                 else if (frame % 2 == 0) -1 else 0
            if (temp == R.drawable.neco_playing && (toy == "toy_rope" || toy == "toy_laser")) dx = if (frame % 2 == 0) -1 else 1
        } else {
            when (mood) {
                Mood.IDLE -> { dy = if ((frame / 5) % 2 == 0) 0 else 1; if (frame % 28 < 2) res = R.drawable.neco_blink }
                Mood.HAPPY -> dy = intArrayOf(0, -2, -3, -2)[frame % 4]
                Mood.SLEEPING -> dy = if ((frame / 8) % 2 == 0) 0 else 1
                Mood.HUNGRY -> if (frame % 16 < 6) dx = if (frame % 2 == 0) -1 else 1
                Mood.LONELY -> dy = if ((frame / 10) % 2 == 0) 0 else 1
            }
        }
        val night = res == R.drawable.neco_sleeping
        cv.save(); cv.scale(s, s)
        Sprites.drawBg(cv, context, night)
        if (Sprites.currentBg(context, night) == "bg_snow") {
            for (i in 0 until 14) {
                val x = (i * 17) % 48 + (sin((frame + i * 3) / 4f) * 1.5f).toInt()
                val y = (i * 7 + frame) % 48
                cv.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, white)
            }
        }
        Sprites.drawBed(cv, context)
        cv.save(); cv.translate(dx.toFloat(), dy.toFloat())
        Sprites.drawDog(cv, context, res)
        cv.restore()

        if (active && temp == R.drawable.neco_playing && toy != null) {
            val r = Sprites.res(context, "item_$toy")
            if (r != 0) {
                val t = ((now - playStart) / 2000f).coerceIn(0f, 1f)
                val (x, y) = toyPos(toy, t)
                cv.drawBitmap(Sprites.bmp(context, r), x, y, paint)
            }
        }

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
