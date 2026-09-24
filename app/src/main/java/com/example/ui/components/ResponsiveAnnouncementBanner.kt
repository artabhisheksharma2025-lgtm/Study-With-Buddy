package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnnouncementEntity

@Composable
fun ResponsiveAnnouncementBanner(
    banner: AnnouncementEntity,
    onDismiss: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDismissed by remember(banner.announcementId) { mutableStateOf(false) }

    if (isDismissed) return

    val handleAction: () -> Unit = {
        if (onActionClick != null) {
            onActionClick()
        } else if (!banner.actionUrl.isNullOrBlank()) {
            try {
                var url = banner.actionUrl.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("content://") && !url.startsWith("file://")) {
                    url = "https://$url"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)

                val isDownload = url.endsWith(".apk", ignoreCase = true) ||
                        banner.actionLabel?.contains("download", ignoreCase = true) == true ||
                        banner.actionLabel?.contains("apk", ignoreCase = true) == true ||
                        banner.actionLabel?.contains("file", ignoreCase = true) == true

                if (isDownload) {
                    Toast.makeText(context, "📥 Starting download: ${banner.actionLabel ?: "File/APK"}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Opening: ${banner.actionLabel ?: banner.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else if (!banner.actionLabel.isNullOrBlank()) {
            Toast.makeText(context, banner.actionLabel, Toast.LENGTH_SHORT).show()
        }
    }

    val (bgColor, accentColor, iconVector) = when (banner.priority) {
        "URGENT" -> Triple(
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            Icons.Filled.Warning
        )
        "HIGH" -> Triple(
            Color(0xFFFFFBEB),
            Color(0xFFD97706),
            Icons.Filled.NotificationsActive
        )
        "LOW" -> Triple(
            Color(0xFFF0FDF4),
            Color(0xFF16A34A),
            Icons.Filled.CheckCircleOutline
        )
        else -> Triple( // NORMAL
            Color(0xFFEFF6FF),
            Color(0xFF2563EB),
            Icons.Filled.Campaign
        )
    }

    val hasPhoto = !banner.imageUrl.isNullOrBlank()

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (hasPhoto) {
            // PHOTO BANNER WITH OVERLAID ACTION BUTTON AND CLICKABLE FULL CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { handleAction() }
                    .testTag("announcement_banner_photo_${banner.announcementId}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Photo Background
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrim gradient for contrast and readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )

                    // Top row: Priority Badge + Optional Dismiss Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = banner.priority,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (banner.isDismissible) {
                            IconButton(
                                onClick = {
                                    isDismissed = true
                                    onDismiss?.invoke()
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Bottom content overlaid DIRECTLY ON PHOTO
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = banner.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )

                        // Prominent Action Button directly on the photo
                        if (!banner.actionLabel.isNullOrBlank() || !banner.actionUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isDownload = banner.actionUrl?.endsWith(".apk", ignoreCase = true) == true ||
                                    banner.actionLabel?.contains("download", ignoreCase = true) == true ||
                                    banner.actionLabel?.contains("apk", ignoreCase = true) == true

                            Button(
                                onClick = { handleAction() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDownload) Color(0xFF10B981) else accentColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDownload) Icons.Filled.Download else Icons.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = banner.actionLabel ?: if (isDownload) "Download APK" else "Open Link",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // STANDARD TEXT CARD BANNER (CLICKABLE CARD + ACTION BUTTON)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = modifier
                    .fillMaxWidth()
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { handleAction() }
                    .testTag("announcement_banner_${banner.announcementId}")
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val isWide = maxWidth > 550.dp

                    if (isWide) {
                        // Wide screen horizontal layout (tablets/desktops)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = banner.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    PriorityBadge(priority = banner.priority, color = accentColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = banner.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF334155),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!banner.actionLabel.isNullOrBlank() || !banner.actionUrl.isNullOrBlank()) {
                                    val isDownload = banner.actionUrl?.endsWith(".apk", ignoreCase = true) == true ||
                                            banner.actionLabel?.contains("download", ignoreCase = true) == true

                                    Button(
                                        onClick = { handleAction() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDownload) Color(0xFF10B981) else accentColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDownload) Icons.Filled.Download else Icons.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = banner.actionLabel ?: if (isDownload) "Download" else "Open",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (banner.isDismissible) {
                                    IconButton(
                                        onClick = {
                                            isDismissed = true
                                            onDismiss?.invoke()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss announcement",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Mobile compact vertical layout
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = banner.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        PriorityBadge(priority = banner.priority, color = accentColor)
                                    }
                                }

                                if (banner.isDismissible) {
                                    IconButton(
                                        onClick = {
                                            isDismissed = true
                                            onDismiss?.invoke()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = banner.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp
                            )

                            if (!banner.actionLabel.isNullOrBlank() || !banner.actionUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val isDownload = banner.actionUrl?.endsWith(".apk", ignoreCase = true) == true ||
                                        banner.actionLabel?.contains("download", ignoreCase = true) == true

                                Button(
                                    onClick = { handleAction() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDownload) Color(0xFF10B981) else accentColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDownload) Icons.Filled.Download else Icons.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = banner.actionLabel ?: if (isDownload) "Download" else "Open",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = priority,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
