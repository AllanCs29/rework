@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.routineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.routineapp.data.*
import com.example.routineapp.ui.theme.RoutineTheme
import com.example.routineapp.util.scheduleReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek

enum class Tab { HOY, TRABAJO, PESAS, ESTUDIO, STATS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(true) }
            RoutineTheme(dark = dark) {
                val ctx = this
                var tab by remember { mutableStateOf(Tab.HOY) }
                var items by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
                var work by remember { mutableStateOf<List<WorkTask>>(emptyList()) }

                LaunchedEffect(Unit) {
                    val saved = loadItems(ctx)
                    items = if (saved.isNotEmpty() && !isNewDay(ctx)) saved else {
                        val gen = generateTodayPlan()
                        saveItems(ctx, gen); markToday(ctx); gen
                    }
                    work = loadWork(ctx)
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RoutineApp", fontWeight = FontWeight.Bold)
                                    Text(LocalDate.now().toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            actions = { IconButton(onClick = { dark = !dark }) { Icon(Icons.Outlined.DarkMode, null) } }
                        )
                    },
                    bottomBar = {
                        Surface(tonalElevation = 3.dp) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomTabButton(Tab.HOY, tab, Icons.Outlined.Today, "Hoy") { tab = it }
                                BottomTabButton(Tab.TRABAJO, tab, Icons.Outlined.Work, "Trabajo") { tab = it }
                                BottomTabButton(Tab.PESAS, tab, Icons.Outlined.FitnessCenter, "Pesas") { tab = it }
                                BottomTabButton(Tab.ESTUDIO, tab, Icons.Outlined.School, "Estudio") { tab = it }
                                BottomTabButton(Tab.STATS, tab, Icons.Outlined.Assessment, "Stats") { tab = it }
                            }
                        }
                    }
                ) { inner ->
                    Column(Modifier.padding(inner).padding(16.dp).fillMaxSize()) {
                        when (tab) {
                            Tab.HOY -> TodayTab(
                                items = items,
                                onAdd = { title, time -> items = items + RoutineItem(title, time, false) },
                                onToggle = { srcIndex, checked ->
                                    items = items.toMutableList().also { l -> l[srcIndex] = l[srcIndex].copy(done = checked) }
                                },
                                onGenerate = {
                                    val gen = generateTodayPlan()
                                    items = gen; saveItems(ctx, gen); markToday(ctx)
                                },
                                onSave = {
                                    saveItems(ctx, items)
                                    items.forEach { it.time?.let { t -> scheduleReminder(ctx, it.title, t) } }
                                    appendHistory(ctx, items.count { it.done }, items.size)
                                }
                            )
                            Tab.TRABAJO -> TrabajoTab(
                                tasks = work,
                                onAdd = { t -> work = work + WorkTask(t, false) },
                                onToggle = { idx, c ->
                                    val up = work.toMutableList(); up[idx] = up[idx].copy(done = c); work = up
                                },
                                onDelete = { idx ->
                                    work = work.toMutableList().also { it.removeAt(idx) }
                                },
                                onSave = { saveWork(ctx, work) }
                            )
                            Tab.PESAS -> Text("Sección Pesas (personaliza tu plan).")
                            Tab.ESTUDIO -> Text("Sección Estudio.")
                            Tab.STATS -> Text("Días guardados: ${loadHistory(ctx).size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    value: Tab,
    current: Tab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (Tab) -> Unit
) {
    val selected = current == value
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        IconButton(onClick = { onClick(value) }) { Icon(icon, label, tint = color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun TodayTab(
    items: List<RoutineItem>,
    onAdd: (String, String?) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var sortAsc by remember { mutableStateOf(true) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(title, { title = it }, label = { Text("Actividad") }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(time, { time = it }, label = { Text("Hora HH:mm") }, modifier = Modifier.width(160.dp))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = {
            if (title.isNotBlank()) { onAdd(title.trim(), time.ifBlank { null }); title = ""; time = "" }
        }) { Icon(Icons.Outlined.Bolt, null); Spacer(Modifier.width(6.dp)); Text("Agregar") }
        FilledTonalButton(onClick = onGenerate) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Generar HOY") }
        Button(onClick = onSave) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(6.dp)); Text("Guardar + Notificar") }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(search, { search = it }, label = { Text("Buscar") }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        FilterChip(selected = sortAsc, onClick = { sortAsc = !sortAsc }, label = { Text(if (sortAsc) "Hora ↑" else "Hora ↓") })
    }
    Spacer(Modifier.height(12.dp))

    val display = items
        .filter { it.title.contains(search, true) || search.isBlank() }
        .sortedWith(compareBy<com.example.routineapp.data.RoutineItem> { it.time?.let { t -> runCatching { LocalTime.parse(t) }.getOrNull() } }.let { if (sortAsc) it else it.reversed() })

    val done = items.count { it.done }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size
    Text("Progreso", style = MaterialTheme.typography.labelMedium)
    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    Text("$done / ${items.size} completadas", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        itemsIndexed(display) { _, item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.time ?: "—", modifier = Modifier.width(64.dp), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { c ->
                            val idx = items.indexOfFirst { src -> src.title == item.title && src.time == item.time }
                            if (idx >= 0) onToggle(idx, c)
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(item.title)
                }
            }
        }
    }
}

@Composable
fun TrabajoTab(
    tasks: List<WorkTask>,
    onAdd: (String) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onSave: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(text, { text = it }, label = { Text("Pendiente de trabajo") }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        FilledTonalButton(onClick = {
            if (text.isNotBlank()) { onAdd(text.trim()); text = "" }
        }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar") }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onSave) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(6.dp)); Text("Guardar") }
    Spacer(Modifier.height(12.dp))

    LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        itemsIndexed(tasks) { idx, item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.done, onCheckedChange = { c -> onToggle(idx, c) })
                    Spacer(Modifier.width(10.dp))
                    Text(item.title, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDelete(idx) }) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
                }
            }
        }
    }
}

fun generateTodayPlan(): List<RoutineItem> {
    val dow = DayOfWeek.from(LocalDate.now())
    val list = mutableListOf<RoutineItem>()
    list += RoutineItem("Levantarse", "07:00", false)
    list += RoutineItem("Trabajo", "08:00", false)
    val pesas = when (dow) {
        DayOfWeek.MONDAY -> "Pesas: Empuje (Pecho/Hombro/Tríceps)"
        DayOfWeek.TUESDAY -> "Pesas: Piernas (Cuádriceps/Glúteo)"
        DayOfWeek.WEDNESDAY -> "Pesas: Tirón (Espalda/Bíceps)"
        DayOfWeek.THURSDAY -> "Pesas: Full Body ligero"
        DayOfWeek.FRIDAY -> "Pesas: Core + movilidad"
        else -> null
    }
    pesas?.let { list += RoutineItem(it, "16:00", false) }
    list += RoutineItem("Estudio programación (2h)", "17:15", false)
    list += RoutineItem("Fútbol: rondos/decisiones 20-30m", "21:00", false)
    list += RoutineItem("Higiene / Ordenar cuarto", "22:00", false)
    return list
}
