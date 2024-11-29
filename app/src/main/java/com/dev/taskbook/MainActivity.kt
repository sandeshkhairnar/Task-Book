package com.dev.taskbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlin.math.roundToInt

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val estimatedTime: Int, // in minutes
    val startTime: LocalTime = LocalTime.now(),
    var isCompleted: Boolean = false,
    var progress: Float = 0f,
    var completedDate: String? = null,
    var isRunning: Boolean = false,
    var elapsedTime: Int = 0, // in seconds
    var completedDateTime: LocalDateTime? = null,
    val isPaused: Boolean = false,
    val assignedHours: Int = 0,
    val assignedMinutes: Int = 0,
    val assignedSeconds: Int = 0,
    val date: LocalDate
) {
    fun getTotalAssignedTimeFormatted(): String {
        return String.format("%02d:%02d:%02d", assignedHours, assignedMinutes, assignedSeconds)
    }

    fun getCompletionTimeFormatted(): String? {
        return completedDateTime?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels {
        val repository = TaskRepository(TaskDatabase.getDatabase(this).taskDao())
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Pass the view model to the Compose UI
            TaskBookApp(taskViewModel)
        }
    }
}



sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    data object Home : Screen("home", Icons.Default.Home, "Home")
    data object Contributions : Screen("contributions", Icons.Default.DateRange, "Contributions")
    data object Progress : Screen("progress", Icons.Filled.PieChart, "Progress")
}

@Composable
fun TaskBookApp(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Contributions, Screen.Progress)

    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Coroutine scope for task timer
    val coroutineScope = rememberCoroutineScope()
    var activeTimerJob by remember { mutableStateOf<Job?>(null) }

    fun startTaskTimer(task: Task) {
        activeTimerJob?.cancel() // Cancel any existing timer

        activeTimerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                viewModel.updateTask(
                    task.copy(
                        elapsedTime = task.elapsedTime + 1,
                        progress = ((task.elapsedTime + 1) / 60f) / task.estimatedTime,
                        isRunning = true,
                        isCompleted = ((task.elapsedTime + 1) / 60f) / task.estimatedTime >= 1.0f,
                        completedDate = if (((task.elapsedTime + 1) / 60f) / task.estimatedTime >= 1.0f)
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        else null
                    )
                )
            }
        }
    }

    fun pauseTaskTimer() {
        activeTimerJob?.cancel()
        activeTimerJob = null
        viewModel.updateRunningTasksPaused()
    }

    fun handleTaskAction(task: Task) {
        if (!task.isRunning) {
            // Start the task
            viewModel.setActiveTask(task)
            startTaskTimer(task)
        } else {
            // Pause the task
            pauseTaskTimer()
        }
    }

    fun handleTaskCompletion(task: Task) {
        viewModel.updateTask(
            task.copy(
                isCompleted = true,
                completedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                progress = 1f,
                isRunning = false
            )
        )
        pauseTaskTimer()
    }

    fun handleTaskResume(task: Task) {
        viewModel.updateTask(
            task.copy(
                isCompleted = false,
                completedDate = null,
                progress = 0f,
                elapsedTime = 0,
                isRunning = false
            )
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = navController.currentDestination?.route == screen.route,
                        onClick = { navController.navigate(screen.route) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    tasks = tasks,
                    onAddTask = { newTask ->
                        viewModel.addTask(newTask)
                    },
                    onTaskStatusChange = { updatedTask ->
                        if (updatedTask.isCompleted) {
                            handleTaskCompletion(updatedTask)
                        } else {
                            handleTaskResume(updatedTask)
                        }
                    },
                    onShowAddDialog = { showAddTaskDialog = true },
                    onStartPauseClicked = { task -> handleTaskAction(task) },
                    onTaskDeleted = { taskToDelete ->
                        if (taskToDelete.isRunning) {
                            pauseTaskTimer()
                        }
                        viewModel.deleteTask(taskToDelete)
                    }
                )
            }
            composable(Screen.Contributions.route) {
                ContributionsScreen(completedTasks = tasks.filter { it.isCompleted })
            }
            composable(Screen.Progress.route) {
                ProgressScreen(tasks = tasks)
            }
        }

        // In your TaskBookApp composable
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskAdded = { name, description, estimatedTime, hours, minutes, seconds ->
                    val newTask = Task(
                        id = tasks.size,
                        name = name,
                        description = description,
                        estimatedTime = estimatedTime,
                        startTime = LocalTime.now(),
                        assignedHours = hours,
                        assignedMinutes = minutes,
                        assignedSeconds = seconds,
                        date = LocalDate.now()
                    )
                    viewModel.addTask(newTask)
                    showAddTaskDialog = false
                }
            )
        }
    }
}


