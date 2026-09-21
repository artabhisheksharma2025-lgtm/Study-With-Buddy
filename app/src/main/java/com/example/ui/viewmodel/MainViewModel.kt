package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.data.util.StatsCalculator
import com.example.data.util.UserStudyStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.loggedInUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentUserId: String?
        get() = currentUser.value?.userId

    // Subjects Flow
    val subjects: StateFlow<List<SubjectEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getSubjectsForUser(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Sessions Flow
    val studySessions: StateFlow<List<StudySessionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getStudySessionsForUser(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Stats StateFlow
    val userStats: StateFlow<UserStudyStats> = studySessions
        .map { sessions -> StatsCalculator.calculateStats(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStudyStats())

    // Active Timer State
    private val _timerState = MutableStateFlow(
        ActiveTimerEntity(
            userId = "",
            subjectId = "",
            subjectName = "Mathematics",
            title = "",
            elapsedSeconds = 0L,
            isRunning = false
        )
    )
    val timerState: StateFlow<ActiveTimerEntity> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // Goals Flow
    val studyGoals: StateFlow<List<StudyGoalEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getGoalsForUser(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications Flow
    val notifications: StateFlow<List<AppNotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotificationsForUser(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast/Dialog Feedback
    private val _uiEventMessage = MutableStateFlow<String?>(null)
    val uiEventMessage: StateFlow<String?> = _uiEventMessage.asStateFlow()

    private val _completedSessionEvent = MutableStateFlow<StudySessionEntity?>(null)
    val completedSessionEvent: StateFlow<StudySessionEntity?> = _completedSessionEvent.asStateFlow()

    init {
        // Observe active timer from DB on launch
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    val savedTimer = repository.getActiveTimer(user.userId).firstOrNull()
                    if (savedTimer != null) {
                        var elapsed = savedTimer.elapsedSeconds
                        var running = savedTimer.isRunning
                        if (running) {
                            val now = System.currentTimeMillis()
                            val deltaSec = (now - savedTimer.lastUpdatedMillis) / 1000
                            if (deltaSec > 0) {
                                elapsed += deltaSec
                            }
                        }
                        _timerState.value = savedTimer.copy(
                            elapsedSeconds = elapsed,
                            isRunning = running,
                            lastUpdatedMillis = System.currentTimeMillis()
                        )
                        if (running) {
                            startTimerCoroutines()
                        }
                    } else {
                        _timerState.value = ActiveTimerEntity(
                            userId = user.userId,
                            subjectId = subjects.value.firstOrNull()?.subjectId ?: "",
                            subjectName = subjects.value.firstOrNull()?.name ?: "Mathematics",
                            elapsedSeconds = 0L,
                            isRunning = false
                        )
                    }
                }
            }
        }
    }

    // --- Timer Actions ---
    fun selectSubjectForTimer(subject: SubjectEntity) {
        _timerState.value = _timerState.value.copy(
            subjectId = subject.subjectId,
            subjectName = subject.name
        )
        saveCurrentTimerState()
    }

    fun setTimerTitle(title: String) {
        _timerState.value = _timerState.value.copy(title = title)
        saveCurrentTimerState()
    }

    fun startOrResumeTimer() {
        val uId = currentUserId ?: return
        val current = _timerState.value
        val startTime = if (current.startTimeMillis == 0L) System.currentTimeMillis() else current.startTimeMillis

        _timerState.value = current.copy(
            userId = uId,
            isRunning = true,
            startTimeMillis = startTime,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        startTimerCoroutines()
        saveCurrentTimerState()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        saveCurrentTimerState()
    }

    fun resetTimer() {
        timerJob?.cancel()
        val uId = currentUserId ?: return
        val defaultSub = subjects.value.firstOrNull()
        _timerState.value = ActiveTimerEntity(
            userId = uId,
            subjectId = defaultSub?.subjectId ?: "",
            subjectName = defaultSub?.name ?: "Mathematics",
            elapsedSeconds = 0L,
            isRunning = false
        )
        viewModelScope.launch {
            repository.clearActiveTimer(uId)
        }
    }

    fun finishSession(notes: String = "") {
        val uId = currentUserId ?: return
        val state = _timerState.value
        if (state.elapsedSeconds < 5) {
            _uiEventMessage.value = "Session too short to save (minimum 5 seconds)."
            return
        }

        pauseTimer()
        val endTime = System.currentTimeMillis()
        val startTime = if (state.startTimeMillis > 0) state.startTimeMillis else endTime - (state.elapsedSeconds * 1000)

        viewModelScope.launch {
            val session = repository.saveStudySession(
                userId = uId,
                subjectId = state.subjectId,
                subjectName = state.subjectName,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = state.elapsedSeconds,
                title = state.title.ifBlank { "${state.subjectName} Session" },
                notes = notes
            )
            _completedSessionEvent.value = session
            resetTimer()
        }
    }

    private fun startTimerCoroutines() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.isRunning) {
                delay(1000L)
                val newElapsed = _timerState.value.elapsedSeconds + 1
                _timerState.value = _timerState.value.copy(
                    elapsedSeconds = newElapsed,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
                if (newElapsed % 5 == 0L) { // persist timer every 5 seconds
                    saveCurrentTimerState()
                }
            }
        }
    }

    private fun saveCurrentTimerState() {
        val uId = currentUserId ?: return
        viewModelScope.launch {
            repository.saveActiveTimer(_timerState.value.copy(userId = uId))
        }
    }

    fun dismissCompletedSessionEvent() {
        _completedSessionEvent.value = null
    }

    // --- Manual Session Entry ---
    fun addManualSession(
        subjectName: String,
        durationMinutes: Long,
        title: String,
        notes: String
    ) {
        val uId = currentUserId ?: return
        if (durationMinutes <= 0) {
            _uiEventMessage.value = "Duration must be greater than 0 minutes."
            return
        }

        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val durationSec = durationMinutes * 60
            val startTime = endTime - (durationSec * 1000)
            val sub = subjects.value.find { it.name.equals(subjectName, ignoreCase = true) }
            val subId = sub?.subjectId ?: "sub_custom_$uId"

            repository.saveStudySession(
                userId = uId,
                subjectId = subId,
                subjectName = subjectName,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSec,
                title = title.ifBlank { "$subjectName Session" },
                notes = notes
            )
            _uiEventMessage.value = "Study session saved!"
        }
    }

    // --- Session Editing & Deletion ---
    fun updateSession(session: StudySessionEntity) {
        viewModelScope.launch {
            repository.updateStudySession(session)
            _uiEventMessage.value = "Session updated."
        }
    }

    fun deleteSession(sessionId: String) {
        val uId = currentUserId ?: return
        viewModelScope.launch {
            repository.deleteStudySession(sessionId, uId)
            _uiEventMessage.value = "Session deleted."
        }
    }

    // --- Custom Subject ---
    fun createSubject(name: String, colorHex: String = "#3F51B5") {
        val uId = currentUserId ?: return
        if (name.isBlank()) {
            _uiEventMessage.value = "Subject name cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.createCustomSubject(uId, name, colorHex)
            _uiEventMessage.value = "Subject '$name' added!"
        }
    }

    fun updateSubject(subject: com.example.data.model.SubjectEntity, newName: String) {
        if (newName.isBlank()) {
            _uiEventMessage.value = "Subject name cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.updateSubject(subject.copy(name = newName.trim()))
            _uiEventMessage.value = "Subject updated to '$newName'!"
        }
    }

    fun deleteSubject(subject: com.example.data.model.SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _uiEventMessage.value = "Subject '${subject.name}' deleted."
        }
    }

    // --- Goal Operations ---
    fun createGoal(
        title: String,
        targetDurationHours: Int,
        targetSessions: Int,
        daysCount: Int,
        subjectName: String
    ) {
        val uId = currentUserId ?: return
        if (title.isBlank()) {
            _uiEventMessage.value = "Please enter a goal title."
            return
        }
        val start = System.currentTimeMillis()
        val end = start + (daysCount * 86400000L)

        viewModelScope.launch {
            repository.createGoal(
                userId = uId,
                title = title,
                targetDurationMinutes = targetDurationHours * 60,
                targetSessions = targetSessions,
                startDate = start,
                endDate = end,
                subjectName = subjectName
            )
            _uiEventMessage.value = "Study goal created!"
        }
    }

    fun deleteGoal(goal: StudyGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _uiEventMessage.value = "Goal removed."
        }
    }

    // --- Profile & Privacy Settings ---
    fun updatePrivacySetting(visibility: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(user.copy(privacyVisibility = visibility))
            _uiEventMessage.value = "Privacy set to $visibility."
        }
    }

    fun updateProfileInfo(fullName: String, username: String) {
        val user = currentUser.value ?: return
        if (fullName.isBlank() || username.isBlank()) {
            _uiEventMessage.value = "Fields cannot be blank."
            return
        }
        viewModelScope.launch {
            repository.updateUserProfile(user.copy(fullName = fullName.trim(), username = username.trim()))
            _uiEventMessage.value = "Profile updated."
        }
    }

    fun updateNotificationSetting(category: String, enabled: Boolean) {
        val user = currentUser.value ?: return
        val updated = when (category) {
            "goals" -> user.copy(notificationGoalReminders = enabled)
            "streak" -> user.copy(notificationStreakReminders = enabled)
            "requests" -> user.copy(notificationFriendRequests = enabled)
            "activity" -> user.copy(notificationFriendActivity = enabled)
            else -> user
        }
        viewModelScope.launch {
            repository.updateUserProfile(updated)
        }
    }

    fun clearUiMessage() {
        _uiEventMessage.value = null
    }
}
