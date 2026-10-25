package com.neco.pet

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var pv: PetView
    private lateinit var info: TextView
    private lateinit var coinsTv: TextView
    private lateinit var foodBar: ProgressBar
    private lateinit var joyBar: ProgressBar

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        coinsTv = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER; setPadding(0, dp(16), 0, 0) }
        pv = PetView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(300), dp(300)) }
        info = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)) }

        fun statRow(label: String): ProgressBar {
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; layoutParams = LinearLayout.LayoutParams(dp(220), dp(14))
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@MainActivity).apply { text = label; width = dp(50) })
                addView(bar)
            }
            root.addView(row)
            return bar
        }
        fun buttons(vararg b: Pair<String, () -> Unit>) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for ((label, act) in b) addView(Button(this@MainActivity).apply {
                text = label; setOnClickListener { act() }
            })
        }

        root.addView(coinsTv); root.addView(pv); root.addView(info)
        foodBar = statRow("Food"); joyBar = statRow("Joy")
        root.addView(buttons(
            "Feed" to { if (Pet.feed(this)) pv.eat(null) else toast("Neco is full!"); refresh() },
            "Play" to {
                val r = Pet.play(this)
                pv.play(Pet.equipped(this, "toy"))
                toast("+${r.coins} coins" + (if (r.loved) " - Neco LOVES this toy!" else ""))
                refresh()
            },
            "Pet" to { Pet.pet(this); pv.petted(); refresh() }
        ))
        root.addView(buttons(
            "Treats" to { showTreats() },
            "Closet" to { showCloset() },
            "Shop" to { showShop() }
        ))
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
        UpdateWorker.schedule(this)
    }

    private val root by lazy {
        LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(16)) }
    }

    override fun onResume() {
        super.onResume()
        val bonus = Pet.daily(this)
        if (bonus > 0) toast("Daily bonus: +$bonus coins!")
        refresh()
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        Notifier.post(this)
    }

    private fun refresh() {
        val m = Pet.mood(this)
        val s = Pet.state(this)
        pv.mood = m
        info.text = m.text
        foodBar.progress = s.food.toInt()
        joyBar.progress = s.joy.toInt()
        coinsTv.text = "Coins: ${Pet.coins(this)}"
        Notifier.post(this)
    }

    private fun showTreats() {
        val have = Pet.ITEMS.filter { it.slot == "treat" && Pet.count(this, it.id) > 0 }
        if (have.isEmpty()) { toast("No treats yet. Buy some in the Shop!"); return }
        AlertDialog.Builder(this).setTitle("Give a treat")
            .setItems(have.map { "${it.name}  x${Pet.count(this, it.id)}" }.toTypedArray()) { _, i ->
                val t = have[i]
                Pet.giveTreat(this, t)
                pv.eat(Sprites.bmp(this, Sprites.res(this, "item_${t.id}")))
                refresh()
            }.setNegativeButton("Close", null).show()
    }

    private fun cat(slot: String) = when (slot) {
        "treat" -> "Treat"; "head" -> "Hat"; "neck" -> "Neck"; "face" -> "Face"
        "bed" -> "Bed"; "bg" -> "Scene"; else -> "Toy"
    }

    private fun showShop() {
        val list = Pet.ITEMS.filter { it.slot == "treat" || !Pet.owned(this, it.id) }
        val labels = list.map {
            val fx = if (it.slot == "treat") "  (food +${it.food}, joy +${it.joy})" else ""
            "${cat(it.slot)}: ${it.name} - ${it.price} coins$fx"
        }
        AlertDialog.Builder(this).setTitle("Shop - ${Pet.coins(this)} coins")
            .setItems(labels.toTypedArray()) { _, i ->
                val item = list[i]
                toast(if (Pet.buy(this, item)) "Bought ${item.name}!" else "Not enough coins")
                refresh(); showShop()
            }.setNegativeButton("Close", null).show()
    }

    private fun showCloset() {
        val list = Pet.ITEMS.filter { it.slot != "treat" && Pet.owned(this, it.id) }
        val labels = list.map { (if (Pet.equipped(this, it.slot) == it.id) "[on] " else "") + "${cat(it.slot)}: ${it.name}" }
        AlertDialog.Builder(this).setTitle("Closet (tap to use / remove)")
            .setItems(labels.toTypedArray()) { _, i ->
                Pet.equip(this, list[i]); refresh(); showCloset()
            }.setNegativeButton("Close", null).show()
    }
}
