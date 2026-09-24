package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.model.FriendRequestEntity
import com.example.data.model.FriendshipEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSocialScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Friendships, 1: Friend Requests
    val friendships by adminViewModel.friendships.collectAsState()
    val friendRequests by adminViewModel.friendRequests.collectAsState()
    val users by adminViewModel.users.collectAsState()
    val userMap = remember(users) { users.associateBy { it.userId } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Friendships (${friendships.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Friend Requests (${friendRequests.size})") }
            )
        }

        if (selectedTab == 0) {
            // Friendships list
            if (friendships.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No established friendships found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(friendships, key = { it.friendshipId }) { f ->
                        val u1 = userMap[f.userId1]?.fullName ?: f.userId1
                        val u2 = userMap[f.userId2]?.fullName ?: f.userId2
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Diversity3, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("$u1 ⟷ $u2", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("Connected since ${sdf.format(Date(f.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (SecurityUtils.canManageUsers(admin.role)) {
                                    IconButton(onClick = { /* admin dissolve */ }) {
                                        Icon(Icons.Filled.LinkOff, contentDescription = "Dissolve friendship", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Friend Requests
            if (friendRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friend requests recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(friendRequests, key = { it.requestId }) { req ->
                        val sender = userMap[req.senderId]?.fullName ?: req.senderId
                        val receiver = userMap[req.receiverId]?.fullName ?: req.receiverId
                        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("$sender ➔ $receiver", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("Sent: ${sdf.format(Date(req.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    color = if (req.status == "PENDING") Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = req.status,
                                        color = if (req.status == "PENDING") Color(0xFFB45309) else Color(0xFF15803D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
