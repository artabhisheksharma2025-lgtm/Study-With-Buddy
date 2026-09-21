package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudySessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timerState by mainViewModel.timerState.collectAsState()
    val subjects by mainViewModel.subjects.collectAsState()
    val sessions by mainViewModel.studySessions.collectAsState()
    val stats by mainViewModel.userStats.collectAsState()
    val completedEvent by mainViewModel.completedSessionEvent.collectAsState()

    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showCreateSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var editSubjectNameInput by remember { mutableStateOf("") }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    var showFinishNotesDialog by remember { mutableStateOf(false) }
    var finishNotesInput by remember { mutableStateOf("") }

    // Search and filter states for History
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterSubject by remember { mutableStateOf<String?>(null) }
    var editingSession by remember { mutableStateOf<StudySessionEntity?>(null) }
    var deletingSessionId by remember { mutableStateOf<String?>(null) }

    val filteredSessions = remember(sessions, searchQuery, selectedFilterSubject) {
        sessions.filter { session ->
            val matchesQuery = searchQuery.isBlank() ||
                    session.title.contains(searchQuery, ignoreCase = true) ||
                    session.subjectName.contains(searchQuery, ignoreCase = true) ||
                    session.notes.contains(searchQuery, ignoreCase = true)
            val matchesSubject = selectedFilterSubject == null || session.subjectName == selectedFilterSubject
            matchesQuery && matchesSubject
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📚 Study Hub & Timer",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("screen_study")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Timer Stopwatch Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timer_card")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT SESSION",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Timer Display
                        Text(
                            text = AppRepository.formatTimerDisplay(timerState.elapsedSeconds),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("timer_display_text")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subject Dropdown Selector
                        Box {
                            OutlinedButton(
                                onClick = { showSubjectDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("select_subject_button")
                            ) {
                                Icon(Icons.Filled.Class, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subject: ${timerState.subjectName}")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showSubjectDropdown,
                                onDismissRequest = { showSubjectDropdown = false }
                            ) {
                                subjects.forEach { subject ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = subject.name,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            showSubjectDropdown = false
                                                            editSubjectNameInput = subject.name
                                                            subjectToEdit = subject
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Edit,
                                                            contentDescription = "Edit Subject",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            showSubjectDropdown = false
                                                            subjectToDelete = subject
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Delete,
                                                            contentDescription = "Delete Subject",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            mainViewModel.selectSubjectForTimer(subject)
                                            showSubjectDropdown = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "+ Create New Subject",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showSubjectDropdown = false
                                        showCreateSubjectDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Session Title Input
                        OutlinedTextField(
                            value = timerState.title,
                            onValueChange = { mainViewModel.setTimerTitle(it) },
                            placeholder = { Text("Session Title (e.g. Chapter 5 Revision)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timer_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Timer Controls (Start/Pause, Resume, Finish)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (!timerState.isRunning) {
                                Button(
                                    onClick = { mainViewModel.startOrResumeTimer() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("timer_start_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (timerState.elapsedSeconds > 0) "Resume" else "Start Studying")
                                }
                            } else {
                                Button(
                                    onClick = { mainViewModel.pauseTimer() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("timer_pause_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            if (timerState.elapsedSeconds > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { showFinishNotesDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("timer_finish_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish")
                                }
                            }
                        }
                    }
                }
            }

            // Subject Management Breakdown Cards
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📚 My Subjects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCreateSubjectDialog = true }) {
                            Text("+ Add Subject")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects, key = { it.subjectId }) { subject ->
                            val subStat = stats.subjectBreakdown.find { it.subjectName.equals(subject.name, ignoreCase = true) }
                            val totalTimeSec = subStat?.totalTimeSeconds ?: 0L
                            val sessionCount = subStat?.sessionCount ?: 0
                            val isSelected = selectedFilterSubject == subject.name

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable {
                                        selectedFilterSubject = if (isSelected) null else subject.name
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editSubjectNameInput = subject.name
                                                    subjectToEdit = subject
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = "Edit Subject",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    subjectToDelete = subject
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Delete Subject",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = AppRepository.formatDurationShort(totalTimeSec),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$sessionCount sessions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Study History Section Header & Search Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 Study History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedFilterSubject != null) {
                            TextButton(onClick = { selectedFilterSubject = null }) {
                                Text("Clear Filter ($selectedFilterSubject)")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by title, subject or notes...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study sessions found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredSessions) { session ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_item_${session.sessionId}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = session.title.ifBlank { "Study Session" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (session.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = session.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = AppRepository.formatDurationShort(session.durationSeconds),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.sessionDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row {
                                    IconButton(
                                        onClick = { editingSession = session },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { deletingSessionId = session.sessionId },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Finish Session Notes Dialog
        if (showFinishNotesDialog) {
            AlertDialog(
                onDismissRequest = { showFinishNotesDialog = false },
                title = { Text("Finish Study Session 🎉") },
                text = {
                    Column {
                        Text("Add notes for this session (Optional):")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = finishNotesInput,
                            onValueChange = { finishNotesInput = it },
                            placeholder = { Text("e.g. Revised chapters 1-3, completed formula review.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.finishSession(finishNotesInput)
                            showFinishNotesDialog = false
                            finishNotesInput = ""
                        },
                        modifier = Modifier.testTag("save_session_button")
                    ) {
                        Text("Save Session")
                    }
                },
                dismissButton = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showFinishNotesDialog = false }) {
                            Text("Cancel")
                        }
                        OutlinedButton(
                            onClick = {
                                mainViewModel.finishSession("")
                                showFinishNotesDialog = false
                                finishNotesInput = ""
                            },
                            modifier = Modifier.testTag("skip_notes_button")
                        ) {
                            Text("Skip & Save")
                        }
                    }
                }
            )
        }

        // Completion Dialog Event
        completedEvent?.let { compSession ->
            AlertDialog(
                onDismissRequest = { mainViewModel.dismissCompletedSessionEvent() },
                title = { Text("Study Session Completed 🎉") },
                text = {
                    Column {
                        Text("Subject: ${compSession.subjectName}", fontWeight = FontWeight.Bold)
                        Text("Duration: ${AppRepository.formatDurationShort(compSession.durationSeconds)}")
                        Text("Date: ${compSession.sessionDate}")
                        if (compSession.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Notes: ${compSession.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { mainViewModel.dismissCompletedSessionEvent() }) {
                        Text("Awesome!")
                    }
                }
            )
        }

        // Create Custom Subject Dialog
        if (showCreateSubjectDialog) {
            AlertDialog(
                onDismissRequest = { showCreateSubjectDialog = false },
                title = { Text("Create New Subject") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = { newSubjectName = it },
                            label = { Text("Subject Name") },
                            placeholder = { Text("e.g. Economics, Law, Microbiology") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSubjectName.isNotBlank()) {
                                mainViewModel.createSubject(newSubjectName)
                                showCreateSubjectDialog = false
                                newSubjectName = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateSubjectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Session Dialog
        editingSession?.let { sess ->
            var editTitle by remember { mutableStateOf(sess.title) }
            var editNotes by remember { mutableStateOf(sess.notes) }

            AlertDialog(
                onDismissRequest = { editingSession = null },
                title = { Text("Edit Session") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.updateSession(
                                sess.copy(title = editTitle, notes = editNotes)
                            )
                            editingSession = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingSession = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Confirm Delete Dialog
        deletingSessionId?.let { delId ->
            AlertDialog(
                onDismissRequest = { deletingSessionId = null },
                title = { Text("Delete Session?") },
                text = { Text("Are you sure you want to delete this study session? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.deleteSession(delId)
                            deletingSessionId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingSessionId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Subject Dialog
        subjectToEdit?.let { sub ->
            AlertDialog(
                onDismissRequest = { subjectToEdit = null },
                title = { Text("Edit Subject Name ✏️") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editSubjectNameInput,
                            onValueChange = { editSubjectNameInput = it },
                            label = { Text("Subject Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_subject_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editSubjectNameInput.isNotBlank()) {
                                mainViewModel.updateSubject(sub, editSubjectNameInput)
                                subjectToEdit = null
                            }
                        },
                        modifier = Modifier.testTag("save_subject_edit_button")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { subjectToEdit = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Subject Dialog
        subjectToDelete?.let { sub ->
            AlertDialog(
                onDismissRequest = { subjectToDelete = null },
                title = { Text("Delete Subject?") },
                text = { Text("Are you sure you want to delete subject '${sub.name}'? Existing study sessions in history will be kept.") },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.deleteSubject(sub)
                            subjectToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_subject_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { subjectToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
