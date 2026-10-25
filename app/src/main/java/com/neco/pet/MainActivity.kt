package com.neco.pet

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var img: ImageView
    private lateinit var info: TextView

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        img = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(280), dp(280)) }
        info = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(24)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun btn(label: String, act: () -> Unit) = Button(this).apply {
            text = label
            setOnClickListener { act(); refresh() }
            row.addView(this)
        }
        btn("Feed") { if (!Pet.feed(this)) Toast.makeText(this, "Neco is full!", Toast.LENGTH_SHORT).show() }
        btn("Play") { Pet.play(this) }
        btn("Pet") { Pet.pet(this) }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(img); addView(info); addView(row)
        })
        UpdateWorker.schedule(this)
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        Notifier.post(this)
    }

    private fun refresh() {
        val m = Pet.mood(this)
        val s = Pet.state(this)
        val d = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, m.sprite))
        d.paint.isFilterBitmap = false // keep the pixels crisp
        img.setImageDrawable(d)
        info.text = "${m.text}\nFood ${s.food.toInt()}%  •  Joy ${s.joy.toInt()}%"
        Notifier.post(this)
    }
}
