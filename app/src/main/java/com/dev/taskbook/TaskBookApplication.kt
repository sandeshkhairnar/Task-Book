package com.dev.taskbook

import android.app.Application

class TaskBookApplication : Application() {
    private val database by lazy { TaskDatabase.getDatabase(this) }
    val repository by lazy { TaskRepository(database.taskDao()) }
    override fun onCreate() {
        super.onCreate()
    }
}