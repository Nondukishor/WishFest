package com.nipu.wishfest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nipu.wishfest.ai.AiWishGenerator
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.ui.components.WishBottomBar
import com.nipu.wishfest.ui.components.WishScreenBackground
import com.nipu.wishfest.ui.components.WishTab
import com.nipu.wishfest.ui.theme.Blush
import com.nipu.wishfest.ui.theme.Marigold
import com.nipu.wishfest.ui.theme.MarigoldSoft
import com.nipu.wishfest.ui.theme.SurfaceOverlay
import com.nipu.wishfest.ui.theme.SurfaceRaised
import kotlinx.coroutines.launch

fun getEffectiveCardTextColor(layerColorHex: String?, bgColorHex: String): Color {
    val isLightBg = try {
        val parseColor = if (bgColorHex.startsWith("gradient:")) {
            val hexes = bgColorHex.removePrefix("gradient:").split(",")
            android.graphics.Color.parseColor(hexes[0].trim())
        } else if (bgColorHex.startsWith("#")) {
            android.graphics.Color.parseColor(bgColorHex.trim())
        } else {
            android.graphics.Color.WHITE
        }
        val red = android.graphics.Color.red(parseColor)
        val green = android.graphics.Color.green(parseColor)
        val blue = android.graphics.Color.blue(parseColor)
        (red * 0.299 + green * 0.587 + blue * 0.114) > 180
    } catch (e: Exception) {
        false
    }

    if (layerColorHex != null && layerColorHex.startsWith("#")) {
        try {
            if (isLightBg && (layerColorHex.equals("#FFFFFF", ignoreCase = true) || layerColorHex.equals("#FFF8DC", ignoreCase = true))) {
                return Color(0xFF1B4332)
            }
            return Color(android.graphics.Color.parseColor(layerColorHex))
        } catch (e: Exception) { }
    }

    return if (isLightBg) Color(0xFF1B4332) else Color.White
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenTemplate: (ProjectModel) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    requestAiCard: Boolean = false,
    onAiRequestHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiWishGenerator = remember { AiWishGenerator() }
    val state by viewModel.uiState.collectAsState()

    var showAiCardDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(requestAiCard) {
        if (requestAiCard) {
            showAiCardDialog = true
            onAiRequestHandled()
        }
    }
    var aiPrompt by remember { mutableStateOf("") }
    var aiRecipient by remember { mutableStateOf("Loved One") }
    var aiLanguage by remember { mutableStateOf("English") }
    var isGeneratingCard by remember { mutableStateOf(false) }

    val languages = listOf("English", "Hindi (हिंदी)", "Tamil (தமிழ்)", "German (Deutsch)", "Bengali (বাংলা)", "Arabic (العربية)")

    if (showAiCardDialog) {
        AlertDialog(
            onDismissRequest = { showAiCardDialog = false },
            containerColor = SurfaceRaised,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Marigold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Create a custom card",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tell us the occasion and who it’s for. You can edit every detail afterward.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Occasion") },
                        placeholder = { Text("Durga Puja, Sunday brunch…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Marigold,
                            cursorColor = Marigold
                        )
                    )
                    OutlinedTextField(
                        value = aiRecipient,
                        onValueChange = { aiRecipient = it },
                        label = { Text("Recipient") },
                        placeholder = { Text("Mom, Friend…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Marigold,
                            cursorColor = Marigold
                        )
                    )

                    Text(
                        "Language",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(languages) { lang ->
                            FilterChip(
                                selected = aiLanguage == lang,
                                onClick = { aiLanguage = lang },
                                label = { Text(lang, fontSize = 11.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Marigold,
                                    selectedLabelColor = Color(0xFF1A1205),
                                    containerColor = SurfaceOverlay,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isGeneratingCard,
                    onClick = {
                        isGeneratingCard = true
                        scope.launch {
                            val generatedProj = aiWishGenerator.generateCreativeCard(aiPrompt, aiRecipient, aiLanguage)
                            isGeneratingCard = false
                            showAiCardDialog = false
                            onOpenTemplate(generatedProj)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Marigold,
                        contentColor = Color(0xFF1A1205)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isGeneratingCard) "Creating…" else "Generate card",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiCardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    WishScreenBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                WishBottomBar(
                    selected = WishTab.Home,
                    onHome = { },
                    onTemplates = onOpenTemplates,
                    onCreate = onCreateCard,
                    onProjects = onOpenProjects
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    HomeHeader(onOpenSettings = onOpenSettings)
                }

                item {
                    CreateHeroCard(onCreate = onCreateCard)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.nipu.wishfest.ui.components.QuickAccessCard("Templates", "Find your next design",
                            Icons.Default.Palette, onOpenTemplates, Modifier.weight(1f))
                        com.nipu.wishfest.ui.components.QuickAccessCard("My cards", "Pick up where you left off",
                            Icons.Default.FolderOpen, onOpenProjects, Modifier.weight(1f))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    SearchField(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.categories) { category ->
                            val selected = state.selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = {
                                    Text(
                                        category,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Marigold,
                                    selectedLabelColor = Color(0xFF1A1205),
                                    containerColor = SurfaceRaised,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                if (state.filteredFestivals.isEmpty()) {
                    item {
                        com.nipu.wishfest.ui.components.EmptyStatePanel(title = "No occasions found",
                            message = "Try a different search or choose All.")
                        TextButton(onClick = { viewModel.onSearchQueryChanged(""); viewModel.onCategorySelected("All") },
                            modifier = Modifier.padding(horizontal = 20.dp)) { Text("Show all occasions") }
                    }
                }

                items(state.filteredFestivals) { festival ->
                    Spacer(modifier = Modifier.height(28.dp))
                    FestivalSection(
                        festival = festival,
                        onTemplateSelected = { template ->
                            val project = viewModel.createProjectFromTemplate(template)
                            onOpenTemplate(project)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "WishFest",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Greetings worth sending",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceRaised)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateHeroCard(onCreate: () -> Unit) {
    Surface(
        onClick = onCreate,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3D304B),
                            Color(0xFF322736),
                            Color(0xFF232330)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Marigold.copy(alpha = 0.18f)
                ) {
                    Text(
                        "A little thought. A lasting smile.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MarigoldSoft,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Make their day.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Start with AI, a template, or your own idea. Make every wish personal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Marigold
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF1A1205),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Create a card",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1A1205)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceRaised,
        tonalElevation = 0.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search occasions…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Marigold
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FestivalSection(
    festival: FestivalItem,
    onTemplateSelected: (TemplateItem) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(festival.icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    festival.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (festival.tagline.isNotEmpty()) {
                    Text(
                        festival.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(festival.templates) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: TemplateItem,
    onClick: () -> Unit
) {
    Box(Modifier.width(160.dp)) {
        com.nipu.wishfest.ui.templates.LargeTemplateCard(template, onClick)
    }
}
