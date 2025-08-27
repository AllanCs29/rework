@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.routineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.routineapp.data.*
import com.example.routineapp.ui.theme.RoutineTheme
import com.example.routineapp.util.PdfExporter
import com.example.routineapp.util.scheduleReminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 👇 Íconos: importa solo lo necesario para evitar conflictos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Save

enum class Tab { HOY, TRABAJO, EJERCICIO, ESTUDIO, STATS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(true) }
            RoutineTheme(dark = dark) {
                val ctx = this
                var tab by remember { mutableStateOf(Tab.HOY) }
                var items by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
                var ex by remember { mutableStateOf(loadExercises(ctx)) }

                LaunchedEffect(Unit) {
                    val saved = loadItems(ctx)
                    items = if (saved.isNotEmpty() && !isNewDay(ctx)) saved else {
                        val gen = generateTodayPlan()
                        saveItems(ctx, gen); markToday(ctx); gen
                    }
                    if (ex.isEmpty()) ex = defaultWeightsPlan()
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
                            actions = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { dark = !dark }) { Icon(Icons.Outlined.DarkMode, contentDescription = "Tema") }
                                    IconButton(onClick = { PdfExporter.exportToday(ctx, items) }) { Icon(Icons.Outlined.PictureAsPdf, contentDescription = "PDF Hoy") }
                                    IconButton(onClick = { PdfExporter.exportWeekly(ctx, loadHistory(ctx)) }) { Icon(Icons.Outlined.Insights, contentDescription = "PDF Semana") }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Surface(tonalElevation = 3.dp) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomTabButton(Tab.HOY, tab, Icons.Outlined.Today, "Hoy") { tab = it }
                                BottomTabButton(Tab.TRABAJO, tab, Icons.Outlined.WorkOutline, "Trabajo") { tab = it }
                                BottomTabButton(Tab.EJERCICIO, tab, Icons.Outlined.FitnessCenter, "Ejercicio") { tab = it }
                                BottomTabButton(Tab.ESTUDIO, tab, Icons.Outlined.School, "Estudio") { tab = it }
                                BottomTabButton(Tab.STATS, tab, Icons.Outlined.Insights, "Stats") { tab = it }
                            }
                        }
                    }
                ) { inner ->
                    Column(
                        Modifier
                            .padding(inner)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
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
                            Tab.TRABAJO -> WorkTab()
                            Tab.EJERCICIO -> ExerciseTab(exList = ex, onUpdate = { up -> ex = up; saveExercises(ctx, up) })
                            Tab.ESTUDIO -> StudyTab()
                            Tab.STATS -> StatsTab(todayItems = items, history = loadHistory(ctx))
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
    icon: ImageVector,
    label: String,
    onClick: (Tab) -> Unit
) {
    val selected = current == value
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        IconButton(onClick = { onClick(value) }) {
            Icon(icon, label, tint = color)
        }
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
            if (title.isNotBlank()) {
                onAdd(title.trim(), time.ifBlank { null }); title = ""; time = ""
            }
        }) { Icon(Icons.Outlined.Bolt, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Agregar") }
        FilledTonalButton(onClick = onGenerate) { Icon(Icons.Outlined.Refresh, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Generar HOY") }
        Button(onClick = onSave) { Icon(Icons.Outlined.Save, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Guardar + Notificar") }
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
        .sortedWith(
            compareBy<RoutineItem> { it.time?.let { t -> runCatching { LocalTime.parse(t) }.getOrNull() } }
                .let { if (sortAsc) it else it.reversed() }
        )

    val done = items.count { it.done }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size
    Text("Progreso", style = MaterialTheme.typography.labelMedium)
    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    Text("$done / ${items.size} completadas", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        itemsIndexed(display) { _, item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
fun WorkTab() {
    Column {
        Text("Trabajo — lista de pendientes", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Próximamente: lista editable con horarios y recordatorios.")
    }
}

@Composable
fun ExerciseTab(exList: List<Exercise>, onUpdate: (List<Exercise>) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { Text("Pesas — Rutina de hoy", fontWeight = FontWeight.Bold) }
        itemsIndexed(exList) { idx, e ->
            ElevatedCard {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.SemiBold)
                        Text("${e.sets} x ${e.reps} reps", style = MaterialTheme.typography.labelMedium)
                    }
                    AssistChip(
                        onClick = {
                            val up = exList.toMutableList()
                            up[idx] = e.copy(doneSets = (e.doneSets + 1).coerceAtMost(e.sets))
                            onUpdate(up)
                        },
                        label = { Text("+ set  ${e.doneSets}/${e.sets}") }
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Fútbol — Drills del día", fontWeight = FontWeight.Bold)
        }
        itemsIndexed(defaultFootballDrills()) { _, d ->
            ElevatedCard { Text(d, modifier = Modifier.padding(14.dp)) }
        }
    }
}

@Composable
fun StudyTab() {
    Text("Estudio — Pomodoro", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    var work by remember { mutableStateOf(50) }
    var rest by remember { mutableStateOf(10) }
    var remaining by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var onWork by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun start() {
        if (running) return
        running = true
        val startSec = (if (onWork) work else rest) * 60
        remaining = if (remaining > 0) remaining else startSec
        job = scope.launch {
            while (running && remaining > 0) {
                delay(1000)
                remaining -= 1
            }
            if (remaining <= 0) {
                running = false; onWork = !onWork; remaining = 0
            }
        }
    }
    fun pause() { running = false; job?.cancel() }
    fun reset() { running = false; job?.cancel(); remaining = 0; onWork = true }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = work.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { work = it.coerceIn(5, 120) } },
            label = { Text("Trabajo (min)") },
            modifier = Modifier.width(150.dp)
        )
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = rest.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { rest = it.coerceIn(1, 60) } },
            label = { Text("Descanso (min)") },
            modifier = Modifier.width(150.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Button(onClick = { start() }) { Text(if (running) "Continuar" else "Iniciar") }
        FilledTonalButton(onClick = { pause() }) { Text("Pausar") }
        OutlinedButton(onClick = { reset() }) { Text("Reset") }
        val mins = remaining / 60; val secs = remaining % 60
        Spacer(Modifier.width(8.dp))
        Text("Tiempo: %02d:%02d".format(mins, secs), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatsTab(todayItems: List<RoutineItem>, history: List<DayHistory>) {
    val done = todayItems.count { it.done }
    val total = todayItems.size
    val progress = if (total == 0) 0f else done.toFloat() / total
    Text("Hoy — progreso", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    Text("$done / $total completadas")
    Spacer(Modifier.height(16.dp))

    val last14 = history.takeLast(14)
    if (last14.isEmpty()) {
        Text("Aún no hay historial. Usa 'Guardar + Notificar' en HOY para empezar.")
        return
    }
    Text("Tendencia (14 días)", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        if (last14.size >= 2) {
            val step = size.width / (last14.size - 1)
            var px = 0f
            var py = size.height * (1f - last14[0].ratio())
            for (i in 1 until last14.size) {
                val x = i * step
                val y = size.height * (1f - last14[i].ratio())
                drawLine(Color(0xFF6B7D57), Offset(px, py), Offset(x, y), strokeWidth = 6f)
                px = x; py = y
            }
        }
    }
}

private fun DayHistory.ratio(): Float = if (total == 0) 0f else done.toFloat() / total

fun generateTodayPlan(): List<RoutineItem> {
    val dow = LocalDate.now().dayOfWeek
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

fun defaultWeightsPlan(): List<Exercise> {
    val dow = LocalDate.now().dayOfWeek
    return when (dow) {
        DayOfWeek.MONDAY -> listOf(
            Exercise("Press banca mancuernas", 4, 8),
            Exercise("Elevaciones laterales", 3, 12),
            Exercise("Fondos en banco", 3, 12),
            Exercise("Flexiones", 3, 15),
        )
        DayOfWeek.TUESDAY -> listOf(
            Exercise("Sentadilla goblet", 4, 10),
            Exercise("Zancadas", 3, 12),
            Exercise("Puente de glúteo", 3, 15),
        )
        DayOfWeek.WEDNESDAY -> listOf(
            Exercise("Remo mancuerna", 4, 10),
            Exercise("Curl bíceps alterno", 3, 12),
            Exercise("Face pull banda", 3, 15),
        )
        DayOfWeek.THURSDAY -> listOf(
            Exercise("Peso muerto rumano", 4, 8),
            Exercise("Press militar", 3, 10),
            Exercise("Plancha (seg)", 3, 45),
        )
        DayOfWeek.FRIDAY -> listOf(
            Exercise("Rueda abdominal / Hollow", 4, 12),
            Exercise("Farmer walk (pasos)", 4, 40),
            Exercise("Hip hinge movilidad", 3, 10),
        )
        else -> listOf(Exercise("Circuito movilidad", 3, 10))
    }
}

fun defaultFootballDrills(): List<String> = listOf(
    "Rondos 4v2: orientación corporal (2x6')",
    "1v1 cierre: temporización (2x5')",
    "Juego posicional 3 zonas (2x8')",
    "Primer control + pase (3x8')"
)
