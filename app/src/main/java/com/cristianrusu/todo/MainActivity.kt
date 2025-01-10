package com.cristianrusu.todo

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

data class ToDoEntity(
    val id: String = "",
    var title: String = "",
    var done: Boolean = false,
    var tags: MutableSet<String> = mutableSetOf()
) {
    // Convert from Firestore document to ToDoEntity
    constructor(document: DocumentSnapshot) : this(
        id = document.id,
        title = document.getString("title") ?: "",
        done = document.getBoolean("done") ?: false,
        tags = (document.get("tags") as? List<String>)?.toMutableSet() ?: mutableSetOf()
    )

    // Convert ToDoEntity to Firestore document
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "done" to done,
            "tags" to tags.toList() // Firestore requires lists for arrays
        )
    }
}


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
    var toDoList = remember { mutableStateListOf<ToDoEntity>() } // connect to db
    var newTaskTitle by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        getTodosFromFirestore(
            onSuccess = { todos ->
                toDoList = todos.toMutableStateList()
                Log.i("Firebase", toDoList.first().id.toString())
            },
            onFailure = { e ->
                Log.e("Firebase", "Error getting documents: ", e)
            }
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("To-Do Tracker") },
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
                                val newTask = ToDoEntity(title = newTaskTitle, tags = tags.toMutableSet())
                                saveToDoToFirestore(newTask)
                                toDoList.add(newTask)
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
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(toDoList.size) { index ->
                        val task = toDoList[index]
                        TaskItemCard(
                            task = task,
                            onRemove = { toDoList.removeAt(index) },
                            onToggleDone = { isChecked ->
                                val updatedTask = task.copy(done = isChecked)
                                toDoList[index] = updatedTask
                                updateToDoFromFirestore(updatedTask)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if(toDoList.any{item -> item.done}) {
                    Button(
                        onClick = {
                            val toDoRemove = toDoList.filter { item -> item.done }
                            toDoList.removeIf{item -> item in toDoRemove}
                            toDoRemove.forEach { item -> deleteTodoFromFirestore(item.id,onFailure = { error ->
                                Log.e("Firestore", "Error deleting from Firestore")
                            })}},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Completed")
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
                IconButton(
                    onClick = {
                        deleteTodoFromFirestore(task.id, onFailure = { error ->
                            Log.e("Firestore", "Error deleting from Firestore")
                        })
                        onRemove()
                    }) {
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

// Function to save a ToDo entity to Firestore
fun saveToDoToFirestore(todo: ToDoEntity) {
    val db = FirebaseFirestore.getInstance()
    val todosCollection = db.collection("todos")

    // Adding the ToDo as a new document in Firestore
    todosCollection.add(todo.toMap())
        .addOnSuccessListener { documentReference ->
            Log.d("Firebase", "DocumentSnapshot added with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.w("Firebase", "Error adding document", e)
        }
}

fun updateToDoFromFirestore(todo: ToDoEntity) {
    val db = FirebaseFirestore.getInstance()
    val todosCollection = db.collection("todos")

    todosCollection.document(todo.id).update(todo.toMap())
        .addOnSuccessListener { documentReference ->
            Log.d("Firebase", "DocumentSnapshot updated with ID:")
        }
        .addOnFailureListener { e ->
            Log.w("Firebase", "Error updating document", e)
        }
}

// Function to fetch todos from Firestore
fun getTodosFromFirestore(onSuccess: (List<ToDoEntity>) -> Unit, onFailure: (Exception) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val todosCollection = db.collection("todos")

    todosCollection.get()
        .addOnSuccessListener { result: QuerySnapshot ->
            val todos = result.documents.map { document ->
                ToDoEntity(document) // Convert the Firestore document to ToDoEntity
            }
            onSuccess(todos)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

// Function to delete todo from Firestore
fun deleteTodoFromFirestore(id: String, onFailure: (Exception) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val todosCollection = db.collection("todos")

    todosCollection.document(id).delete()
        .addOnSuccessListener {
            Log.i("Firestore", "Successfully deleted todo with id: $id")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error deleting todo with id: $id", e)
            onFailure(e)
        }
}

