package com.example.julesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.julesapp.api.ChatRequest
import com.example.julesapp.api.JulesApiService
import com.example.julesapp.api.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    project: Project,
    apiService: JulesApiService,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("Ready") }
    var progress by remember { mutableStateOf(0.0f) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var messageQueue by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    // Function to send a message
    fun sendMessage(msgToSend: String) {
        // Optimistically set status to Busy IMMEDIATELY to prevent race condition in LaunchedEffect
        status = "Busy"
        chatHistory = chatHistory + "You: $msgToSend"
        scope.launch {
            try {
                val response = apiService.sendMessage(ChatRequest(project.id, msgToSend))
                chatHistory = chatHistory + "Jules: ${response.message}"
                status = response.status
                progress = response.progress
            } catch (e: Exception) {
                chatHistory = chatHistory + "Error: ${e.message}"
                status = "Ready" // Reset status on error so queue can continue (or user can retry)
            }
        }
    }

    // Polling for status
    LaunchedEffect(project.id) {
        while (true) {
            try {
                val statusResponse = apiService.getStatus(project.id)
                status = statusResponse.status
                progress = statusResponse.progress
                if (statusResponse.logs.isNotEmpty()) {
                    logs = statusResponse.logs
                }
            } catch (e: Exception) {
                // Ignore polling errors for smoother UI, or log them
            }
            delay(5000)
        }
    }

    // Auto-send from queue when Ready
    LaunchedEffect(status, messageQueue) {
        if (status == "Ready" && messageQueue.isNotEmpty()) {
            val nextMessage = messageQueue.first()
            messageQueue = messageQueue.drop(1)
            sendMessage(nextMessage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        SmallTopAppBar(
            title = { Text(project.name) },
            navigationIcon = {
                Button(onClick = onBack) { Text("Back") }
            }
        )

        // Split view: Top is Status/Logs, Bottom is Chat
        Column(modifier = Modifier.weight(1f)) {
            // Status Window
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Status: $status", style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Logs:", style = MaterialTheme.typography.labelSmall)
                    LazyColumn {
                        items(logs) { log ->
                            Text(log, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Chat Window
            LazyColumn(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(8.dp)
            ) {
                items(chatHistory) { chatMsg ->
                    Text(
                        text = chatMsg,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                            .padding(8.dp)
                    )
                }
            }

            // Queue Display (if items queued)
            if (messageQueue.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.1f)
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text("Queued (${messageQueue.size}):", style = MaterialTheme.typography.labelSmall)
                        LazyColumn {
                            items(messageQueue) { queuedMsg ->
                                Text("• $queuedMsg", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (status == "Ready") "Ask Jules..." else "Add to queue...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (message.isNotBlank()) {
                    val msgToSend = message
                    message = ""
                    if (status == "Ready" && messageQueue.isEmpty()) {
                        sendMessage(msgToSend)
                    } else {
                        messageQueue = messageQueue + msgToSend
                    }
                }
            }) {
                Text(if (status == "Ready" && messageQueue.isEmpty()) "Send" else "Queue")
            }
        }
    }
}

@Composable
fun SmallTopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationIcon()
        Spacer(modifier = Modifier.width(16.dp))
        ProvideTextStyle(value = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onPrimary)) {
            title()
        }
    }
}
