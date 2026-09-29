package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.FocusModeHelper
import com.example.data.util.InstalledAppItem
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowAppsScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    val savedAllowedApps by mainViewModel.allowedApps.collectAsState()
    var currentAllowedSet by remember(savedAllowedApps) {
        mutableStateOf(savedAllowedApps.toMutableSet())
    }

    var allApps by remember {
        mutableStateOf(FocusModeHelper.loadInstalledApps(context, currentAllowedSet))
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ALLOWED", "RESTRICTED"

    // Synchronize apps when currentAllowedSet changes
    val displayedApps = remember(allApps, currentAllowedSet, searchQuery, selectedFilter) {
        allApps.map { it.copy(isAllowed = currentAllowedSet.contains(it.packageName)) }
            .filter { item ->
                val matchesQuery = searchQuery.isBlank() ||
                        item.appName.contains(searchQuery, ignoreCase = true) ||
                        item.packageName.contains(searchQuery, ignoreCase = true) ||
                        item.category.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "ALLOWED" -> item.isAllowed
                    "RESTRICTED" -> !item.isAllowed
                    else -> true
                }
                matchesQuery && matchesFilter
            }
            .sortedWith(
                compareByDescending<InstalledAppItem> { it.isAllowed }
                    .thenBy { it.appName.lowercase() }
            )
    }

    val allowedCount = currentAllowedSet.size
    val totalCount = allApps.size
    val restrictedCount = (totalCount - allowedCount).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📱 Allow Apps",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Focus Mode App Restrictions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("allow_apps_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            mainViewModel.updateAllowedApps(currentAllowedSet)
                            Toast.makeText(context, "Allowed apps saved to profile!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("save_allowed_apps_top_button")
                    ) {
                        Text(
                            text = "SAVE",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$allowedCount Allowed • $restrictedCount Restricted",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Saved to user profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            mainViewModel.updateAllowedApps(currentAllowedSet)
                            Toast.makeText(context, "Allowed apps saved to profile!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_allowed_apps_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply")
                    }
                }
            }
        },
        modifier = modifier.testTag("screen_allow_apps")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Explanatory Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Focus Mode App Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "When Start Study is pressed, Focus Mode locks distractions. Only apps with Allow switched ON can be opened.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Preset Selection Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val studyPresets = FocusModeHelper.DEFAULT_ALLOWED_PACKAGES
                                currentAllowedSet = currentAllowedSet.toMutableSet().apply {
                                    addAll(studyPresets)
                                }
                                Toast.makeText(context, "Study tools allowed!", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Study Essentials") },
                            leadingIcon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(10.dp)
                        )

                        AssistChip(
                            onClick = {
                                currentAllowedSet = allApps.map { it.packageName }.toMutableSet()
                            },
                            label = { Text("Allow All") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(10.dp)
                        )

                        AssistChip(
                            onClick = {
                                currentAllowedSet = mutableSetOf()
                            },
                            label = { Text("Restrict All") },
                            leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allow_apps_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All ($totalCount)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "ALLOWED",
                        onClick = { selectedFilter = "ALLOWED" },
                        label = { Text("Allowed ($allowedCount)") },
                        leadingIcon = {
                            if (selectedFilter == "ALLOWED") {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "RESTRICTED",
                        onClick = { selectedFilter = "RESTRICTED" },
                        label = { Text("Restricted ($restrictedCount)") },
                        leadingIcon = {
                            if (selectedFilter == "RESTRICTED") {
                                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Apps List
            items(displayedApps, key = { it.packageName }) { appItem ->
                AppAllowToggleCard(
                    appItem = appItem,
                    isAllowed = currentAllowedSet.contains(appItem.packageName),
                    onToggle = { isAllowed ->
                        val updated = currentAllowedSet.toMutableSet()
                        if (isAllowed) {
                            updated.add(appItem.packageName)
                        } else {
                            updated.remove(appItem.packageName)
                        }
                        currentAllowedSet = updated
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AppAllowToggleCard(
    appItem: InstalledAppItem,
    isAllowed: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllowed) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAllowed) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${appItem.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Avatar Icon
            val avatarBg = when {
                isAllowed -> IndigoPrimary.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
            val iconTint = when {
                isAllowed -> IndigoPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                val iconVector: ImageVector = when {
                    appItem.appName.contains("Calculator", ignoreCase = true) -> Icons.Filled.Calculate
                    appItem.appName.contains("Note", ignoreCase = true) -> Icons.Filled.EditNote
                    appItem.appName.contains("Book", ignoreCase = true) -> Icons.Filled.MenuBook
                    appItem.appName.contains("Calendar", ignoreCase = true) -> Icons.Filled.CalendarMonth
                    appItem.appName.contains("Clock", ignoreCase = true) || appItem.appName.contains("Timer", ignoreCase = true) -> Icons.Filled.Timer
                    appItem.appName.contains("Chrome", ignoreCase = true) || appItem.appName.contains("Browser", ignoreCase = true) -> Icons.Filled.Public
                    appItem.appName.contains("YouTube", ignoreCase = true) -> Icons.Filled.PlayCircle
                    appItem.appName.contains("Music", ignoreCase = true) || appItem.appName.contains("Spotify", ignoreCase = true) -> Icons.Filled.MusicNote
                    appItem.appName.contains("Chat", ignoreCase = true) || appItem.appName.contains("WhatsApp", ignoreCase = true) -> Icons.Filled.Chat
                    else -> Icons.Filled.Apps
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Name and Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appItem.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAllowed) EmeraldAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (isAllowed) "Allowed in Focus" else "Restricted",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isAllowed) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${appItem.category}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Allow / Not Allowed Toggle Switch
            Switch(
                checked = isAllowed,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("toggle_${appItem.packageName}")
            )
        }
    }
}
