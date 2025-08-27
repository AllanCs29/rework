package com.example.routineapp.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

data class RoutineItem(val title: String, val time: String?, val done: Boolean)
data class WorkTask(val title: String, val done: Boolean)
data class DayHistory(val date: String, val done: Int, val total: Int)

private fun prefs(ctx: Context): SharedPreferences =
    ctx.getSharedPreferences("routine_prefs", Context.MODE_PRIVATE)

// ---- Daily items ----
fun saveItems(ctx: Context, items: List<RoutineItem>) {
    val s = items.joinToString("||") { listOf(it.title, it.time ?: "", it.done).joinToString("|") }
    prefs(ctx).edit().putString("items", s).apply()
}
fun loadItems(ctx: Context): List<RoutineItem> {
    val raw = prefs(ctx).getString("items", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("||").mapNotNull {
        val p = it.split("|")
        if (p.size < 3) null else RoutineItem(p[0], p[1].ifBlank { null }, p[2].toBoolean())
    }
}

// ---- Work tasks (persistent) ----
fun saveWork(ctx: Context, tasks: List<WorkTask>) {
    val s = tasks.joinToString("||") { listOf(it.title, it.done).joinToString("|") }
    prefs(ctx).edit().putString("work", s).apply()
}
fun loadWork(ctx: Context): List<WorkTask> {
    val raw = prefs(ctx).getString("work", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("||").mapNotNull {
        val p = it.split("|")
        if (p.size < 2) null else WorkTask(p[0], p[1].toBoolean())
    }
}

// ---- Day switches / history ----
fun markToday(ctx: Context) {
    prefs(ctx).edit().putString("last_day", LocalDate.now().toString()).apply()
}
fun isNewDay(ctx: Context): Boolean {
    val last = prefs(ctx).getString("last_day", "") ?: ""
    return last != LocalDate.now().toString()
}

fun appendHistory(ctx: Context, done: Int, total: Int) {
    val list = loadHistory(ctx).toMutableList()
    list.add(DayHistory(LocalDate.now().toString(), done, total))
    val s = list.joinToString("||") { "${it.date}|${it.done}|${it.total}" }
    prefs(ctx).edit().putString("hist", s).apply()
}
fun loadHistory(ctx: Context): List<DayHistory> {
    val raw = prefs(ctx).getString("hist", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("||").mapNotNull {
        val p = it.split("|")
        if (p.size < 3) null else DayHistory(p[0], p[1].toInt(), p[2].toInt())
    }
}
