package com.neco.pet

import android.content.Context
import java.util.Calendar

enum class Mood(val sprite: Int, val text: String) {
    IDLE(R.drawable.neco_idle, "Neco is chilling"),
    HAPPY(R.drawable.neco_happy, "Neco is happy!"),
    HUNGRY(R.drawable.neco_hungry, "Neco is hungry!"),
    SLEEPING(R.drawable.neco_sleeping, "Neco is sleeping... zzz")
}

/** Stats are computed from timestamps, so nothing needs to run in the background. */
object Pet {
    private const val FOOD_HOURS = 14f  // full -> empty
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

    /** returns false if Neco is already full */
    fun feed(c: Context): Boolean {
        val s = state(c)
        if (s.food >= 95f) return false
        save(c, State((s.food + 35f).coerceAtMost(100f), (s.joy + 5f).coerceAtMost(100f)), System.currentTimeMillis(), true)
        return true
    }

    fun play(c: Context) {
        val s = state(c)
        save(c, State((s.food - 8f).coerceAtLeast(0f), (s.joy + 25f).coerceAtMost(100f)), System.currentTimeMillis(), true)
    }

    fun pet(c: Context) {
        val s = state(c)
        save(c, State(s.food, (s.joy + 10f).coerceAtMost(100f)), System.currentTimeMillis(), true)
    }

    fun mood(c: Context): Mood {
        val s = state(c)
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val recent = now - prefs(c).getLong("act", 0) < 30 * 60_000
        return when {
            s.food < 35 -> Mood.HUNGRY
            hour >= 23 || hour < 7 -> Mood.SLEEPING
            recent || s.joy >= 75 -> Mood.HAPPY
            else -> Mood.IDLE
        }
    }
}
