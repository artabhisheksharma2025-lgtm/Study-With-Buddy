package com.example.data.remote

import android.util.Log
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
}
