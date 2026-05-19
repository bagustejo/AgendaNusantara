package com.sdm.agendanusantara

/**
 * Data class merepresentasikan satu baris tugas dari tabel tasks.
 */
data class Task(
    val id          : Int,
    val title       : String,
    val description : String,
    val dueDate     : String,   // "yyyy-MM-dd"
    val category    : String,   // "penting" | "biasa"
    var isDone      : Boolean
)
