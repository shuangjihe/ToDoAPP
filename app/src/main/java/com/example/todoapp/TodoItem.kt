package com.example.todoapp

data class TodoItem(
    val text: String,
    var isCompleted: Boolean = false
)