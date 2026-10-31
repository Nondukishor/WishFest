package com.nipu.wishfest.ui.editor

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nipu.wishfest.ai.AiWishGenerator
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import com.nipu.wishfest.platform.AssetManager
import com.nipu.wishfest.rendering.SceneRenderer
import com.nipu.wishfest.ui.theme.Marigold
import com.nipu.wishfest.ui.theme.NeonAccent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

private class DoubleBitmapBuffer(width: Int, height: Int) {
    private var front: Bitmap = createBitmap(width, height)
    private var back: Bitmap = createBitmap(width, height)

    @Synchronized
    fun render(renderBlock: (Bitmap) -> Unit): Bitmap {
        back.eraseColor(android.graphics.Color.TRANSPARENT)
        renderBlock(back)
        val temp = front
        front = back
        back = temp
        return front
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialProject: ProjectModel,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateExport: (ProjectModel) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val assetManager = remember { AssetManager(context) }
    val sceneRenderer = remember { SceneRenderer(context) }

    LaunchedEffect(initialProject.id) {
        viewModel.initProject(initialProject)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val project = state.project ?: return@rememberLauncherForActivityResult
            val copiedPath = assetManager.copyUriToProjectAsset(project.id, uri)
            if (copiedPath != null) {
                viewModel.updateBackgroundPhoto(copiedPath)
            }
        }
    }

    val currentProject = state.project ?: initialProject
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedLayer = currentProject.layers.find { it.id == state.selectedLayerId }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var removalSnapshot by remember { mutableStateOf<ProjectModel?>(null) }

