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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cristianrusu.todo.ui.theme.ToDoTheme

data class ToDoEntity(
    var title: String,
    var done: Boolean = false,
    var tags: MutableSet<String> = mutableSetOf()
)

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
    val toDoList = remember { mutableStateListOf<ToDoEntity>() }
    var newTaskTitle by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateMapOf<String, Boolean>() }

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
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val tags = selectedTags.filterValues { it }.keys
                                toDoList.add(ToDoEntity(newTaskTitle, tags = tags.toMutableSet()))
                                newTaskTitle = ""
                                selectedTags.clear()
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()){
                    TagSelectionRow(selectedTags = selectedTags)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { toDoList.removeIf{item -> item.done} },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Completed")
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(toDoList.size) { index ->
                        val task = toDoList[index]
                        TaskItemCard(
                            task = task,
                            onRemove = { toDoList.removeAt(index) },
                            onToggleDone = { isChecked ->
                                toDoList[index] = task.copy(done = isChecked)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TaskItemCard(
    task: ToDoEntity,
    onRemove: () -> Unit,
    onToggleDone: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = onToggleDone
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task"
                    )
                }
            }
            // Display selected tags
            if (task.tags.isNotEmpty()) {
                Text(
                    text = task.tags.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 32.dp, top = 8.dp) // Indent to align with the title
                )
            }
        }
    }
}


@Composable
fun TagSelectionRow(selectedTags: MutableMap<String, Boolean>) {
    val tags = listOf("School", "Personal", "House")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = selectedTags[tag] ?: false
            TagChip(
                tag = tag,
                isChecked = isSelected,
                onCheckedChange = { isChecked ->
                    selectedTags[tag] = isChecked
                }
            )
        }
    }
}


@Composable
fun TagChip(tag: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
            Text(
                modifier = Modifier.padding(4.dp),
                text = tag,
                style = MaterialTheme.typography.bodySmall
            )
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
