package com.example.routineapp.data

import android.content.Context
import java.time.LocalDate

private const val PREF = "routine_store"
private const val KEY_ITEMS = "items"
private const val KEY_DAY = "last_day"
private const val KEY_HISTORY = "history"
private const val KEY_EX = "exercises"

fun saveItems(ctx: Context, items: List<RoutineItem>) {
    val enc = items.joinToString("||") { "${it.title}::${it.time ?: ""}::${it.done}" }
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, enc).apply()
}
fun loadItems(ctx: Context): List<RoutineItem> {
    val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ITEMS, "") ?: ""
    if (s.isBlank()) return emptyList()
    return s.split("||").mapNotNull {
        val p = it.split("::")
        if (p.size >= 3) RoutineItem(p[0], p[1].ifBlank { null }, p[2].toBoolean()) else null
    }
}

fun markToday(ctx: Context) {
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_DAY, LocalDate.now().toString()).apply()
}
fun isNewDay(ctx: Context): Boolean {
    val last = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_DAY, null)
    return last != LocalDate.now().toString()
}

fun appendHistory(ctx: Context, done: Int, total: Int) {
    val today = LocalDate.now().toString()
    val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    val cur = prefs.getString(KEY_HISTORY, "") ?: ""
    val line = "$today,$done,$total"
    val newv = (if (cur.isBlank()) line else "$cur|$line")
    prefs.edit().putString(KEY_HISTORY, newv).apply()
}
fun loadHistory(ctx: Context): List<DayHistory> {
    val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_HISTORY, "") ?: ""
    if (s.isBlank()) return emptyList()
    return s.split("|").mapNotNull {
        val p = it.split(","); if (p.size == 3) DayHistory(p[0], p[1].toInt(), p[2].toInt()) else null
    }
}

fun saveExercises(ctx: Context, list: List<Exercise>) {
    val enc = list.joinToString("|") { "${it.name};${it.sets};${it.reps};${it.doneSets}" }
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_EX, enc).apply()
}
fun loadExercises(ctx: Context): List<Exercise> {
    val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_EX, "") ?: ""
    if (s.isBlank()) return emptyList()
    return s.split("|").mapNotNull {
        val p = it.split(";")
        if (p.size >= 4) Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt()) else null
    }
}