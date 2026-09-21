package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.data.model.FriendRequestEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.UserEntity
import com.example.data.remote.OnlineUser
import com.example.data.repository.AppRepository
import com.example.data.util.StatsCalculator
import com.example.data.util.UserStudyStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendActivityItem(
    val id: String,
    val friendName: String,
    val message: String,
    val timeAgo: String
)

class FriendsViewModel(private val repository: AppRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.loggedInUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentUserId: String?
        get() = currentUser.value?.userId

    // Accepted Friends List
    val friendsList: StateFlow<List<UserEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getAcceptedFriendsFlow(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending Requests
    val pendingReceivedRequests: StateFlow<List<FriendRequestEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getPendingReceivedRequests(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search by Study ID
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResultUser = MutableStateFlow<UserEntity?>(null)
    val searchResultUser: StateFlow<UserEntity?> = _searchResultUser.asStateFlow()

    private val _searchMessage = MutableStateFlow<String?>(null)
    val searchMessage: StateFlow<String?> = _searchMessage.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isOnlineSyncing = MutableStateFlow(false)
    val isOnlineSyncing: StateFlow<Boolean> = _isOnlineSyncing.asStateFlow()

    private val _onlineCommunityUsers = MutableStateFlow<List<OnlineUser>>(emptyList())
    val onlineCommunityUsers: StateFlow<List<OnlineUser>> = _onlineCommunityUsers.asStateFlow()

    private val _uiToast = MutableStateFlow<String?>(null)
    val uiToast: StateFlow<String?> = _uiToast.asStateFlow()

    // Selected Friend for Detail View & Comparison
    private val _selectedFriend = MutableStateFlow<UserEntity?>(null)
    val selectedFriend: StateFlow<UserEntity?> = _selectedFriend.asStateFlow()

    private val _compareFriend = MutableStateFlow<UserEntity?>(null)
    val compareFriend: StateFlow<UserEntity?> = _compareFriend.asStateFlow()

    // Friend Stats Cache Map (FriendId -> UserStudyStats)
    private val _friendStatsMap = MutableStateFlow<Map<String, UserStudyStats>>(emptyMap())
    val friendStatsMap: StateFlow<Map<String, UserStudyStats>> = _friendStatsMap.asStateFlow()

    // Activity Feed
    private val _activityFeed = MutableStateFlow<List<FriendActivityItem>>(emptyList())
    val activityFeed: StateFlow<List<FriendActivityItem>> = _activityFeed.asStateFlow()

    init {
        // Observe friends list and calculate/fetch stats for friends respecting privacy
        viewModelScope.launch {
            friendsList.collect { friends ->
                val newStatsMap = mutableMapOf<String, UserStudyStats>()
                val activities = mutableListOf<FriendActivityItem>()

                for (friend in friends) {
                    if (friend.privacyVisibility != "PRIVATE") {
                        val sessions = repository.getStudySessionsForUser(friend.userId).firstOrNull() ?: emptyList()
                        val stats = StatsCalculator.calculateStats(sessions)
                        newStatsMap[friend.userId] = stats

                        // Generate recent activity items for activity feed
                        val recentSession = sessions.firstOrNull()
                        if (recentSession != null) {
                            activities.add(
                                FriendActivityItem(
                                    id = "act_" + recentSession.sessionId,
                                    friendName = friend.fullName,
                                    message = "completed a ${AppRepository.formatDurationShort(recentSession.durationSeconds)} ${recentSession.subjectName} session.",
                                    timeAgo = "Recently"
                                )
                            )
                        }
                        if (stats.currentStreakDays >= 3) {
                            activities.add(
                                FriendActivityItem(
                                    id = "streak_" + friend.userId,
                                    friendName = friend.fullName,
                                    message = "reached a ${stats.currentStreakDays}-day study streak! 🔥",
                                    timeAgo = "Today"
                                )
                            )
                        }
                    }
                }
                _friendStatsMap.value = newStatsMap
                _activityFeed.value = activities
            }
        }

        // Trigger initial online sync and community directory loading
        viewModelScope.launch {
            currentUser.filterNotNull().collect {
                syncOnline()
            }
        }
    }

    fun syncOnline() {
        viewModelScope.launch {
            _isOnlineSyncing.value = true
            try {
                repository.syncCurrentUserOnline()
                val newReqs = repository.syncOnlineRequestsForCurrentUser()
                if (newReqs > 0) {
                    _uiToast.value = "Received $newReqs new online friend request(s)! 👥"
                }
                val onlineUsers = repository.fetchOnlineCommunityUsers()
                _onlineCommunityUsers.value = onlineUsers
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error in syncOnline", e)
            } finally {
                _isOnlineSyncing.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _searchMessage.value = null
        if (query.isBlank()) {
            _searchResultUser.value = null
        }
    }

    fun performSearchByStudyId() {
        val query = _searchQuery.value.trim().uppercase().replace(" ", "-").removePrefix("#").removePrefix("@")
        if (query.isBlank()) {
            _searchMessage.value = "Please enter a Study ID (e.g. STU-7K92P4 or FVBW7A)."
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _searchMessage.value = null
            _searchResultUser.value = null
            try {
                val foundUser = repository.searchUserByStudyId(query)
                if (foundUser == null) {
                    _searchResultUser.value = null
                    _searchMessage.value = "Study ID '$query' not found online or in local directory. Please check spelling or invite your friend to share their Study ID."
                } else if (foundUser.userId == currentUserId || foundUser.studyId.equals(currentUser.value?.studyId, ignoreCase = true)) {
                    _searchResultUser.value = foundUser
                    _searchMessage.value = "This is your own Study ID!"
                } else {
                    _searchResultUser.value = foundUser
                    _searchMessage.value = null
                }
            } catch (e: Exception) {
                _searchMessage.value = "Search error: ${e.localizedMessage}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendFriendRequest(targetStudyId: String) {
        val uId = currentUserId ?: return
        viewModelScope.launch {
            val result = repository.sendFriendRequest(uId, targetStudyId)
            result.onSuccess { msg ->
                _uiToast.value = msg
                _searchResultUser.value = null
                _searchQuery.value = ""
            }.onFailure { ex ->
                _uiToast.value = ex.message ?: "Could not send friend request."
            }
        }
    }

    fun acceptRequest(request: FriendRequestEntity) {
        viewModelScope.launch {
            repository.acceptFriendRequest(request)
            _uiToast.value = "Friend request accepted!"
        }
    }

    fun rejectRequest(request: FriendRequestEntity) {
        viewModelScope.launch {
            repository.rejectFriendRequest(request)
            _uiToast.value = "Friend request declined."
        }
    }

    fun removeFriend(friendId: String) {
        val uId = currentUserId ?: return
        viewModelScope.launch {
            repository.removeFriend(uId, friendId)
            _uiToast.value = "Friend removed."
            if (_selectedFriend.value?.userId == friendId) {
                _selectedFriend.value = null
            }
            if (_compareFriend.value?.userId == friendId) {
                _compareFriend.value = null
            }
        }
    }

    fun selectFriendForDetail(friend: UserEntity?) {
        _selectedFriend.value = friend
    }

    fun selectFriendForCompare(friend: UserEntity?) {
        _compareFriend.value = friend
    }

    suspend fun getSenderUserForRequest(senderId: String): UserEntity? {
        return repository.getUserById(senderId)
    }

    fun clearToast() {
        _uiToast.value = null
    }
}
