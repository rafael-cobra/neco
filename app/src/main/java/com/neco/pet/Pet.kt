package com.neco.pet

import android.content.Context
import java.util.Calendar

enum class Mood(val sprite: Int, val text: String) {
    IDLE(R.drawable.neco_idle, "Neco is chilling"),
    HAPPY(R.drawable.neco_happy, "Neco is happy!"),
    HUNGRY(R.drawable.neco_hungry, "Neco is hungry!"),
    LONELY(R.drawable.neco_lonely, "Neco misses you..."),
    SLEEPING(R.drawable.neco_sleeping, "Neco is sleeping... zzz")
}

/** slot: treat, head, neck, face, bed */
data class Item(val id: String, val name: String, val price: Int, val slot: String, val food: Int = 0, val joy: Int = 0)

object Pet {
    val ITEMS = listOf(
        Item("treat_bone", "Bone", 5, "treat", 20, 15),
        Item("treat_cookie", "Cookie", 3, "treat", 10, 10),
        Item("treat_steak", "Steak", 12, "treat", 50, 25),
        Item("hat_party", "Party hat", 30, "head"),
        Item("hat_crown", "Crown", 80, "head"),
        Item("neck_bow", "Bow tie", 20, "neck"),
        Item("neck_scarf", "Scarf", 25, "neck"),
        Item("face_shades", "Sunglasses", 40, "face"),
        Item("bed_pink", "Pink cushion", 0, "bed"),
        Item("bed_blue", "Blue basket", 40, "bed"),
        Item("bed_star", "Star bed", 80, "bed")
    )

    private const val FOOD_HOURS = 14f
    private const val JOY_HOURS = 20f
    data class State(val food: Float, val joy: Float)

    private fun prefs(c: Context) = c.getSharedPreferences("neco", Context.MODE_PRIVATE)

    fun state(c: Context, now: Long = System.currentTimeMillis()): State {
        val p = prefs(c)
        if (!p.contains("t")) save(c, State(80f, 80f), now)
        val h = (now - p.getLong("t", now)) / 3_600_000f
        return State(
            (p.getFloat("food", 80f) - h * 100f / FOOD_HOURS).coerceIn(0f, 100f),
            (p.getFloat("joy", 80f) - h * 100f / JOY_HOURS).coerceIn(0f, 100f)
        )
    }

    private fun save(c: Context, s: State, now: Long, acted: Boolean = false) {
        val e = prefs(c).edit().putFloat("food", s.food).putFloat("joy", s.joy).putLong("t", now)
        if (acted) e.putLong("act", now)
        e.apply()
    }

    private fun bump(c: Context, food: Float, joy: Float) {
        val s = state(c)
        save(c, State((s.food + food).coerceIn(0f, 100f), (s.joy + joy).coerceIn(0f, 100f)), System.currentTimeMillis(), true)
    }

    /** false if Neco is already full */
    fun feed(c: Context): Boolean {
        if (state(c).food >= 95f) return false
        bump(c, 35f, 5f); return true
    }

    fun play(c: Context) { bump(c, -8f, 25f); addCoins(c, 5) }

    fun pet(c: Context) {
        bump(c, 0f, 10f)
        val now = System.currentTimeMillis()
        val p = prefs(c)
        if (now - p.getLong("pc", 0) > 5 * 60_000) { p.edit().putLong("pc", now).apply(); addCoins(c, 1) }
    }

    fun mood(c: Context): Mood {
        val s = state(c)
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val recent = now - prefs(c).getLong("act", 0) < 30 * 60_000
        return when {
            s.food < 35 -> Mood.HUNGRY
            hour >= 23 || hour < 7 -> Mood.SLEEPING
            s.joy < 30 -> Mood.LONELY
            recent || s.joy >= 75 -> Mood.HAPPY
            else -> Mood.IDLE
        }
    }

    // ---- coins, shop, closet ----
    fun coins(c: Context) = prefs(c).getInt("coins", 10)
    fun addCoins(c: Context, n: Int) = prefs(c).edit().putInt("coins", coins(c) + n).apply()

    fun daily(c: Context): Int {
        val day = System.currentTimeMillis() / 86_400_000L
        val p = prefs(c)
        if (p.getLong("day", -1) == day) return 0
        p.edit().putLong("day", day).apply()
        addCoins(c, 15)
        return 15
    }

    fun owned(c: Context, id: String) =
        id == "bed_pink" || (prefs(c).getStringSet("own", emptySet()) ?: emptySet()).contains(id)

    fun count(c: Context, id: String) = prefs(c).getInt("n_$id", 0)

    fun equipped(c: Context, slot: String): String? =
        prefs(c).getString("eq_$slot", if (slot == "bed") "bed_pink" else null)

    fun buy(c: Context, item: Item): Boolean {
        if (coins(c) < item.price) return false
        if (item.slot != "treat" && owned(c, item.id)) return false
        addCoins(c, -item.price)
        if (item.slot == "treat") {
            prefs(c).edit().putInt("n_${item.id}", count(c, item.id) + 1).apply()
        } else {
            val set = HashSet(prefs(c).getStringSet("own", emptySet()) ?: emptySet())
            set.add(item.id)
            prefs(c).edit().putStringSet("own", set).apply()
            equip(c, item)
        }
        return true
    }

    fun equip(c: Context, item: Item) {
        val e = prefs(c).edit()
        if (item.slot == "bed") e.putString("eq_bed", item.id)
        else if (equipped(c, item.slot) == item.id) e.remove("eq_${item.slot}")
        else e.putString("eq_${item.slot}", item.id)
        e.apply()
    }

    fun giveTreat(c: Context, item: Item): Boolean {
        val n = count(c, item.id)
        if (n <= 0) return false
        prefs(c).edit().putInt("n_${item.id}", n - 1).apply()
        bump(c, item.food.toFloat(), item.joy.toFloat())
        return true
    }
}