@Composable
fun HomeScreen(
    tasks: List<Task>,
    onAddTask: (Task) -> Unit,
    onTaskStatusChange: (Task) -> Unit,
    onShowAddDialog: () -> Unit,
    onStartPauseClicked: (Task) -> Unit,
    onTaskDeleted: (Task) -> Unit
) {
    // State to track FAB position
    var fabOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val ongoingTasks = tasks.filter { !it.isCompleted }
        val completedTasks = tasks.filter { it.isCompleted }

        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome! Add your first task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                if (ongoingTasks.isNotEmpty()) {
                    Text(
                        "Ongoing Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TaskList(
                        tasks = ongoingTasks,
                        onTaskStatusChange = onTaskStatusChange,
                        onTaskDeleted = onTaskDeleted,
                        showResumeButton = false,
                        onStartPauseClicked = onStartPauseClicked
                    )
                }

                if (completedTasks.isNotEmpty()) {
                    Text(
                        "Completed Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TaskList(
                        tasks = completedTasks,
                        onTaskStatusChange = onTaskStatusChange,
                        onTaskDeleted = onTaskDeleted,
                        showResumeButton = true,
                        onStartPauseClicked = onStartPauseClicked
                    )
                }
            }
        }

        // Draggable FAB
        FloatingActionButton(
            onClick = onShowAddDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd) // Position at bottom right initially
                .offset { IntOffset(fabOffset.x.roundToInt(), fabOffset.y.roundToInt()) } // Apply drag offset
                .padding(bottom = 16.dp, end = 16.dp) // Add padding to keep it away from edges
                .shadow(10.dp, shape = CircleShape)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffset = fabOffset + dragAmount
                    }
                }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task")
        }
    }
}
@Composable
fun ContributionsScreen(completedTasks: List<Task>) {
    val selectedDate = remember { mutableStateOf(LocalDate.now()) }
    val currentMonth = remember { mutableStateOf(YearMonth.now()) }

    // Group tasks by the completed date
    val tasksByDate: Map<LocalDate, List<Task>> = completedTasks.groupBy {
        LocalDate.parse(it.completedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF666666)) // Light background color
    ) {
        Text(
            "Total Contributions: ${completedTasks.size}",
            fontSize = 18.sp,
            color = Color(0xFF666666), // Lighter text color
            modifier = Modifier.padding(bottom = 16.dp)
        )


    // Display total contributions


        // Calendar view for displaying tasks
        CalendarView(
            tasksByDate = tasksByDate,
            selectedDate = selectedDate.value,
            currentMonth = currentMonth.value,
            onDateSelected = { date -> selectedDate.value = date },
            onMonthChange = { newMonth -> currentMonth.value = newMonth }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display tasks for the selected date
        val tasksForSelectedDate = tasksByDate[selectedDate.value] ?: emptyList()

        LazyColumn {
            if (tasksForSelectedDate.isNotEmpty()) {
                items(tasksForSelectedDate) { task ->
                    TaskCard(task)
                }
            } else {
                item {
                    Text(
                        "No tasks completed on this day.",
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)), // Add shadow for elevation
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White) // Set the background color here
                .padding(16.dp)
        ) {
            Column {
                Text(
                    task.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF333333) // Darker text color
                )
                Text(
                    "Completed on: ${task.completedDate}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
@Composable
fun CalendarView(
    tasksByDate: Map<LocalDate, List<Task>>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Set the background color to white
    ) {
        Column {
            // Month and Year with Navigation Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    modifier = Modifier
                        .clickable { onMonthChange(currentMonth.minusMonths(1)) }
                        .padding(16.dp)
                        .size(32.dp)
                )

                Text(
                    text = "${currentMonth.month.name} ${currentMonth.year}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF333333) // Darker text color
                )

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    modifier = Modifier
                        .clickable { onMonthChange(currentMonth.plusMonths(1)) }
                        .padding(16.dp)
                        .size(32.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        day,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        color = Color(0xFF888888) // Lighter text color for day headers
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Empty boxes for days before the first day of the month
                items(firstDayOfMonth) {
                    Box(modifier = Modifier.height(40.dp))
                }

                // Days of the current month
                items(daysInMonth) { day ->
                    val date = currentMonth.atDay(day + 1)
                    val hasTasks = tasksByDate[date]?.isNotEmpty() == true
                    val isSelected = date == selectedDate

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.Blue else Color.Transparent
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (day + 1).toString(),
                                color = if (isSelected) Color.Blue else Color.Black // Highlight selected date
                            )
                            if (hasTasks) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Green, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ProgressScreen(tasks: List<Task>) {
    var selectedRange by remember { mutableStateOf(DateRange.TODAY) }
    var customStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)) // Light background for better contrast
    ) {
        Text(
            text = "Progress Overview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Date Range Selection with horizontal scroll
        DateRangeSelector(
            selectedRange = selectedRange,
            onRangeSelected = {
                selectedRange = it
                if (it != DateRange.CUSTOM) {
                    customStartDate = null
                    customEndDate = null
                }
            }
        )

        // Custom date range picker (if CUSTOM is selected)
        if (selectedRange == DateRange.CUSTOM) {
            CustomDateRangePicker(
                startDate = customStartDate,
                endDate = customEndDate,
                onStartDateSelected = { customStartDate = it },
                onEndDateSelected = { customEndDate = it }
            )
        }

        // Filter tasks based on selected date range
        val filteredTasks = filterTasksByDateRange(
            tasks,
            selectedRange,
            customStartDate,
            customEndDate
        )

        // Calculate time spent for filtered tasks
        val timeSpentByTask = filteredTasks.map {
            val timeSpent = (it.estimatedTime * it.progress).toInt()
            it to timeSpent
        }

        val totalTimeSpent = timeSpentByTask.sumOf { it.second }

        Text(
            text = "Total Time Spent: ${formatTime(totalTimeSpent)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // If tasks are available, show task distribution and pie chart
        if (filteredTasks.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp)) // Shadow for depth
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Task Distribution",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    CustomPieChart(
                        timeSpentByTask = timeSpentByTask,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(timeSpentByTask) { (task, timeSpent) ->
                            LegendItem(
                                taskName = task.name,
                                color = taskColor(filteredTasks.indexOf(task)),
                                timeSpent = formatTime(timeSpent),
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No tasks found for selected date range",
                fontSize = 16.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}


@Composable
fun DateRangeSelector(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(DateRange.values().toList()) { range ->
            Button(
                onClick = { onRangeSelected(range) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRange == range)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    range.name,
                    color = if (selectedRange == range)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CustomDateRangePicker(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DatePickerButton(
            label = "Start Date",
            date = startDate,
            onDateSelected = onStartDateSelected // Fixed syntax here
        )
        DatePickerButton(
            label = "End Date",
            date = endDate,
            onDateSelected = onEndDateSelected // Fixed syntax here
        )
    }
}

@Composable
fun DatePickerButton(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    Button(onClick = { showDatePicker = true }) {
        Text(date?.toString() ?: label)
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth)
                onDateSelected(selectedDate)
                showDatePicker = false
            },
            year, month, day
        ).show()
    }
}

@Composable
fun LegendItem(
    taskName: String,
    color: Color,
    timeSpent: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        Text(
            text = taskName,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Text(text = timeSpent)
    }
}

fun filterTasksByDateRange(
    tasks: List<Task>,
    range: DateRange,
    customStartDate: LocalDate?,
    customEndDate: LocalDate?
): List<Task> {
    val today = LocalDate.now()

    return when (range) {
        DateRange.TODAY -> tasks.filter { it.date == today }
        DateRange.WEEK -> tasks.filter {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            it.date in startOfWeek..endOfWeek
        }
        DateRange.MONTH -> tasks.filter {
            val startOfMonth = today.withDayOfMonth(1)
            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
            it.date in startOfMonth..endOfMonth
        }
        DateRange.YEAR -> tasks.filter {
            val startOfYear = today.withDayOfYear(1)
            val endOfYear = today.withDayOfYear(today.lengthOfYear())
            it.date in startOfYear..endOfYear
        }
        DateRange.CUSTOM -> {
            if (customStartDate != null && customEndDate != null) {
                tasks.filter { it.date in customStartDate..customEndDate }
            } else {
                tasks
            }
        }
    }
}

fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val seconds = (minutes * 60) % 60
    return String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds)
}

@Composable
fun CustomPieChart(
    timeSpentByTask: List<Pair<Task, Int>>, // List of tasks with their respective time spent
    modifier: Modifier = Modifier
) {
    val total = timeSpentByTask.sumOf { it.second }
    if (total == 0) return

    // Calculate each task's angle in the pie chart based on time spent
    var startAngle = 0f
    Canvas(modifier = modifier) {
        timeSpentByTask.forEachIndexed { index, (task, timeSpent) ->
            val sweepAngle = 360f * (timeSpent.toFloat() / total)

            // Draw the arc for each task with a different color
            drawArc(
                color = taskColor(index), // Use unique color for each task
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = size
            )

            // Update the start angle for the next arc
            startAngle += sweepAngle
        }
    }
}

enum class DateRange {
    TODAY, WEEK, MONTH, YEAR, CUSTOM
}


// Helper function to assign a unique color to each task
fun taskColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF5722), // Orange
        Color(0xFFF44336), // Red
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF3F51B5), // Indigo
        Color(0xFFCDDC39), // Lime
        Color(0xFF009688), // Teal
        Color(0xFF673AB7), // Deep Purple
        Color(0xFFFF9800), // Amber
        Color(0xFF8BC34A), // Light Green
        Color(0xFF795548)  // Brown
    )
    return colors[index % colors.size] // Cycles through the colors
}


