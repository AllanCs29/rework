package com.example.routineapp.data

data class RoutineItem(
    val title: String,
    val time: String?,
    val done: Boolean
)

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val doneSets: Int = 0
)

data class DayHistory(
    val date: String,
    val done: Int,
    val total: Int
)