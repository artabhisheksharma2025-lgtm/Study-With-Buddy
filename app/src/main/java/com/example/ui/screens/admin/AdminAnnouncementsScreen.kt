package com.example.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdminEntity
import com.example.data.model.AnnouncementEntity
import com.example.data.util.SecurityUtils
import com.example.ui.components.ResponsiveAnnouncementBanner
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val announcements by adminViewModel.announcements.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (SecurityUtils.canManageAnnouncements(admin.role)) {
                FloatingActionButton(
                    onClick = {
                        editingAnnouncement = null
                        showDialog = true
                    },
                    modifier = Modifier.testTag("admin_add_announcement_fab")
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = "Create Announcement")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Announcements & Banners (${announcements.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (announcements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No announcements configured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(announcements, key = { it.announcementId }) { item ->
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = when (item.status) {
                                                "PUBLISHED" -> Color(0xFFDCFCE7)
                                                "DRAFT" -> Color(0xFFF1F5F9)
                                                else -> Color(0xFFFEF3C7)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.status,
                                                color = when (item.status) {
                                                    "PUBLISHED" -> Color(0xFF15803D)
                                                    "DRAFT" -> Color(0xFF475569)
                                                    else -> Color(0xFFB45309)
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Location: ${item.displayLocation}",
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (SecurityUtils.canManageAnnouncements(admin.role)) {
                                        Row {
                                            IconButton(onClick = {
                                                editingAnnouncement = item
                                                showDialog = true
                                            }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { adminViewModel.deleteAnnouncement(item) }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Live banner render preview
                                ResponsiveAnnouncementBanner(banner = item)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Audience: ${item.targetAudience} • Valid: ${sdf.format(Date(item.startDate))} - ${sdf.format(Date(item.endDate))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AnnouncementEditDialog(
            announcement = editingAnnouncement,
            onDismiss = { showDialog = false },
            onSave = { ann ->
                adminViewModel.saveAnnouncement(ann)
                showDialog = false
            }
        )
    }
}

@Composable
fun AnnouncementEditDialog(
    announcement: AnnouncementEntity?,
    onDismiss: () -> Unit,
    onSave: (AnnouncementEntity) -> Unit
) {
    var title by remember { mutableStateOf(announcement?.title ?: "") }
    var message by remember { mutableStateOf(announcement?.message ?: "") }
    var imageUrl by remember { mutableStateOf(announcement?.imageUrl ?: "") }
    var priority by remember { mutableStateOf(announcement?.priority ?: "NORMAL") }
    var status by remember { mutableStateOf(announcement?.status ?: "PUBLISHED") }
    var targetAudience by remember { mutableStateOf(announcement?.targetAudience ?: "EVERYONE") }
    var displayLocation by remember { mutableStateOf(announcement?.displayLocation ?: "BOTH") }
    var actionLabel by remember { mutableStateOf(announcement?.actionLabel ?: "") }
    var actionUrl by remember { mutableStateOf(announcement?.actionUrl ?: "") }
    var isDismissible by remember { mutableStateOf(announcement?.isDismissible ?: true) }

    val priorities = listOf("LOW", "NORMAL", "HIGH", "URGENT")
    val locations = listOf("BOTH", "USER_APP", "ADMIN_DASHBOARD")
    val audiences = listOf("EVERYONE", "NEW_USERS", "ACTIVE_USERS")

    // Android Photo Picker Launcher for uploading banner photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (announcement == null) "Create Banner / Announcement" else "Edit Banner", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Banner Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Banner Message *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // ---------------- PHOTO UPLOAD SECTION ----------------
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Banner Photo / Image (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        if (imageUrl.isNotBlank()) {
                            // Thumbnail Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Banner Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(6.dp).align(Alignment.TopStart)
                                ) {
                                    Text("Preview", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Photo", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = { imageUrl = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Photo", fontSize = 11.sp)
                                }
                            }
                        } else {
                            // Upload Button & Presets
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Photo from Gallery / Device", fontSize = 12.sp)
                            }

                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Or Paste Image Web URL") },
                                placeholder = { Text("https://example.com/banner.jpg") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )

                            // Preset Templates
                            Text("Or Pick a Preset Banner Graphic:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { imageUrl = "android.resource://com.example/drawable/img_study_hero_1789969284065" },
                                    label = { Text("📚 Study Art", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80" },
                                    label = { Text("🚀 App Launch", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // ---------------- ACTION BUTTON & DOWNLOAD LINK ----------------
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Action Button & Download Link", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedTextField(
                            value = actionLabel,
                            onValueChange = { actionLabel = it },
                            label = { Text("Action Button Label") },
                            placeholder = { Text("e.g. 📥 Download APK, 📄 Download Notes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Action Label Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SuggestionChip(
                                onClick = { actionLabel = "📥 Download APK" },
                                label = { Text("📥 APK", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { actionLabel = "📄 Download File" },
                                label = { Text("📄 File", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { actionLabel = "🚀 Update Now" },
                                label = { Text("🚀 Update", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { actionLabel = "🔗 Open Link" },
                                label = { Text("🔗 Link", fontSize = 11.sp) }
                            )
                        }

                        OutlinedTextField(
                            value = actionUrl,
                            onValueChange = { actionUrl = it },
                            label = { Text("Download Link / File / Action URL") },
                            placeholder = { Text("https://example.com/studytracker.apk") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Text(
                            text = "💡 Clicking the banner photo or action button directly opens this download link / file.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // ---------------- TARGETING & DISPLAY SETTINGS ----------------
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        var expandedPriority by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expandedPriority = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(priority, fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = expandedPriority, onDismissRequest = { expandedPriority = false }) {
                            priorities.forEach { p ->
                                DropdownMenuItem(text = { Text(p) }, onClick = {
                                    priority = p
                                    expandedPriority = false
                                })
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        var expandedLoc by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expandedLoc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(displayLocation, fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = expandedLoc, onDismissRequest = { expandedLoc = false }) {
                            locations.forEach { l ->
                                DropdownMenuItem(text = { Text(l) }, onClick = {
                                    displayLocation = l
                                    expandedLoc = false
                                })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Allow User to Dismiss:", fontSize = 12.sp)
                    Switch(checked = isDismissible, onCheckedChange = { isDismissible = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = announcement?.announcementId ?: "banner_${System.currentTimeMillis()}"
                    onSave(
                        AnnouncementEntity(
                            announcementId = id,
                            title = title.trim(),
                            message = message.trim(),
                            imageUrl = if (imageUrl.isBlank()) null else imageUrl.trim(),
                            priority = priority,
                            status = status,
                            targetAudience = targetAudience,
                            displayLocation = displayLocation,
                            isDismissible = isDismissible,
                            actionLabel = if (actionLabel.isBlank()) null else actionLabel.trim(),
                            actionUrl = if (actionUrl.isBlank()) null else actionUrl.trim(),
                            startDate = announcement?.startDate ?: (System.currentTimeMillis() - 60000L),
                            endDate = announcement?.endDate ?: (System.currentTimeMillis() + 86400000L * 30),
                            createdBy = "Admin"
                        )
                    )
                },
                enabled = title.isNotBlank() && message.isNotBlank()
            ) {
                Text("Save Announcement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