    // Undo belongs to this deletion only; dismiss it if another edit supersedes it.
    LaunchedEffect(currentProject) {
        if (removalSnapshot != null && currentProject != removalSnapshot) {
            snackbar.currentSnackbarData?.dismiss()
            removalSnapshot = null
        }
    }
    val deleteSelection: () -> Unit = {
        val layer = viewModel.uiState.value.project?.layers?.find { it.id == viewModel.uiState.value.selectedLayerId }
        if (layer != null && !layer.locked) {
            snackbar.currentSnackbarData?.dismiss()
            viewModel.deleteSelectedLayer()
            val snapshot = viewModel.uiState.value.project
            removalSnapshot = snapshot
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = "${layerLabel(layer)} removed", actionLabel = "Undo", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed && viewModel.uiState.value.project == snapshot) {
                    viewModel.undo()
                    viewModel.selectLayer(layer.id)
                }
                if (removalSnapshot == snapshot) removalSnapshot = null
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Edit card", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::undo,
                        enabled = state.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo last change")
                    }
                    IconButton(
                        onClick = viewModel::redo,
                        enabled = state.canRedo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo last change")
                    }
                    Button(
                        onClick = { onNavigateExport(currentProject) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            // Interactive Canvas Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardSizeDp = minOf(maxWidth * 0.95f, maxHeight * 0.95f)
                val doubleBuffer = remember { DoubleBitmapBuffer(720, 720) }
                var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var activeDraggingLayerId by remember { mutableStateOf<String?>(null) }
                var baseSceneBitmap by remember { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(currentProject, activeDraggingLayerId) {
                    val draggingId = activeDraggingLayerId
                    val base = baseSceneBitmap

                    val b = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        doubleBuffer.render { target ->
                            if (draggingId != null && base != null && !base.isRecycled) {
                                val activeLayer = currentProject.layers.find { it.id == draggingId }
                                if (activeLayer != null) {
                                    sceneRenderer.renderLayerOntoBase(base, currentProject, activeLayer, 720, 720, reuseBitmap = target)
                                } else {
                                    sceneRenderer.renderToBitmap(currentProject, 720, 720, reuseBitmap = target)
                                }
                            } else {
                                sceneRenderer.renderToBitmap(currentProject, 720, 720, reuseBitmap = target)
                            }
                        }
                    }
                    if (isActive) {
                        currentBitmap = b
                    }
                }
                val previewBitmap = currentBitmap ?: remember { sceneRenderer.renderToBitmap(currentProject, 720, 720) }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier.size(cardSizeDp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Canvas Preview",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
                        currentProject.layers.find { it.id == state.selectedLayerId }?.let { selected ->
                            Canvas(Modifier.fillMaxSize()) {
                                val t = selected.transform
                                val sx = size.width / currentProject.canvas.width
                                val sy = size.height / currentProject.canvas.height
                                val origin = Offset(t.x * sx, t.y * sy)
                                val bounds = androidx.compose.ui.geometry.Size(t.width * sx, t.height * sy)
                                rotate(t.rotation, Offset(origin.x + bounds.width / 2, origin.y + bounds.height / 2)) {
                                    drawRect(Color(0xFFE5A93C), origin, bounds, style = Stroke(2.dp.toPx()))
                                }
                            }
                        }

                        fun hitLayer(position: Offset): LayerModel? = currentProject.layers.lastOrNull { layer ->
                            val x = position.x * currentProject.canvas.width / canvasSize.width
                            val y = position.y * currentProject.canvas.height / canvasSize.height
                            val t = layer.transform
                            val angle = Math.toRadians(-t.rotation.toDouble())
                            val dx = x - (t.x + t.width / 2)
                            val dy = y - (t.y + t.height / 2)
                            val localX = dx * kotlin.math.cos(angle) - dy * kotlin.math.sin(angle) + t.width / 2
                            val localY = dx * kotlin.math.sin(angle) + dy * kotlin.math.cos(angle) + t.height / 2
                            layer.visible && !layer.locked && localX >= 0 && localX <= t.width && localY >= 0 && localY <= t.height
                        }
                        Box(Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
                            .pointerInput(canvasSize) {
                                coroutineScope {
                                    launch {
                                        detectTapGestures { position ->
                                            val layer = hitLayer(position)
                                            viewModel.selectLayer(layer?.id)
                                            if (layer is LayerModel.Image) selectedTabIndex = 1
                                        }
                                    }
                                    launch {
                                        var draggedLayer: LayerModel? = null
                                        var initialTransform: TransformModel? = null
                                        var totalDragOffset = Offset.Zero

                                        detectDragGestures(
                                            onDragStart = { position ->
                                                val layer = hitLayer(position)
                                                viewModel.selectLayer(layer?.id)
                                                if (layer != null && !layer.locked && layer.visible) {
                                                    draggedLayer = layer
                                                    initialTransform = layer.transform
                                                    totalDragOffset = Offset.Zero
                                                    if (layer is LayerModel.Image) selectedTabIndex = 1
                                                    activeDraggingLayerId = layer.id
                                                    baseSceneBitmap = sceneRenderer.renderBaseScene(currentProject, layer.id, 720, 720)
                                                    viewModel.startGesture()
                                                } else {
                                                    draggedLayer = null
                                                    initialTransform = null
                                                    activeDraggingLayerId = null
                                                    baseSceneBitmap = null
                                                }
                                            },
                                            onDrag = { change, amount ->
                                                val layer = draggedLayer
                                                val initial = initialTransform
                                                val latest = viewModel.uiState.value.project ?: currentProject
                                                if (layer != null && initial != null && canvasSize.width > 0 && canvasSize.height > 0) {
                                                    change.consume()
                                                    totalDragOffset += amount
                                                    val scaleX = latest.canvas.width.toFloat() / canvasSize.width
                                                    val scaleY = latest.canvas.height.toFloat() / canvasSize.height
                                                    val newX = (initial.x + totalDragOffset.x * scaleX)
                                                        .coerceIn(0f, (latest.canvas.width - initial.width).coerceAtLeast(0f))
                                                    val newY = (initial.y + totalDragOffset.y * scaleY)
                                                        .coerceIn(0f, (latest.canvas.height - initial.height).coerceAtLeast(0f))

                                                    viewModel.updateLayerTransform(
                                                        layer.id,
                                                        initial.copy(x = newX, y = newY),
                                                        isTransient = true
                                                    )
                                                }
                                            },
                                            onDragEnd = {
                                                if (draggedLayer != null) {
                                                    viewModel.endGesture()
                                                    draggedLayer = null
                                                    initialTransform = null
                                                    activeDraggingLayerId = null
                                                    baseSceneBitmap = null
                                                }
                                            },
                                            onDragCancel = {
                                                if (draggedLayer != null) {
                                                    viewModel.endGesture()
                                                    draggedLayer = null
                                                    initialTransform = null
                                                    activeDraggingLayerId = null
                                                    baseSceneBitmap = null
                                                }
                                            }
                                        )
                                    }
                                }
                            })

                        val selectedImage = currentProject.layers.filterIsInstance<LayerModel.Image>()
                            .find { it.id == state.selectedLayerId && it.visible && !it.locked }
                        if (selectedImage != null && canvasSize.width > 1) {
                            val t = selectedImage.transform
                            val sx = canvasSize.width.toFloat() / currentProject.canvas.width
                            val sy = canvasSize.height.toFloat() / currentProject.canvas.height
                            val angle = Math.toRadians(t.rotation.toDouble())
                            val dx = t.width * sx / 2
                            val dy = t.height * sy / 2
                            val cornerX = (t.x + t.width / 2) * sx + dx * kotlin.math.cos(angle).toFloat() - dy * kotlin.math.sin(angle).toFloat()
                            val cornerY = (t.y + t.height / 2) * sy + dx * kotlin.math.sin(angle).toFloat() + dy * kotlin.math.cos(angle).toFloat()
                            val handleSize = with(LocalDensity.current) { 44.dp.toPx() }
                            Box(Modifier.offset {
                                IntOffset(
                                    (cornerX - handleSize / 2).coerceIn(0f, (canvasSize.width - handleSize).coerceAtLeast(0f)).toInt(),
                                    (cornerY - handleSize / 2).coerceIn(0f, (canvasSize.height - handleSize).coerceAtLeast(0f)).toInt())
                            }.size(44.dp).clip(CircleShape).background(NeonAccent)
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(selectedImage.id, canvasSize) {
                                    var initial: TransformModel? = null
                                    var total = Offset.Zero
                                    detectDragGestures(
                                        onDragStart = {
                                            initial = viewModel.uiState.value.project?.layers?.find { it.id == selectedImage.id }?.transform
                                            total = Offset.Zero
                                            selectedTabIndex = 1
                                            viewModel.startGesture()
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            total += amount
                                            initial?.let { original ->
                                                viewModel.updateLayerTransform(
                                                    selectedImage.id,
                                                    com.nipu.wishfest.domain.models.resizeImageFromCorner(original,
                                                        total.x / sx, total.y / sy,
                                                        currentProject.canvas.width, currentProject.canvas.height),
                                                    isTransient = true
                                                )
                                            }
                                        },
                                        onDragEnd = {
                                            viewModel.endGesture()
                                        },
                                        onDragCancel = {
                                            viewModel.endGesture()
                                        }
                                    )
                                }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.OpenInFull, contentDescription = "Resize selected image",
                                    tint = Color.Black, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Control Sheet Panel
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
            ) {
                Column {
                    SelectedLayerActions(selectedLayer, deleteSelection)
                    val tabs = listOf("Message", "Elements", "Background", "Decorate")

                    androidx.compose.material3.PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 8.dp) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        item { TextButton(onClick = { viewModel.addTextLayer("Your message"); selectedTabIndex = 0 }) { Text("+ Add message") } }
                        items(currentProject.layers, key = { it.id }) { layer ->
                            FilterChip(selected = state.selectedLayerId == layer.id,
                                onClick = {
                                    viewModel.selectLayer(layer.id)
                                    selectedTabIndex = if (layer is LayerModel.Text && layer.fontId == "sticker") 3 else if (layer is LayerModel.Text) 0 else 1
                                }, label = { Text(when (layer) {
                                    is LayerModel.Text -> layer.text.replace("\n", " ").take(18)
                                    is LayerModel.Image -> com.nipu.wishfest.domain.models.DevotionalArtworks.nameFor(layer.assetPath)
                                }, maxLines = 1) })
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        when (selectedTabIndex) {
                            0 -> TextControlTab(
                                project = currentProject,
                                selectedLayerId = state.selectedLayerId,
                                onUpdateText = viewModel::updateSelectedTextLayer,
                                onReplaceWithAiWish = viewModel::replaceTextWithAiWish
                            )
                            1 -> LayersControlTab(
                                project = currentProject,
                                selectedLayerId = state.selectedLayerId,
                                onAddImage = viewModel::addImageLayer,
                                onUpdateImage = viewModel::updateLayerTransform,
                                onUpdateImageTint = viewModel::updateImageTint,
                                onUpdateImageOpacity = viewModel::updateImageOpacity,
                                onAddText = viewModel::addTextLayer
                            )
                            3 -> DecorationControlTab(currentProject, state.selectedLayerId, viewModel)
                            2 -> BackgroundControlTab(
                                canvas = currentProject.canvas,
                                onSelectColor = viewModel::updateBackgroundColor,
                                onPickPhoto = { photoPickerLauncher.launch("image/*") },
                                onRotatePhoto90 = viewModel::rotateBackgroundPhoto90,
                                onUpdatePhotoRotation = viewModel::updateBackgroundPhotoRotation,
                                onRemovePhoto = { viewModel.updateBackgroundPhoto("") }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun layerLabel(layer: LayerModel): String = when (layer) {
    is LayerModel.Image -> com.nipu.wishfest.domain.models.DevotionalArtworks.nameFor(layer.assetPath)
    is LayerModel.Text -> if (layer.fontId == "sticker") "Sticker" else "Text"
}

@Composable
internal fun SelectedLayerActions(layer: LayerModel?, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (layer != null) {
                Icon(when (layer) {
                    is LayerModel.Image -> Icons.Default.Image
                    is LayerModel.Text -> if (layer.fontId == "sticker") Icons.Default.EmojiEmotions else Icons.Default.TextFields
                }, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(if (layer == null) "Select an element" else "${layerLabel(layer)} selected",
                    style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(when {
                    layer == null -> "Tap an item on your card or choose a layer below."
                    layer.locked -> "This layer is locked."
                    layer is LayerModel.Text -> layer.text.replace('\n', ' ')
                    else -> "Move, resize or remove this image."
                }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onDelete, enabled = layer != null && !layer.locked,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Delete", maxLines = 1)
            }
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialColor = remember(initialHex) {
        try {
            Color(initialHex.toColorInt())
        } catch (_: Exception) {
            Color.Red
        }
    }

    var red by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255f) }
    var customHexInput by remember { mutableStateOf(initialHex) }

    val currentColor = Color(red / 255f, green / 255f, blue / 255f)
    val currentHex = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 Select Custom Color", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentHex,
                        color = if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // RGB Sliders
                Text("Red: ${red.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = red, onValueChange = { red = it; customHexInput = currentHex }, valueRange = 0f..255f)

                Text("Green: ${green.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = green, onValueChange = { green = it; customHexInput = currentHex }, valueRange = 0f..255f)

                Text("Blue: ${blue.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = blue, onValueChange = { blue = it; customHexInput = currentHex }, valueRange = 0f..255f)

                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = { input ->
                        customHexInput = input
                        if (input.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                            try {
                                val parsed = input.toColorInt()
                                red = android.graphics.Color.red(parsed).toFloat()
                                green = android.graphics.Color.green(parsed).toFloat()
                                blue = android.graphics.Color.blue(parsed).toFloat()
                            } catch (_: Exception) { }
                        }
                    },
                    label = { Text("Hex Code (e.g. #FF007F)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onColorSelected(currentHex)
                onDismiss()
            }) {
                Text("Apply Color", fontWeight = FontWeight.Bold)
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
fun TextControlTab(
    project: ProjectModel,
    selectedLayerId: String?,
    onUpdateText: (String, String?, String?, Float?, String?) -> Unit,
    onReplaceWithAiWish: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val aiWishGenerator = remember { AiWishGenerator() }

    var showAiDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var aiOccasion by remember(project.title) {
        val initial = if (project.title in listOf("Start from scratch", "Untitled Card", "AI Custom Card", "")) "Festive Celebration" else project.title
        mutableStateOf(initial)
    }
    var aiRecipient by remember { mutableStateOf("Friend") }
    var aiTone by remember { mutableStateOf("Warm") }
    var aiLanguage by remember { mutableStateOf("English") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    val quickOccasions = listOf("Lakshmi Puja", "Diwali", "Birthday", "Wedding", "New Year", "Eid", "Good Morning")
    val tones = listOf("Warm", "Devotional / Sacred", "Poetic", "Short & Playful", "Formal")
    val languages = listOf("English", "Hindi (हिंदी)", "Bengali (বাংলা)", "Tamil (தமிழ்)", "German (Deutsch)", "Arabic (العربية)")

    val selectedLayer = (project.layers.find { it.id == selectedLayerId } as? LayerModel.Text)?.takeIf { it.fontId != "sticker" }
    val scrollState = rememberScrollState()

    if (showColorPicker && selectedLayer != null) {
        CustomColorPickerDialog(
            initialHex = selectedLayer.color,
            onDismiss = { showColorPicker = false },
            onColorSelected = { customHex ->
                onUpdateText(selectedLayer.text, customHex, null, null, null)
            }
        )
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("✨ AI Wish Assistant") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select topic, recipient, tone and language to write a customized wish.")

                    OutlinedTextField(
                        value = aiOccasion,
                        onValueChange = { aiOccasion = it },
                        label = { Text("Occasion / Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickOccasions) { occ ->
                            FilterChip(
                                selected = aiOccasion.equals(occ, ignoreCase = true),
                                onClick = { aiOccasion = occ },
                                label = { Text(occ, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiRecipient,
                        onValueChange = { aiRecipient = it },
                        label = { Text("Recipient Name / Relation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Wish Tone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tones) { t ->
                            FilterChip(
                                selected = aiTone == t,
                                onClick = { aiTone = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Wish Language", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(languages) { lang ->
                            FilterChip(
                                selected = aiLanguage == lang,
                                onClick = { aiLanguage = lang },
                                label = { Text(lang, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isGeneratingAi,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black),
                    onClick = {
                        isGeneratingAi = true
                        scope.launch {
                            val finalOccasion = aiOccasion.ifBlank { "Festive Celebration" }
                            val finalRecipient = aiRecipient.ifBlank { "Friend" }
                            val generated = aiWishGenerator.generateWish(
                                festivalName = finalOccasion,
                                recipient = finalRecipient,
                                tone = aiTone,
                                language = aiLanguage
                            )
                            onReplaceWithAiWish(generated)
                            isGeneratingAi = false
                            showAiDialog = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isGeneratingAi) "Generating..." else "Generate Wish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        if (selectedLayer != null) {
            Text("Your message", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = selectedLayer.text,
                onValueChange = { newText -> onUpdateText(newText, null, null, null, null) },
                label = { Text("Write your wish") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAiDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Help me write")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Style Selector Chips
            Text("Font style", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val fontOptions = listOf(
                "default" to "Sans Modern",
                "serif" to "Serif Elegant",
                "bengali" to "Bengali Script",
                "arabic" to "Arabic Calligraphy",
                "cursive" to "Calligraphy",
                "bold" to "Headline Bold",
                "monospace" to "Monospace"
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fontOptions) { (fontId, label) ->
                    FilterChip(
                        selected = selectedLayer.fontId.equals(fontId, ignoreCase = true),
                        onClick = { onUpdateText(selectedLayer.text, null, fontId, null, null) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Alignment", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val alignments = listOf("left" to "⬅️ Left", "center" to "↔️ Center", "right" to "➡️ Right")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alignments) { (align, label) ->
                    FilterChip(
                        selected = selectedLayer.alignment.equals(align, ignoreCase = true),
                        onClick = { onUpdateText(selectedLayer.text, null, null, null, align) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Font Size: ${selectedLayer.fontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = selectedLayer.fontSize,
                onValueChange = { newSize -> onUpdateText(selectedLayer.text, null, null, newSize, null) },
                valueRange = 20f..120f
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Text color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showColorPicker = true }) {
                    Text("🎨 Custom Color")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val colors = listOf("#000000", "#FFFFFF", "#FFD700", "#FF4500", "#1A4331", "#8B0000", "#2B1055")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colors) { hexColor ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hexColor.toColorInt()))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onUpdateText(selectedLayer.text, hexColor, null, null, null) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Choose a message above, or tap + Add message to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LayersControlTab(
    project: ProjectModel,
    selectedLayerId: String?,
    onAddImage: (String) -> Unit,
    onUpdateImage: (String, TransformModel) -> Unit,
    onUpdateImageTint: (String, String) -> Unit,
    onUpdateImageOpacity: (String, Float) -> Unit,
    onAddText: (String) -> Unit
) {
    val tintSwatches = listOf(
        "#E5A93C", "#C41E3A", "#1E88E5", "#0F766E", "#5D4037",
        "#8B1E3D", "#F5B942", "#1565C0", "#1B1B2F", "#FFFFFF", "#000000"
    )
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val selectedImage = project.layers.filterIsInstance<LayerModel.Image>().find { it.id == selectedLayerId }
        if (selectedImage != null) {
            Text("Selected image · ${com.nipu.wishfest.domain.models.DevotionalArtworks.nameFor(selectedImage.assetPath)}",
                fontWeight = FontWeight.Bold)
            Text("Drag to move · gold corner to resize · change color below for illustrations.",
                style = MaterialTheme.typography.bodySmall)
            val ratio = selectedImage.transform.height / selectedImage.transform.width.coerceAtLeast(1f)
            val maximumSize = minOf(project.canvas.width.toFloat(), project.canvas.height / ratio)
            val minimumSize = minOf(minOf(project.canvas.width, project.canvas.height) * .08f, maximumSize)
            Text("Size · ${(selectedImage.transform.width / project.canvas.width * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = selectedImage.transform.width.coerceIn(minimumSize, maximumSize),
                valueRange = minimumSize..maximumSize,
                onValueChange = { size ->
                    onUpdateImage(selectedImage.id, com.nipu.wishfest.domain.models.resizeImage(
                        selectedImage.transform, size, project.canvas.width, project.canvas.height))
                })
            Text("Rotation · ${selectedImage.transform.rotation.toInt()}°", style = MaterialTheme.typography.labelLarge)
            Slider(value = selectedImage.transform.rotation.coerceIn(-180f, 180f), valueRange = -180f..180f,
                onValueChange = { onUpdateImage(selectedImage.id, selectedImage.transform.copy(rotation = it)) })

            Text(
                "Opacity · ${(selectedImage.opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = selectedImage.opacity.coerceIn(0.05f, 1f),
                valueRange = 0.05f..1f,
                onValueChange = { onUpdateImageOpacity(selectedImage.id, it) }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdateImageOpacity(selectedImage.id, 1f) }) {
                    Text("Opaque")
                }
                OutlinedButton(onClick = { onUpdateImageOpacity(selectedImage.id, 0.5f) }) {
                    Text("50%")
                }
                OutlinedButton(onClick = { onUpdateImageOpacity(selectedImage.id, 0.25f) }) {
                    Text("Soft")
                }
            }

            val canTint = com.nipu.wishfest.domain.models.DevotionalArtworks.isTintable(selectedImage.assetPath) ||
                selectedImage.tintColor.isNotBlank()
            if (canTint) {
                Text("Illustration color", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedButton(onClick = { onUpdateImageTint(selectedImage.id, "") }) {
                            Text("Original")
                        }
                    }
                    items(tintSwatches) { hex ->
                        val selected = selectedImage.tintColor.equals(hex, ignoreCase = true)
                        val swatch = try {
                            Color(hex.toColorInt())
                        } catch (_: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Marigold else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateImageTint(selectedImage.id, hex) }
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAddImage(selectedImage.assetPath) }) { Text("Duplicate image") }
            }
        }
        DevotionalImagePicker(onAddImage)
        Text("Quick Greetings", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedButton(onClick = { onAddText("শুভ বিজয়া!") }) {
                    Text("🌸 Subho Bijoya")
                }
            }
            item {
                OutlinedButton(onClick = { onAddText("Eid Mubarak! 🌙") }) {
                    Text("🌙 Eid Mubarak")
                }
            }
            item {
                OutlinedButton(onClick = { onAddText("Happy Diwali! 🪔") }) {
                    Text("🪔 Happy Diwali")
                }
            }
            item {
                OutlinedButton(onClick = { onAddText("Happy New Year 🎉") }) {
                    Text("🎉 New Year")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddText("Custom Text") },
                colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black)
            ) {
                Text("➕ Add Text Layer", fontWeight = FontWeight.Bold)
            }

        }
    }
}

@Composable
private fun DevotionalImagePicker(onAddImage: (String) -> Unit) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("All") }
    val catalog = com.nipu.wishfest.domain.models.DevotionalArtworks
    val filtered = remember(category) {
        if (category == "All") catalog.items
        else catalog.items.filter { it.category == category }
    }

    Text("Devotional illustrations", fontWeight = FontWeight.Bold)
    Text("Tintable faces & symbols can change color and size after you add them.",
        style = MaterialTheme.typography.bodySmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(catalog.categories) { cat ->
            FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat) }
            )
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filtered, key = { it.assetPath }) { artwork ->
            val thumbnail = remember(artwork.assetPath) {
                context.assets.open(artwork.assetPath).use {
                    android.graphics.BitmapFactory.decodeStream(it, null,
                        android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 })
                }
            }
            Surface(
                onClick = { onAddImage(artwork.assetPath) },
                modifier = Modifier.size(width = 112.dp, height = 148.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (artwork.tintable) {
                                    try {
                                        Color(artwork.defaultTint.toColorInt()).copy(alpha = 0.18f)
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.surface
                                    }
                                } else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        thumbnail?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = artwork.name,
                                modifier = Modifier.size(88.dp),
                                colorFilter = if (artwork.tintable) {
                                    androidx.compose.ui.graphics.ColorFilter.tint(
                                        try {
                                            Color(artwork.defaultTint.toColorInt())
                                        } catch (_: Exception) {
                                            Marigold
                                        }
                                    )
                                } else null
                            )
                        }
                    }
                    Text(artwork.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(
                        if (artwork.tintable) "Recolor" else "Full color",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundControlTab(
    canvas: CanvasModel,
    onSelectColor: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRotatePhoto90: () -> Unit,
    onUpdatePhotoRotation: (Float) -> Unit,
    onRemovePhoto: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showColorPicker) {
        CustomColorPickerDialog(
            initialHex = if (canvas.backgroundColor.startsWith("#")) canvas.backgroundColor else "#1A4331",
            onDismiss = { showColorPicker = false },
            onColorSelected = { customHex ->
                onSelectColor(customHex)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canvas.backgroundPhotoPath.isNotEmpty()) {
            Text("📷 Background Photo Controls", fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRotatePhoto90,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔄 Rotate 90°", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("❌ Remove Photo")
                }
            }

            Text("Rotation Angle: ${canvas.backgroundPhotoRotation.toInt()}°", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = canvas.backgroundPhotoRotation,
                onValueChange = onUpdatePhotoRotation,
                valueRange = 0f..360f
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        Text("Festive Gradient Presets", fontWeight = FontWeight.Bold)
        val gradients = listOf(
            "gradient:#5C061C,#E5A93C" to Pair(Color(0xFF5C061C), Color(0xFFE5A93C)),
            "gradient:#064E3B,#10B981" to Pair(Color(0xFF064E3B), Color(0xFF10B981)),
            "gradient:#2B1055,#8B5CF6" to Pair(Color(0xFF2B1055), Color(0xFF8B5CF6)),
            "gradient:#0284C7,#38BDF8" to Pair(Color(0xFF0284C7), Color(0xFF38BDF8)),
            "gradient:#991B1B,#F97316" to Pair(Color(0xFF991B1B), Color(0xFFF97316)),
            "gradient:#4C1D95,#EC4899" to Pair(Color(0xFF4C1D95), Color(0xFFEC4899)),
            "gradient:#0F172A,#D97706" to Pair(Color(0xFF0F172A), Color(0xFFD97706)),
            "gradient:#FFF7ED,#FDE68A" to Pair(Color(0xFFFFF7ED), Color(0xFFFDE68A))
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gradients) { (gradKey, colors) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.first, colors.second)))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onSelectColor(gradKey) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Solid Color Presets", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showColorPicker = true }) {
                Text("🎨 Custom Color")
            }
        }
        val bgColors = listOf("#8B0000", "#1A4331", "#2B1055", "#004080", "#4A0E17", "#FFF8DC", "#FFFFFF", "#000000")

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bgColors) { hexColor ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(hexColor.toColorInt()))
                        .border(1.5.dp, Color.LightGray, CircleShape)
                        .clickable { onSelectColor(hexColor) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📷 Choose Background Photo")
        }
    }
}

@Composable
private fun DecorationControlTab(project: ProjectModel, selectedId: String?, viewModel: EditorViewModel) {
    var category by remember { mutableStateOf("Celebrate") }
    var customEmoji by remember { mutableStateOf("") }
    val packs = linkedMapOf(
        "Celebrate" to listOf("🎉", "🎂", "🎁", "🎈", "🥳", "✨", "🎊", "🪄"),
        "Love" to listOf("💖", "💕", "💌", "🧸", "🌹", "🦋", "🥰", "💐"),
        "Festive" to listOf("🪔", "🌙", "⭐", "🌸", "🕯️", "🎄", "🌺", "🙏")
    )
    val sticker = project.layers.filterIsInstance<LayerModel.Text>().find { it.id == selectedId && it.fontId == "sticker" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("A little joy, a lot of personality", fontWeight = FontWeight.Bold)
        Text("Tap a sticker to add it. Tap or drag it on your card to edit.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packs.keys.toList()) { name ->
                FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packs.getValue(category)) { emoji ->
                Surface(onClick = { viewModel.addSticker(emoji) }, shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(emoji, fontSize = 30.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = customEmoji, onValueChange = { customEmoji = it },
                label = { Text("Your emoji or sticker text") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.addSticker(customEmoji.trim()); customEmoji = "" }, enabled = customEmoji.isNotBlank()) { Text("Add") }
        }
        if (sticker != null) {
            Text("Selected sticker · ${sticker.text}", fontWeight = FontWeight.Bold)
            Text("Size · ${sticker.fontSize.toInt()}")
            Slider(value = sticker.fontSize.coerceIn(48f, 240f), valueRange = 48f..240f, onValueChange = {
                viewModel.updateSelectedTextLayer(sticker.text, fontSize = it)
                viewModel.updateLayerTransform(sticker.id, sticker.transform.copy(width = it * 1.5f, height = it * 1.7f))
            })
            Text("Rotation · ${sticker.transform.rotation.toInt()}°")
            Slider(value = sticker.transform.rotation.coerceIn(-180f, 180f), valueRange = -180f..180f,
                onValueChange = { viewModel.updateLayerTransform(sticker.id, sticker.transform.copy(rotation = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.addSticker(sticker.text) }) { Text("Duplicate") }
            }
        }
        Text("Ready-made looks", fontWeight = FontWeight.Bold)
        Text("Applies a background, frame and matching typography. Undo to restore your previous look.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Blossom", "Midnight", "Golden")) { name ->
                OutlinedButton(onClick = { viewModel.applyCreativeStyle(name) }) { Text(name) }
            }
        }
        Text("Frame your wish", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("none" to "None", "simple" to "Classic", "double" to "Double gold", "dashed" to "Stitched", "floral" to "Blossom",
                "scallop" to "Scalloped lace", "postage" to "Postage stamp",
                "deco" to "Art deco", "confetti" to "Confetti",
                "ribbon" to "Ribbon corners", "hearts" to "Love notes")) { (style, label) ->
                FilterChip(selected = project.canvas.borderStyle == style,
                    onClick = { viewModel.updateBorder(style, project.canvas.borderColor) }, label = { Text(label) })
            }
        }
        Text("Lakshmi Puja frames", fontWeight = FontWeight.Bold)
        Text("Lotus, alpana and glowing diyas for a wish of peace and prosperity.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.nipu.wishfest.rendering.HinduFestivalFrames.lakshmiOptions.toList()) { (style, label) ->
                FilterChip(selected = project.canvas.borderStyle == style,
                    onClick = { viewModel.updateBorder(style, project.canvas.borderColor) }, label = { Text(label) })
            }
        }
        Text("Wedding invitation frames", fontWeight = FontWeight.Bold)
        Text("Frame your invitation, then edit the names, date, venue and RSVP in Message.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.nipu.wishfest.rendering.WeddingFrames.options.toList()) { (style, label) ->
                FilterChip(selected = project.canvas.borderStyle == style,
                    onClick = { viewModel.updateBorder(style, project.canvas.borderColor) }, label = { Text(label) })
            }
        }
        Text("Hindu festival frames", fontWeight = FontWeight.Bold)
        Text("Light up Puja, Diwali and other festive wishes.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.nipu.wishfest.rendering.HinduFestivalFrames.options.filterKeys { it !in com.nipu.wishfest.rendering.HinduFestivalFrames.lakshmiOptions }.toList()) { (style, label) ->
                FilterChip(selected = project.canvas.borderStyle == style,
                    onClick = { viewModel.updateBorder(style, project.canvas.borderColor) }, label = { Text(label) })
            }
        }
        Text("Frame color", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("#E5A93C", "#FF9933", "#B91C1C", "#F472B6", "#A78BFA", "#34D399", "#FFFFFF", "#1B4332")) { hex ->
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(hex.toColorInt()))
                    .border(if (project.canvas.borderColor == hex) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .clickable { viewModel.updateBorder(project.canvas.borderStyle, hex) })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
