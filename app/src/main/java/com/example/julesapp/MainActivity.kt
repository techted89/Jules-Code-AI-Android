package com.example.julesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.julesapp.api.Project
import com.example.julesapp.api.RetrofitInstance
import com.example.julesapp.ui.screens.ChatScreen
import com.example.julesapp.ui.screens.ProjectSelectionScreen
import com.example.julesapp.ui.theme.JulesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the singleton Retrofit instance
        val apiService = RetrofitInstance.api

        setContent {
            JulesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JulesAppNavigation(apiService)
                }
            }
        }
    }
}

@Composable
fun JulesAppNavigation(apiService: com.example.julesapp.api.JulesApiService) {
    val navController = rememberNavController()
    var currentProject by remember { mutableStateOf<Project?>(null) }

    NavHost(navController = navController, startDestination = "project_selection") {
        composable("project_selection") {
            ProjectSelectionScreen(
                apiService = apiService,
                onProjectSelected = { project ->
                    currentProject = project
                    navController.navigate("chat")
                }
            )
        }
        composable("chat") {
            if (currentProject != null) {
                ChatScreen(
                    project = currentProject!!,
                    apiService = apiService,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
