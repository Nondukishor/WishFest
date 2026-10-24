package com.nipu.wishfest.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nipu.wishfest.domain.models.*
import com.nipu.wishfest.ui.components.*
import com.nipu.wishfest.ui.home.HomeViewModel
import com.nipu.wishfest.ui.home.TemplateItem

@Composable
fun TemplatesScreen(
    viewModel: HomeViewModel,
    onOpenTemplate: (ProjectModel) -> Unit,
    onOpenHome: (() -> Unit)? = null,
    onOpenProjects: (() -> Unit)? = null,
    onCreate: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    var category by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    val templates = remember(state.festivals, category, query) {
        state.festivals.filter { category == "All" || it.category == category }.flatMap { festival ->
            festival.templates.filter { template ->
                "${festival.name} ${template.title} ${template.layers.filterIsInstance<LayerModel.Text>().joinToString(" ") { it.text }}"
                    .contains(query.trim(), ignoreCase = true)
            }
        }
    }
    Scaffold(bottomBar = {
        if (onOpenHome != null && onOpenProjects != null && onCreate != null) {
            WishBottomBar(WishTab.Templates, onOpenHome, {}, onCreate, onOpenProjects)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Find your perfect card", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
            Text("Choose a design. Make it yours.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search occasions or wishes") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Clear search") } },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { name ->
                    FilterChip(
                        selected = category == name, onClick = { category = name }, label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("${templates.size} designs", style = MaterialTheme.typography.labelLarge)
                if (category != "All" || query.isNotEmpty()) TextButton(onClick = { category = "All"; query = "" }) { Text("Reset filters") }
            }
            if (templates.isEmpty()) {
                EmptyStatePanel(title = "No matching cards", message = "Try another occasion or reset your filters.")
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates, key = { it.id }) { template ->
                        LargeTemplateCard(template) { onOpenTemplate(viewModel.createProjectFromTemplate(template)) }
                    }
                }
            }
        }
    }
}

@Composable
fun LargeTemplateCard(template: TemplateItem, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column {
            val project = remember(template) { ProjectModel(template.id, template.id, template.title,
                template.canvas, template.layers) }
            CardPreview(project, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(Modifier.padding(12.dp)) {
                Text(template.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tap to personalize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
