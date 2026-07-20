package com.lomigoo.classworkmanager.data

data class Classwork(
    val id: Int = 0,
    val courseName: String,
    val actionDescription: String,
    val dateCreated: String,
    val dateTarget: String,
    val isCompleted: Boolean = false
)
