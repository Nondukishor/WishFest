package com.nipu.wishfest

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.ui.editor.EditorScreen
import com.nipu.wishfest.ui.editor.EditorViewModel
import com.nipu.wishfest.ui.export.ExportScreen
import com.nipu.wishfest.ui.export.ExportViewModel
import com.nipu.wishfest.ui.home.HomeScreen
import com.nipu.wishfest.ui.home.HomeViewModel
import com.nipu.wishfest.ui.projects.ProjectsScreen
import com.nipu.wishfest.ui.projects.ProjectsViewModel
import com.nipu.wishfest.ui.settings.SettingsScreen
import com.nipu.wishfest.ui.templates.TemplatesScreen
import com.nipu.wishfest.ui.theme.WishfestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            WishfestTheme {
                val navController = rememberNavController()

                val homeViewModel: HomeViewModel = viewModel()
                val editorViewModel: EditorViewModel = viewModel()
                val projectsViewModel: ProjectsViewModel = viewModel()
                val exportViewModel: ExportViewModel = viewModel()

                var activeProjectForEditor by remember { mutableStateOf<ProjectModel?>(null) }
                var activeProjectForExport by remember { mutableStateOf<ProjectModel?>(null) }

                var showCreateMenu by remember { mutableStateOf(false) }
                var requestAiCard by remember { mutableStateOf(false) }

                fun openMainDestination(route: String) {
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                fun createBlankCard() {
                                activeProjectForEditor = ProjectModel(
                                    id = java.util.UUID.randomUUID().toString(), templateId = "blank", title = "My wish card",
                                    canvas = com.nipu.wishfest.domain.models.CanvasModel(backgroundColor = "#FFFBEB", borderStyle = "double"),
                                    layers = listOf(com.nipu.wishfest.domain.models.LayerModel.Text(
                                        id = java.util.UUID.randomUUID().toString(),
                                        transform = com.nipu.wishfest.domain.models.TransformModel(100f, 380f, 880f, 260f),
                                        text = "Sending you a little joy", fontId = "serif", fontSize = 64f, color = "#713F12"))
                                )
                                navController.navigate("editor")
                }

                if (showCreateMenu) {
                    com.nipu.wishfest.ui.components.CreateCardDialog(
                        onDismiss = { showCreateMenu = false },
                        onAi = {
                            showCreateMenu = false
                            requestAiCard = true
                            openMainDestination("home")
                        },
                        onBlank = { showCreateMenu = false; createBlankCard() },
                        onTemplates = { showCreateMenu = false; openMainDestination("templates") }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenTemplate = { project ->
                                activeProjectForEditor = project
                                navController.navigate("editor")
                            },
                            onOpenTemplates = {
                                openMainDestination("templates")
                            },
                            onOpenProjects = {
                                openMainDestination("projects")
                            },
                            onCreateCard = { showCreateMenu = true },
                            requestAiCard = requestAiCard,
                            onAiRequestHandled = { requestAiCard = false },
                            onOpenSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("templates") {
                        TemplatesScreen(
                            viewModel = homeViewModel,
                            onOpenTemplate = { project ->
                                activeProjectForEditor = project
                                navController.navigate("editor")
                            },
                            onOpenHome = {
                                navController.popBackStack("home", inclusive = false)
                            },
                            onOpenProjects = {
                                openMainDestination("projects")
                            },
                            onCreate = { showCreateMenu = true }
                        )
                    }

                    composable("editor") {
                        val project = activeProjectForEditor
                        if (project != null) {
                            EditorScreen(
                                initialProject = project,
                                viewModel = editorViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateExport = { exportProject ->
                                    activeProjectForExport = exportProject
                                    navController.navigate("export")
                                }
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    composable("projects") {
                        ProjectsScreen(
                            viewModel = projectsViewModel,
                            onOpenProject = { project ->
                                activeProjectForEditor = project
                                navController.navigate("editor")
                            },
                            onNavigateBack = { navController.popBackStack() },
                            onOpenHome = { navController.popBackStack("home", inclusive = false) },
                            onOpenTemplates = { openMainDestination("templates") },
                            onCreate = { showCreateMenu = true }
                        )
                    }

                    composable("export") {
                        val project = activeProjectForExport
                        if (project != null) {
                            ExportScreen(
                                project = project,
                                viewModel = exportViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
