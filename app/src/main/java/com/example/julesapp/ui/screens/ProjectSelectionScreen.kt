package com.example.julesapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.julesapp.api.CreateProjectRequest
import com.example.julesapp.api.JulesApiService
import com.example.julesapp.api.Project
import com.example.julesapp.api.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun ProjectSelectionScreen(
    apiService: JulesApiService,
    onProjectSelected: (Project) -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            projects = apiService.getProjects()
        } catch (e: Exception) {
            errorMessage = "Failed to load projects: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select a Project", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showApiKeyDialog = true }) {
                Text("API Key")
            }
        }
        
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { showCreateDialog = true }) {
            Text("Create New Project")
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(projects) { project ->
                ProjectItem(project = project, onClick = { onProjectSelected(project) })
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, repo, branch ->
                scope.launch {
                    try {
                        val newProject = apiService.createProject(CreateProjectRequest(name, repo, branch))
                        projects = projects + newProject
                        showCreateDialog = false
                        onProjectSelected(newProject)
                    } catch (e: Exception) {
                        errorMessage = "Failed to create project: ${e.message}"
                    }
                }
            }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                // In real app, persist this to SharedPreferences/DataStore
                RetrofitInstance.authInterceptor.setApiKey(key)
                showApiKeyDialog = false
                // Reload projects
                scope.launch {
                    try {
                        errorMessage = null
                        projects = apiService.getProjects()
                    } catch (e: Exception) {
                        errorMessage = "Failed to reload projects: ${e.message}"
                    }
                }
            }
        )
    }
}

@Composable
fun ProjectItem(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(project.name, style = MaterialTheme.typography.titleMedium)
            Text("Repo: ${project.repo ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${project.status}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Name") })
                OutlinedTextField(value = repo, onValueChange = { repo = it }, label = { Text("GitHub Repo") })
                OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") })
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, repo, branch) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set API Key") },
        text = {
            Column {
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(key) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
