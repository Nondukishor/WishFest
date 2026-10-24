package com.nipu.wishfest.ui.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nipu.wishfest.ui.components.WishScreenBackground
import com.nipu.wishfest.ui.theme.Marigold
import com.nipu.wishfest.ui.theme.SurfaceRaised

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val privacyPolicy = remember(context) { context.assets.open("privacy_policy.txt").bufferedReader().use { it.readText() } }
    if (showPrivacyPolicy) {
        AlertDialog(onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Privacy policy") },
            text = { Text(privacyPolicy, modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showPrivacyPolicy = false }) { Text("Done") } })
    }
    val logoBitmap = remember(context) {
        try {
            context.assets.open("logo.png").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    WishScreenBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
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
                        "About",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceRaised,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap,
                                contentDescription = "WishFest Logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column {
                            Text(
                                "WishFest",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Version 1.0 · Offline greeting studio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsBlock(
                    title = "Privacy & storage",
                    lines = listOf(
                        "Card editing works offline. AI is optional on supported devices.",
                        "Google ML Kit may collect diagnostic data; see our privacy policy.",
                        "Cards and assets stay in app-private local storage.",
                        "The system file chooser gives access only to files you select."
                    )
                )

                TextButton(onClick = { showPrivacyPolicy = true }) { Text("Read privacy policy") }
                Text("Support: pro.nipu@gmail.com", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(14.dp))

                SettingsBlock(
                    title = "Built with",
                    lines = listOf(
                        "Built with Android Jetpack Compose and the Android NDK.",
                        "On-device AI uses Google ML Kit and AICore."
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Made for moments worth celebrating",
                    style = MaterialTheme.typography.bodySmall,
                    color = Marigold.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsBlock(title: String, lines: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceRaised,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            lines.forEachIndexed { index, line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (index < lines.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
