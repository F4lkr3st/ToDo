package com.cristianrusu.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cristianrusu.todo.ui.theme.ToDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoTheme {
                ToDoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoApp() {
    var tasks by remember { mutableStateOf(listOf<String>()) }
    var newTask by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                            .padding(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                tasks = tasks + newTask
                                newTask = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                Row {
                    Button(
                        onClick = {
                            tasks = listOf()  // Clear all tasks
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear All Tasks") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks.size) { index ->
                        TaskItem(task = tasks[index], onRemove = {
                            tasks = tasks.toMutableList().apply { removeAt(index) }
                        })
                    }
                }
            }
        }
    )
}

@Composable
fun TaskItem(task: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(task, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ToDoAppPreview() {
    ToDoTheme {
        ToDoApp()
    }
}
