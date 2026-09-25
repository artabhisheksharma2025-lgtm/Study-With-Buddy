package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGroupEntity
import com.example.data.model.UserEntity
import com.example.data.remote.OnlineUser
import com.example.data.repository.AppRepository
import com.example.data.util.UserStudyStats
import com.example.ui.components.LiveCameraQRScannerDialog
import com.example.ui.components.QRScannerDialog
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import com.example.ui.viewmodel.FriendsViewModel
import com.example.ui.viewmodel.MainViewModel

enum class FriendsSubTab(val title: String) {
    MY_FRIENDS("My Friends"),
    GROUPS("Study Groups"),
    ADD_FRIEND("Add Friend"),
    REQUESTS("Requests"),
    ACTIVITY_FEED("Activity Feed"),
    COMPARE("Compare")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeDirectChatFriend by friendsViewModel.activeDirectChatFriend.collectAsState()
    val activeStudyGroup by friendsViewModel.activeStudyGroup.collectAsState()
    val studyGroups by friendsViewModel.studyGroups.collectAsState()

    // Real-time Chat Conversation view (Direct friend chat or study group chat)
    if (activeDirectChatFriend != null || activeStudyGroup != null) {
        BackHandler {
            friendsViewModel.closeChat()
        }
        ChatConversationScreen(
            friendsViewModel = friendsViewModel,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val currentUser by friendsViewModel.currentUser.collectAsState()
    val friends by friendsViewModel.friendsList.collectAsState()
    val pendingRequests by friendsViewModel.pendingReceivedRequests.collectAsState()
    val friendStatsMap by friendsViewModel.friendStatsMap.collectAsState()
    val activityFeed by friendsViewModel.activityFeed.collectAsState()

    val searchQuery by friendsViewModel.searchQuery.collectAsState()
    val searchResultUser by friendsViewModel.searchResultUser.collectAsState()
    val searchMessage by friendsViewModel.searchMessage.collectAsState()
    val isSearching by friendsViewModel.isSearching.collectAsState()
    val isOnlineSyncing by friendsViewModel.isOnlineSyncing.collectAsState()
    val onlineUsers by friendsViewModel.onlineCommunityUsers.collectAsState()
    val uiToast by friendsViewModel.uiToast.collectAsState()

    val selectedFriend by friendsViewModel.selectedFriend.collectAsState()
    val compareFriend by friendsViewModel.compareFriend.collectAsState()

    val myStats by mainViewModel.userStats.collectAsState()

    var activeSubTab by remember { mutableStateOf(FriendsSubTab.MY_FRIENDS) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiToast) {
        uiToast?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            friendsViewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 Friends & Study Community", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showQRScanner = true },
                        modifier = Modifier.testTag("button_topbar_scan_qr")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan Friend QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { friendsViewModel.syncOnline() },
                        modifier = Modifier.testTag("button_sync_online")
                    ) {
                        if (isOnlineSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Sync Online Cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.testTag("screen_friends")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sub-tabs scrollable row
            ScrollableTabRow(
                selectedTabIndex = activeSubTab.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                FriendsSubTab.entries.forEach { tab ->
                    Tab(
                        selected = activeSubTab == tab,
                        onClick = { activeSubTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (activeSubTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                                if (tab == FriendsSubTab.REQUESTS && pendingRequests.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge { Text("${pendingRequests.size}") }
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when (activeSubTab) {
                    FriendsSubTab.MY_FRIENDS -> {
                        MyFriendsListContent(
                            friends = friends,
                            friendStatsMap = friendStatsMap,
                            onViewActivity = { friend -> friendsViewModel.selectFriendForDetail(friend) },
                            onChatWithFriend = { friend -> friendsViewModel.openDirectChat(friend) },
                            onRemoveFriend = { friend -> friendsViewModel.removeFriend(friend.userId) },
                            onSwitchToAddFriend = { activeSubTab = FriendsSubTab.ADD_FRIEND }
                        )
                    }
                    FriendsSubTab.GROUPS -> {
                        StudyGroupsContent(
                            studyGroups = studyGroups,
                            currentUser = currentUser,
                            onCreateGroupClick = { showCreateGroupDialog = true },
                            onOpenGroupChat = { group -> friendsViewModel.openGroupChat(group) },
                            onDeleteGroup = { groupId -> friendsViewModel.deleteStudyGroup(groupId) }
                        )
                    }
                    FriendsSubTab.ADD_FRIEND -> {
                        AddFriendContent(
                            currentUser = currentUser,
                            searchQuery = searchQuery,
                            searchResultUser = searchResultUser,
                            searchMessage = searchMessage,
                            isSearching = isSearching,
                            isOnlineSyncing = isOnlineSyncing,
                            onlineUsers = onlineUsers,
                            onQueryChanged = { friendsViewModel.onSearchQueryChanged(it) },
                            onSearch = { friendsViewModel.performSearchByStudyId() },
                            onOpenQRScanner = { showQRScanner = true },
                            onSendRequest = { studyId -> friendsViewModel.sendFriendRequest(studyId) },
                            onSyncOnline = { friendsViewModel.syncOnline() },
                            onSelectOnlineUser = { studyId ->
                                friendsViewModel.onSearchQueryChanged(studyId)
                                friendsViewModel.performSearchByStudyId()
                            }
                        )
                    }
                    FriendsSubTab.REQUESTS -> {
                        PendingRequestsContent(
                            requests = pendingRequests,
                            friendsViewModel = friendsViewModel
                        )
                    }
                    FriendsSubTab.ACTIVITY_FEED -> {
                        ActivityFeedContent(activityFeed = activityFeed)
                    }
                    FriendsSubTab.COMPARE -> {
                        CompareStudyContent(
                            currentUser = currentUser,
                            myStats = myStats,
                            friends = friends,
                            selectedFriend = compareFriend,
                            friendStatsMap = friendStatsMap,
                            onSelectFriendForCompare = { friendsViewModel.selectFriendForCompare(it) }
                        )
                    }
                }
            }
        }

        // Create Study Group Dialog
        if (showCreateGroupDialog) {
            CreateStudyGroupDialog(
                friends = friends,
                onDismiss = { showCreateGroupDialog = false },
                onCreateGroup = { name, desc, selectedFriends, color ->
                    friendsViewModel.createStudyGroup(name, desc, selectedFriends, color)
                    showCreateGroupDialog = false
                }
            )
        }

        // Friend Detail Activity Modal
        selectedFriend?.let { friend ->
            val friendStats = friendStatsMap[friend.userId] ?: UserStudyStats()

            AlertDialog(
                onDismissRequest = { friendsViewModel.selectFriendForDetail(null) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.fullName.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(friend.fullName, fontWeight = FontWeight.Bold)
                            Text("@${friend.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (friend.privacyVisibility == "PRIVATE") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔒 This student's study activity is set to private.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text("📊 Study Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Today's Study:")
                                Text(AppRepository.formatDurationShort(friendStats.todayTimeSeconds), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("This Week:")
                                Text(AppRepository.formatDurationShort(friendStats.weeklyTimeSeconds), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Sessions:")
                                Text("${friendStats.totalSessionsCount}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Streak:")
                                Text("${friendStats.currentStreakDays} Days 🔥", fontWeight = FontWeight.Bold, color = FlameOrange)
                            }

                            if (friendStats.subjectBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Top Subjects:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                friendStats.subjectBreakdown.take(3).forEach { sub ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${sub.subjectName}")
                                        Text(AppRepository.formatDurationShort(sub.totalTimeSeconds))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { friendsViewModel.selectFriendForDetail(null) }) {
                        Text("Close")
                    }
                }
            )
        }

        // Live Camera QR Scanner Dialog Modal
        if (showQRScanner) {
            LiveCameraQRScannerDialog(
                onCodeScanned = { code ->
                    showQRScanner = false
                    friendsViewModel.onSearchQueryChanged(code)
                    friendsViewModel.performSearchByStudyId()
                    activeSubTab = FriendsSubTab.ADD_FRIEND
                },
                onDismiss = { showQRScanner = false }
            )
        }
    }
}

@Composable
private fun MyFriendsListContent(
    friends: List<UserEntity>,
    friendStatsMap: Map<String, UserStudyStats>,
    onViewActivity: (UserEntity) -> Unit,
    onChatWithFriend: (UserEntity) -> Unit,
    onRemoveFriend: (UserEntity) -> Unit,
    onSwitchToAddFriend: () -> Unit
) {
    if (friends.isEmpty()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Add your first study friend.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connect using unique Study IDs to share goals & progress!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSwitchToAddFriend,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_add_friend_button")
                ) {
                    Text("Add Friend")
                }
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(friends) { friend ->
                val fStats = friendStatsMap[friend.userId] ?: UserStudyStats()

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_card_${friend.userId}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.fullName.take(1),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${friend.username} • ID: ${friend.studyId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = FlameOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "🔥 ${fStats.currentStreakDays} Days",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FlameOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AppRepository.formatDurationShort(fStats.todayTimeSeconds), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("This Week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AppRepository.formatDurationShort(fStats.weeklyTimeSeconds), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sessions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${fStats.totalSessionsCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Top Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fStats.mostStudiedSubject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onRemoveFriend(friend) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Remove")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onChatWithFriend(friend) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.testTag("chat_friend_${friend.userId}")
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { onViewActivity(friend) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Activity")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyGroupsContent(
    studyGroups: List<StudyGroupEntity>,
    currentUser: UserEntity?,
    onCreateGroupClick: () -> Unit,
    onOpenGroupChat: (StudyGroupEntity) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner to create a group
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Group Study & Live Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create groups with friends & chat live",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onCreateGroupClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_study_group_banner_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Group")
                }
            }
        }

        if (studyGroups.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No study groups yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create a study group, add your study friends, and start real-time messaging!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreateGroupClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_create_group_btn")
                    ) {
                        Text("Create Your First Group")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(studyGroups, key = { it.groupId }) { group ->
                    val memberCount = group.memberStudyIds.split(",").filter { it.isNotBlank() }.size
                    val isCreator = currentUser?.userId == group.createdByUserId ||
                            currentUser?.studyId.equals(group.createdByStudyId, ignoreCase = true)

                    val groupColor = remember(group.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(group.colorHex))
                        } catch (e: Exception) {
                            IndigoPrimary
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGroupChat(group) }
                            .testTag("study_group_card_${group.groupId}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(groupColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$memberCount members • Created by ${group.createdByName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "LIVE 💬",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (group.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (group.lastMessageText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = group.lastMessageText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCreator) {
                                    TextButton(
                                        onClick = { onDeleteGroup(group.groupId) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", fontSize = 12.sp)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Button(
                                    onClick = { onOpenGroupChat(group) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Chat")
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
private fun AddFriendContent(
    currentUser: UserEntity?,
    searchQuery: String,
    searchResultUser: UserEntity?,
    searchMessage: String?,
    isSearching: Boolean,
    isOnlineSyncing: Boolean,
    onlineUsers: List<OnlineUser>,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenQRScanner: () -> Unit,
    onSendRequest: (String) -> Unit,
    onSyncOnline: () -> Unit,
    onSelectOnlineUser: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Online Community Live Cloud Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = EmeraldAccent.copy(alpha = 0.12f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌐", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Online Community Sync",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldAccent
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Global directory active. Search any friend by Study ID across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onSyncOnline,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isOnlineSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh Online",
                            tint = EmeraldAccent
                        )
                    }
                }
            }
        }

        // My Study ID Card (Easy Copy & Share)
        if (currentUser != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your Study ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentUser.studyId,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Study ID", currentUser.studyId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied ID: ${currentUser.studyId}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Add me on Study With Buddy! My Study ID is ${currentUser.studyId}. Let's study together!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Study ID"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Search Input Field
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Friend by Study ID",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter full code (e.g. STU-FVBW7A, STU-7K92P4) or letters without prefix.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChanged,
                        placeholder = { Text("e.g. STU-FVBW7A") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("friend_search_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSearch,
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("search_friend_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenQRScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_qr_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📷 Live Camera Scan QR (Direct Open ID)")
                }
            }
        }

        // Feedback / Result message
        searchMessage?.let { msg ->
            Surface(
                color = if (msg.contains("not found", ignoreCase = true)) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (msg.contains("not found", ignoreCase = true)) Icons.Filled.Info else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (msg.contains("not found", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Search Result Card
        searchResultUser?.let { target ->
            val isMyself = target.userId == currentUser?.userId || target.studyId.equals(currentUser?.studyId, ignoreCase = true)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = target.fullName.take(1),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = target.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🌐 Online Student",
                                color = EmeraldAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${target.username} • Study ID: ${target.studyId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    if (!isMyself) {
                        Button(
                            onClick = { onSendRequest(target.studyId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_friend_request_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Friend Request")
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "This is your own profile",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Active Online Community Students
        val otherOnlineUsers = onlineUsers.filter { it.studyId != currentUser?.studyId }
        if (otherOnlineUsers.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🌐 Active Online Students",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${otherOnlineUsers.size} available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                otherOnlineUsers.take(6).forEach { onlineUser ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = onlineUser.fullName.take(1),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = onlineUser.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: ${onlineUser.studyId} • @${onlineUser.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    onSelectOnlineUser(onlineUser.studyId)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRequestsContent(
    requests: List<com.example.data.model.FriendRequestEntity>,
    friendsViewModel: FriendsViewModel
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pending friend requests.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(requests) { req ->
                var senderUser by remember { mutableStateOf<UserEntity?>(null) }
                LaunchedEffect(req.senderId) {
                    senderUser = friendsViewModel.getSenderUserForRequest(req.senderId)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = senderUser?.fullName?.take(1) ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = senderUser?.fullName ?: "Loading...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wants to be study friends",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            TextButton(onClick = { friendsViewModel.rejectRequest(req) }) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = { friendsViewModel.acceptRequest(req) }) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFeedContent(
    activityFeed: List<com.example.ui.viewmodel.FriendActivityItem>
) {
    if (activityFeed.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recent friend activity.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(activityFeed) { act ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${act.friendName} ${act.message}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = act.timeAgo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareStudyContent(
    currentUser: UserEntity?,
    myStats: UserStudyStats,
    friends: List<UserEntity>,
    selectedFriend: UserEntity?,
    friendStatsMap: Map<String, UserStudyStats>,
    onSelectFriendForCompare: (UserEntity?) -> Unit
) {
    var showFriendSelector by remember { mutableStateOf(false) }
    val friendStats = selectedFriend?.let { friendStatsMap[it.userId] } ?: UserStudyStats()

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Compare with:",
                    fontWeight = FontWeight.Bold
                )

                Box {
                    OutlinedButton(onClick = { showFriendSelector = true }) {
                        Text(selectedFriend?.fullName ?: "Select Friend")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showFriendSelector,
                        onDismissRequest = { showFriendSelector = false }
                    ) {
                        friends.forEach { friend ->
                            DropdownMenuItem(
                                text = { Text(friend.fullName) },
                                onClick = {
                                    onSelectFriendForCompare(friend)
                                    showFriendSelector = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFriend == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select an accepted friend above to compare study activity side-by-side! 🤝",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentUser?.fullName ?: "You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedFriend.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VioletTertiary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    CompareRow("Today's Study", AppRepository.formatDurationShort(myStats.todayTimeSeconds), AppRepository.formatDurationShort(friendStats.todayTimeSeconds))
                    CompareRow("Weekly Study", AppRepository.formatDurationShort(myStats.weeklyTimeSeconds), AppRepository.formatDurationShort(friendStats.weeklyTimeSeconds))
                    CompareRow("Monthly Study", AppRepository.formatDurationShort(myStats.monthlyTimeSeconds), AppRepository.formatDurationShort(friendStats.monthlyTimeSeconds))
                    CompareRow("Sessions", "${myStats.totalSessionsCount}", "${friendStats.totalSessionsCount}")
                    CompareRow("Streak", "${myStats.currentStreakDays} Days 🔥", "${friendStats.currentStreakDays} Days 🔥")
                    CompareRow("Top Subject", myStats.mostStudiedSubject, friendStats.mostStudiedSubject)
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, val1: String, val2: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = val1, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            Text(text = val2, fontWeight = FontWeight.Bold, color = VioletTertiary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}
