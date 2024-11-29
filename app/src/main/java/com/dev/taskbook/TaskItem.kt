package com.dev.taskbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: (String, String, Int, Int, Int, Int) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(20.dp)),
        containerColor = Color.White,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6750A4), shape = RoundedCornerShape(20.dp)) // Apply rounded corners directly to the background
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Add New Task",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center) // Align the text to the center of the box
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = Color(0xFFF8F6FF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniPickerItem(
                            label = "HH",
                            value = hours,
                            onValueChange = { hours = it },
                            range = 0..23
                        )
                        Text(":", color = Color(0xFF6750A4))
                        MiniPickerItem(
                            label = "MM",
                            value = minutes,
                            onValueChange = { minutes = it },
                            range = 0..59
                        )
                        Text(":", color = Color(0xFF6750A4))
                        MiniPickerItem(
                            label = "SS",
                            value = seconds,
                            onValueChange = { seconds = it },
                            range = 0..59
                        )
                    }
                }

                if (showError) {
                    Text(
                        "Please fill all fields. Time must be positive.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val estimatedTimeInMinutes = (hours * 60 * 60 + minutes * 60 + seconds) / 60
                    if (taskName.isNotEmpty() && description.isNotEmpty() && estimatedTimeInMinutes > 0) {
                        onTaskAdded(taskName, description, estimatedTimeInMinutes, hours, minutes, seconds)
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
            ) {
                Text(
                    "Add",
                    color = Color(0xFF6750A4),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color(0xFF6750A4),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
fun MiniPickerItem(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(50.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xFF6750A4)
        )
        Box(modifier = Modifier.height(80.dp)) {
            MiniVerticalNumberPicker(
                value = value,
                onValueChange = onValueChange,
                range = range
            )
        }
        Text(
            String.format("%02d", value),
            fontSize = 14.sp,
            color = Color(0xFF6750A4),
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun MiniVerticalNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    val scrollState = rememberLazyListState()
    val numbers = range.toList()

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(numbers) { number ->
            val isSelected = number == value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(number) }
                    .background(
                        if (isSelected) Color(0xFFEADDFF)
                        else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(vertical = 2.dp)  // Reduced padding
            ) {
                Text(
                    text = String.format("%02d", number),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 13.sp,  // Smaller font
                    color = if (isSelected) Color(0xFF6750A4)
                    else Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }

    LaunchedEffect(value) {
        val index = numbers.indexOf(value)
        scrollState.scrollToItem(index)
    }
}

@Composable
fun TaskItem(
    task: Task,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onTaskStatusChange: (Task) -> Unit,
    onTaskDeleted: (Task) -> Unit,
    onStartPauseClicked: (Task) -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    val cardElevation by animateFloatAsState(
        targetValue = if (isExpanded) 8f else 2f,
        label = "elevation"
    )

    fun formatTimeRemaining(estimatedTimeMinutes: Int, elapsedSeconds: Int): String {
        val totalSeconds = estimatedTimeMinutes * 60
        val remainingSeconds = maxOf(totalSeconds - elapsedSeconds, 0)
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpand() }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                when {
                                    task.isCompleted -> MaterialTheme.colorScheme.tertiary
                                    task.isRunning -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onExpand() }) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(rotationState)
                        )
                    }
                    if (!task.isCompleted) {
                        IconButton(onClick = { onTaskDeleted(task) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // Description
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Progress section
                    if (!task.isCompleted) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${(task.progress * 100).toInt()}% Complete",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = task.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            Text(
                                text = "Time remaining: ${formatTimeRemaining(task.estimatedTime, task.elapsedTime)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // Control buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start/Resume Button
                                Button(
                                    onClick = { onStartPauseClicked(task) },
                                    enabled = !task.isRunning,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (task.progress == 0f) Icons.Default.PlayArrow
                                            else Icons.Default.Refresh,
                                            contentDescription = if (task.progress == 0f) "Start task"
                                            else "Resume task",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (task.progress == 0f) "Start" else "Resume",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                // Pause Button
                                Button(
                                    onClick = { onStartPauseClicked(task) },
                                    enabled = task.isRunning,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "Pause task",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pause",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Completed task details section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Task Completed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Completed on: ${task.completedDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            task.getTotalAssignedTimeFormatted()?.let { assignedTime ->
                                Text(
                                    text = "Total Assigned Time: $assignedTime",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            task.getCompletionTimeFormatted()?.let { completionTime ->
                                Text(
                                    text = "Actual Completion Time: $completionTime",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
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
fun TaskList(
    tasks: List<Task>,
    onTaskStatusChange: (Task) -> Unit,
    onTaskDeleted: (Task) -> Unit,
    showResumeButton: Boolean,
    onStartPauseClicked: (Task) -> Unit
) {
    var expandedTaskId by remember { mutableStateOf<Int?>(null) }

    LazyColumn {
        items(tasks) { task ->
            TaskItem(
                task = task,
                isExpanded = task.id == expandedTaskId,
                onExpand = {
                    expandedTaskId = if (expandedTaskId == task.id) null else task.id
                },
                onTaskStatusChange = onTaskStatusChange,
                onTaskDeleted = onTaskDeleted,
                onStartPauseClicked = onStartPauseClicked
            )
        }
    }
}