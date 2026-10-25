package com.neco.pet

import android.content.Context
import android.graphics.*

object Sprites {
    private val cache = HashMap<Int, Bitmap>()
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }

    fun res(c: Context, name: String) = c.resources.getIdentifier(name, "drawable", c.packageName)

    fun bmp(c: Context, res: Int): Bitmap = cache.getOrPut(res) {
        BitmapFactory.decodeResource(c.resources, res, BitmapFactory.Options().apply { inScaled = false })
    }

    private fun item(cv: Canvas, c: Context, id: String?) {
        if (id == null) return
        val r = res(c, "item_$id")
        if (r != 0) cv.drawBitmap(bmp(c, r), 0f, 0f, paint)
    }

    /** All drawing happens on a 48x48 grid; callers scale the canvas. */
    fun currentBg(c: Context, night: Boolean): String =
        if (night && Pet.owned(c, "bg_night")) "bg_night" else (Pet.equipped(c, "bg") ?: "bg_home")

    fun drawBg(cv: Canvas, c: Context, night: Boolean) = item(cv, c, currentBg(c, night))

    fun drawBed(cv: Canvas, c: Context) = item(cv, c, Pet.equipped(c, "bed"))

    fun drawDog(cv: Canvas, c: Context, sprite: Int) {
        cv.drawBitmap(bmp(c, sprite), 0f, 0f, paint)
        for (slot in listOf("neck", "face", "head")) item(cv, c, Pet.equipped(c, slot))
    }

    fun notif(c: Context, sprite: Int): Bitmap {
        val b = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        drawBg(cv, c, sprite == R.drawable.neco_sleeping); drawBed(cv, c); drawDog(cv, c, sprite)
        return Bitmap.createScaledBitmap(b, 192, 192, false)
    }
}
