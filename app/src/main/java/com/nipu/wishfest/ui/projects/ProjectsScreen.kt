package com.nipu.wishfest.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.ui.components.EmptyStatePanel
import com.nipu.wishfest.ui.components.WishScreenBackground
import com.nipu.wishfest.ui.theme.Marigold
import com.nipu.wishfest.ui.theme.SurfaceRaised
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onOpenProject: (ProjectModel) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenHome: (() -> Unit)? = null,
    onOpenTemplates: (() -> Unit)? = null,
    onCreate: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<ProjectModel?>(null) }
    pendingDelete?.let { project ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this card?") }, text = { Text("“${project.title}” will be removed from My cards.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProject(project.id); pendingDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep card") } })
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    WishScreenBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (onOpenHome != null && onOpenTemplates != null && onCreate != null) {
                    com.nipu.wishfest.ui.components.WishBottomBar(
                        com.nipu.wishfest.ui.components.WishTab.Projects, onOpenHome, onOpenTemplates, onCreate, {})
                }
            },
            topBar = {
                RowTopBar(
                    title = "My cards",
                    onNavigateBack = onNavigateBack
                )
            }
        ) { paddingValues ->
            if (state.projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyStatePanel(icon = Icons.Outlined.FolderOpen,
                            title = "Your next lovely card starts here",
                            message = "Cards you create are saved here as you edit.")
                        if (onCreate != null) Button(onClick = onCreate) { Text("Create your first card") }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(state.projects) { project ->
                        ProjectCard(
                            project = project,
                            onOpen = { onOpenProject(project) },
                            onDelete = { pendingDelete = project }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowTopBar(title: String, onNavigateBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceRaised)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun ProjectCard(
    project: ProjectModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.updatedAt) {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        sdf.format(Date(project.updatedAt))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceRaised,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    
            ) {
                com.nipu.wishfest.ui.components.CardPreview(project, Modifier.fillMaxSize())

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .align(Alignment.TopEnd)
                        .clickable(onClick = onDelete)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${project.title}",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }


            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    project.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Marigold,
                        contentColor = Color(0xFF1A1205)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Edit card", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
