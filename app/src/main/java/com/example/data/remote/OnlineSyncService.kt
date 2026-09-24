package com.example.data.remote

import android.util.Log
import com.example.data.model.AnnouncementEntity
import com.example.data.model.UserEntity
import com.example.data.util.UserStudyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class OnlineUser(
    val userId: String,
    val fullName: String,
    val username: String,
    val studyId: String,
    val privacyVisibility: String = "PUBLIC",
    val currentStreakDays: Int = 0,
    val totalStudySeconds: Long = 0L,
    val lastActiveTime: Long = System.currentTimeMillis()
)

data class OnlineFriendRequest(
    val requestId: String,
    val senderStudyId: String,
    val senderName: String,
    val senderUserId: String,
    val receiverStudyId: String,
    val status: String,
    val timestamp: Long
)

object OnlineSyncService {
    private const val TAG = "OnlineSyncService"
    private const val BASE_URL = "https://sanchardb.pages.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value.replace(" ", "+")
        }
    }

    /**
     * Publishes or updates the user profile to the global cloud database so
     * any student can discover and add them by Study ID across devices.
     */
    suspend fun syncUserOnline(user: UserEntity, stats: UserStudyStats? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cleanStudyId = user.studyId.trim().uppercase()
                if (cleanStudyId.isBlank()) return@withContext false

                val fields = mutableListOf(
                    "userId" to user.userId,
                    "fullName" to user.fullName,
                    "username" to user.username,
                    "studyId" to cleanStudyId,
                    "privacyVisibility" to user.privacyVisibility,
                    "lastActiveTime" to System.currentTimeMillis().toString()
                )

                if (stats != null) {
                    fields.add("currentStreakDays" to stats.currentStreakDays.toString())
                    fields.add("totalStudySeconds" to stats.totalTimeSeconds.toString())
                }

                for ((key, value) in fields) {
                    val url = "$BASE_URL/set/studytracker/users/$cleanStudyId/$key?value=${encode(value)}"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().close()
                }

                Log.d(TAG, "Successfully synced user $cleanStudyId online")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync user online", e)
                false
            }
        }

    /**
     * Searches for a student by their Study ID in the online cloud database.
     */
    suspend fun searchUserOnline(studyId: String): OnlineUser? = withContext(Dispatchers.IO) {
        try {
            var cleanId = studyId.trim().uppercase()
            // Normalize: if user entered without "STU-", prepend it
            if (!cleanId.startsWith("STU-") && cleanId.length >= 4 && !cleanId.contains(" ")) {
                cleanId = "STU-$cleanId"
            }
            cleanId = cleanId.replace(" ", "-")

            val url = "$BASE_URL/get/studytracker/users/$cleanId"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            if (json.optString("status") != "success") {
                return@withContext null
            }

            val data = json.optJSONObject("data") ?: return@withContext null

            val parsedUser = OnlineUser(
                userId = data.optString("userId").ifBlank { "user_online_${cleanId.lowercase()}" },
                fullName = data.optString("fullName", "Student"),
                username = data.optString("username", cleanId.lowercase()),
                studyId = data.optString("studyId", cleanId),
                privacyVisibility = data.optString("privacyVisibility", "PUBLIC"),
                currentStreakDays = data.optString("currentStreakDays").toIntOrNull() ?: 0,
                totalStudySeconds = data.optString("totalStudySeconds").toLongOrNull() ?: 0L,
                lastActiveTime = data.optString("lastActiveTime").toLongOrNull() ?: System.currentTimeMillis()
            )

            parsedUser
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user online: $studyId", e)
            null
        }
    }

    /**
     * Fetches all registered students from the online cloud directory for community discovery.
     */
    suspend fun fetchAllOnlineUsers(): List<OnlineUser> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/get/studytracker/users"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                response.close()
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)

            if (json.optString("status") != "success") {
                return@withContext emptyList()
            }

            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val usersList = mutableListOf<OnlineUser>()

            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val userObj = data.optJSONObject(key) ?: continue
                val sId = userObj.optString("studyId", key)
                usersList.add(
                    OnlineUser(
                        userId = userObj.optString("userId").ifBlank { "user_online_${sId.lowercase()}" },
                        fullName = userObj.optString("fullName", "Student"),
                        username = userObj.optString("username", sId.lowercase()),
                        studyId = sId,
                        privacyVisibility = userObj.optString("privacyVisibility", "PUBLIC"),
                        currentStreakDays = userObj.optString("currentStreakDays").toIntOrNull() ?: 0,
                        totalStudySeconds = userObj.optString("totalStudySeconds").toLongOrNull() ?: 0L,
                        lastActiveTime = userObj.optString("lastActiveTime").toLongOrNull() ?: 0L
                    )
                )
            }

            usersList
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all online users", e)
            emptyList()
        }
    }

    /**
     * Sends an online friend request visible across different phones.
     */
    suspend fun sendOnlineFriendRequest(
        sender: UserEntity,
        targetStudyId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanTarget = targetStudyId.trim().uppercase()
            val cleanSender = sender.studyId.trim().uppercase()

            val baseRequestUrl = "$BASE_URL/set/studytracker/requests/$cleanTarget/$cleanSender"

            val fields = listOf(
                "senderStudyId" to cleanSender,
                "senderName" to sender.fullName,
                "senderUserId" to sender.userId,
                "receiverStudyId" to cleanTarget,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis().toString()
            )

            for ((k, v) in fields) {
                val url = "$baseRequestUrl/$k?value=${encode(v)}"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().close()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending online friend request", e)
            false
        }
    }

    /**
     * Fetches all pending requests sent to my Study ID from other phones.
     */
    suspend fun fetchIncomingOnlineRequests(myStudyId: String): List<OnlineFriendRequest> =
        withContext(Dispatchers.IO) {
            try {
                val cleanId = myStudyId.trim().uppercase()
                val url = "$BASE_URL/get/studytracker/requests/$cleanId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    response.close()
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                if (json.optString("status") != "success") {
                    return@withContext emptyList()
                }

                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val requestsList = mutableListOf<OnlineFriendRequest>()

                val keys = data.keys()
                while (keys.hasNext()) {
                    val senderKey = keys.next()
                    val reqObj = data.optJSONObject(senderKey) ?: continue
                    val status = reqObj.optString("status", "PENDING")
                    if (status == "PENDING") {
                        requestsList.add(
                            OnlineFriendRequest(
                                requestId = "online_req_${senderKey}_$cleanId",
                                senderStudyId = reqObj.optString("senderStudyId", senderKey),
                                senderName = reqObj.optString("senderName", "Friend"),
                                senderUserId = reqObj.optString("senderUserId", "user_$senderKey"),
                                receiverStudyId = cleanId,
                                status = status,
                                timestamp = reqObj.optString("timestamp").toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                }

                requestsList
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching incoming online requests", e)
                emptyList()
            }
        }

    /**
     * Updates an online friend request status (e.g. ACCEPTED or DECLINED).
     */
    suspend fun updateOnlineRequestStatus(
        myStudyId: String,
        senderStudyId: String,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanReceiver = myStudyId.trim().uppercase()
            val cleanSender = senderStudyId.trim().uppercase()
            val url = "$BASE_URL/set/studytracker/requests/$cleanReceiver/$cleanSender/status?value=${encode(status)}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating request status online", e)
            false
        }
    }

    /**
     * Publishes or updates an announcement on the global cloud database so that
     * all users' apps automatically receive the latest announcement when connected to the internet.
     */
    suspend fun syncAnnouncementOnline(announcement: AnnouncementEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("announcementId", announcement.announcementId)
                    put("title", announcement.title)
                    put("message", announcement.message)
                    put("imageUrl", announcement.imageUrl ?: "")
                    put("iconName", announcement.iconName ?: "campaign")
                    put("priority", announcement.priority)
                    put("startDate", announcement.startDate)
                    put("endDate", announcement.endDate)
                    put("status", announcement.status)
                    put("targetAudience", announcement.targetAudience)
                    put("displayLocation", announcement.displayLocation)
                    put("isDismissible", announcement.isDismissible)
                    put("actionLabel", announcement.actionLabel ?: "")
                    put("actionUrl", announcement.actionUrl ?: "")
                    put("createdDate", announcement.createdDate)
                    put("createdBy", announcement.createdBy)
                }.toString()

                val url = "$BASE_URL/set/studytracker/announcements/${announcement.announcementId}?value=${encode(json)}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                Log.d(TAG, "Synced announcement ${announcement.announcementId} online: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync announcement online: ${announcement.announcementId}", e)
                false
            }
        }

    /**
     * Deletes an announcement from the cloud database so it disappears from all users' apps.
     */
    suspend fun deleteAnnouncementOnline(announcementId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/delete/studytracker/announcements/$announcementId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                Log.d(TAG, "Deleted announcement $announcementId online: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete announcement online: $announcementId", e)
                false
            }
        }

    /**
     * Fetches all live announcements from the cloud database for user apps.
     */
    suspend fun fetchAllOnlineAnnouncements(): List<AnnouncementEntity> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/get/studytracker/announcements"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                if (root.optString("status") != "success") return@withContext emptyList()
                val data = root.optJSONObject("data") ?: return@withContext emptyList()

                val list = mutableListOf<AnnouncementEntity>()
                val keys = data.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val raw = data.optString(id)
                    val obj = try {
                        JSONObject(raw)
                    } catch (e: Exception) {
                        data.optJSONObject(id) ?: continue
                    }
                    list.add(
                        AnnouncementEntity(
                            announcementId = obj.optString("announcementId", id),
                            title = obj.optString("title", "Announcement"),
                            message = obj.optString("message", ""),
                            imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                            iconName = obj.optString("iconName", "campaign"),
                            priority = obj.optString("priority", "NORMAL"),
                            startDate = obj.optLong("startDate", System.currentTimeMillis()),
                            endDate = obj.optLong("endDate", System.currentTimeMillis() + 86400000L * 7),
                            status = obj.optString("status", "PUBLISHED"),
                            targetAudience = obj.optString("targetAudience", "EVERYONE"),
                            displayLocation = obj.optString("displayLocation", "USER_APP"),
                            isDismissible = obj.optBoolean("isDismissible", true),
                            actionLabel = obj.optString("actionLabel").takeIf { it.isNotBlank() },
                            actionUrl = obj.optString("actionUrl").takeIf { it.isNotBlank() },
                            createdDate = obj.optLong("createdDate", System.currentTimeMillis()),
                            createdBy = obj.optString("createdBy", "Admin")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching online announcements", e)
                emptyList()
            }
        }

    /**
     * Publishes registered or logged-in user credentials and details to the cloud database
     * so that the Admin Console can see their name, email, password, and status.
     */
    suspend fun syncUserAuthOnline(user: UserEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("userId", user.userId)
                    put("fullName", user.fullName)
                    put("username", user.username)
                    put("email", user.email)
                    put("passwordHash", user.passwordHash)
                    put("studyId", user.studyId)
                    put("accountStatus", user.accountStatus)
                    put("isDeleted", user.isDeleted)
                    put("joinedDate", user.joinedDate)
                    put("lastActiveTime", System.currentTimeMillis())
                }.toString()

                val url = "$BASE_URL/set/studytracker/users_auth/${user.userId}?value=${encode(json)}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                Log.d(TAG, "Synced user auth for ${user.email} online: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync user auth online: ${user.email}", e)
                false
            }
        }

    /**
     * Fetches all registered users from the cloud directory for Admin User Management.
     */
    suspend fun fetchAllOnlineAuthUsers(): List<UserEntity> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/get/studytracker/users_auth"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                if (root.optString("status") != "success") return@withContext emptyList()
                val data = root.optJSONObject("data") ?: return@withContext emptyList()

                val list = mutableListOf<UserEntity>()
                val keys = data.keys()
                while (keys.hasNext()) {
                    val uid = keys.next()
                    val raw = data.optString(uid)
                    val obj = try {
                        JSONObject(raw)
                    } catch (e: Exception) {
                        data.optJSONObject(uid) ?: continue
                    }
                    list.add(
                        UserEntity(
                            userId = obj.optString("userId", uid),
                            fullName = obj.optString("fullName", "Student"),
                            username = obj.optString("username", ""),
                            email = obj.optString("email", ""),
                            passwordHash = obj.optString("passwordHash", ""),
                            studyId = obj.optString("studyId", ""),
                            accountStatus = obj.optString("accountStatus", "ACTIVE"),
                            isDeleted = obj.optBoolean("isDeleted", false),
                            joinedDate = obj.optLong("joinedDate", System.currentTimeMillis()),
                            lastActiveTime = obj.optLong("lastActiveTime", System.currentTimeMillis()),
                            isLoggedIn = false
                        )
                    )
                }
                list
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching online auth users", e)
                emptyList()
            }
        }

    /**
     * Deletes user credentials from the cloud database when an admin deletes them.
     */
    suspend fun deleteUserAuthOnline(userId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/delete/studytracker/users_auth/$userId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                Log.d(TAG, "Deleted user auth $userId online: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete user auth online: $userId", e)
                false
            }
        }
}
