package com.dev.taskbook

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    init {
        // Load tasks from repository
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _allTasks.value = tasks
            }
        }
    }

    fun addTask(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.update(task)
        // Refresh the tasks list after update
        _allTasks.update { currentTasks ->
            currentTasks.map {
                if (it.id == task.id) task else it
            }
        }
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun updateRunningTasksPaused() = viewModelScope.launch {
        val updatedTasks = _allTasks.value.map { task ->
            if (task.isRunning) {
                task.copy(
                    isRunning = false,
                    isPaused = true
                )
            } else task
        }

        updatedTasks.forEach { repository.update(it) }
        _allTasks.value = updatedTasks
    }

    fun setActiveTask(activeTask: Task) = viewModelScope.launch {
        val updatedTasks = _allTasks.value.map { task ->
            if (task.id == activeTask.id) {
                task.copy(
                    isRunning = true,
                    isPaused = false
                )
            } else {
                task.copy(
                    isRunning = false,
                    isPaused = true
                )
            }
        }

        updatedTasks.forEach { repository.update(it) }
        _allTasks.value = updatedTasks
    }

    fun startTimer(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(
            isRunning = true,
            isPaused = false
        )
        updateTask(updatedTask)
    }

    fun pauseTimer(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(
            isRunning = false,
            isPaused = true
        )
        updateTask(updatedTask)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaskBookApplication)
                TaskViewModel(application.repository)
            }
        }
    }
}
