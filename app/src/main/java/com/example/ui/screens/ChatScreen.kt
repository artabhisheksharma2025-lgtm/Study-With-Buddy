package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.StudyGroupEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FriendsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    friendsViewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by friendsViewModel.currentUser.collectAsState()
    val activeFriend by friendsViewModel.activeDirectChatFriend.collectAsState()
    val activeGroup by friendsViewModel.activeStudyGroup.collectAsState()
    val messages by friendsViewModel.chatMessages.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showGroupInfoDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Automatically scroll to bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isGroup = activeGroup != null
    val chatTitle = if (isGroup) activeGroup?.name ?: "Study Group" else activeFriend?.fullName ?: "Chat"
    val chatSubtitle = if (isGroup) {
        val count = activeGroup?.memberStudyIds?.split(",")?.filter { it.isNotBlank() }?.size ?: 1
        "$count members • Group Study Chat"
    } else {
        "Study ID: ${activeFriend?.studyId ?: ""} • Real-Time Chat 🟢"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { friendsViewModel.closeChat() },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGroup) {
                                        try {
                                            Color(android.graphics.Color.parseColor(activeGroup?.colorHex ?: "#3F51B5"))
                                        } catch (e: Exception) {
                                            IndigoPrimary
                                        }
                                    } else {
                                        IndigoPrimary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGroup) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = chatTitle.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Column {
                            Text(
                                text = chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = chatSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGroup) MaterialTheme.colorScheme.primary else EmeraldAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    if (isGroup) {
                        IconButton(
                            onClick = { showGroupInfoDialog = true },
                            modifier = Modifier.testTag("group_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Group Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("chat_conversation_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isGroup) Icons.Filled.Groups else Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isGroup) "No messages in ${activeGroup?.name} yet." else "Say hello to ${activeFriend?.fullName}! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Messages are synchronized in real time across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.messageId }) { msg ->
                            val isMe = msg.senderUserId == currentUser?.userId ||
                                    msg.senderStudyId.equals(currentUser?.studyId, ignoreCase = true)

                            ChatMessageBubble(
                                message = msg,
                                isMe = isMe,
                                showSenderName = isGroup && !isMe
                            )
                        }
                    }
                }
            }

            // Quick Study Prompt Chips
            val quickPrompts = listOf(
                "Let's study together! 📚",
                "Starting study timer ⏱️",
                "How is your goal going? 🎯",
                "Break time! ☕",
                "Completed my session! 🔥"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickPrompts) { prompt ->
                    SuggestionChip(
                        onClick = {
                            friendsViewModel.sendChatMessage(prompt)
                        },
                        label = { Text(prompt, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isGroup) "Message ${activeGroup?.name}..." else "Message ${activeFriend?.fullName}...",
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                friendsViewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                friendsViewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message"
                        )
                    }
                }
            }
        }
    }

    // Group Info Dialog
    if (showGroupInfoDialog && activeGroup != null) {
        StudyGroupInfoDialog(
            group = activeGroup!!,
            currentUser = currentUser,
            onDismiss = { showGroupInfoDialog = false },
            onDeleteGroup = {
                friendsViewModel.deleteStudyGroup(activeGroup!!.groupId)
                showGroupInfoDialog = false
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    showSenderName: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (showSenderName) {
            Text(
                text = "${message.senderName} (${message.senderStudyId})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = if (isMe) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (isMe) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudyGroupInfoDialog(
    group: StudyGroupEntity,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val isCreator = currentUser?.userId == group.createdByUserId ||
            currentUser?.studyId.equals(group.createdByStudyId, ignoreCase = true)

    val memberNames = remember(group.memberNames) {
        group.memberNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val memberStudyIds = remember(group.memberStudyIds) {
        group.memberStudyIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(group.name, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Created By:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${group.createdByName} (${group.createdByStudyId})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Created Date:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sdf.format(Date(group.createdAt)), fontSize = 12.sp)
                }

                Text(
                    text = "Members (${memberNames.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    memberNames.forEachIndexed { idx, name ->
                        val sId = memberStudyIds.getOrNull(idx) ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                            }
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            if (sId.isNotBlank()) {
                                Text("($sId)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (isCreator) {
                TextButton(
                    onClick = onDeleteGroup,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Group")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStudyGroupDialog(
    friends: List<UserEntity>,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, description: String, selectedFriends: List<UserEntity>, colorHex: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFriends by remember { mutableStateOf<Set<UserEntity>>(emptySet()) }
    var selectedColor by remember { mutableStateOf("#3F51B5") }

    val groupColors = listOf(
        "#3F51B5" to "Indigo",
        "#009688" to "Teal",
        "#E91E63" to "Pink",
        "#FF9800" to "Orange",
        "#4CAF50" to "Green",
        "#673AB7" to "Purple"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Create Study Group", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g. Math Prep Squad, Science Team") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Goal") },
                    placeholder = { Text("e.g. Daily study sessions & doubts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Group Color Theme:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupColors.forEach { (colorHex, _) ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Add Friends (${selectedFriends.size} selected):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (friends.isEmpty()) {
                    Text(
                        text = "No study friends added yet. You can still create the group and add friends later!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(friends) { friend ->
                            val isChecked = friend in selectedFriends

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedFriends = if (isChecked) {
                                            selectedFriends - friend
                                        } else {
                                            selectedFriends + friend
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedFriends = if (checked) {
                                            selectedFriends + friend
                                        } else {
                                            selectedFriends - friend
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(friend.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(friend.studyId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(groupName.trim(), description.trim(), selectedFriends.toList(), selectedColor)
                    }
                },
                enabled = groupName.isNotBlank(),
                modifier = Modifier.testTag("submit_create_group_btn")
            ) {
                Text("Create Group & Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
