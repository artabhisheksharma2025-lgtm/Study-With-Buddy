package com.example.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object SecurityUtils {

    /**
     * Generates a cryptographically secure 16-byte random salt.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Hashes password using SHA-256 with unique per-user salt and multiple iterations.
     * Never stores plaintext passwords anywhere.
     */
    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        var hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        // Multiple rounds of hashing
        for (i in 0 until 1000) {
            digest.reset()
            hash = digest.digest(hash)
        }
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Verifies if raw password matches stored hash with the provided salt.
     */
    fun verifyPassword(password: String, saltBase64: String, storedHash: String): Boolean {
        val calculated = hashPassword(password, saltBase64)
        return calculated == storedHash
    }

    // Role definitions
    object Roles {
        const val SUPER_ADMIN = "SUPER_ADMIN"
        const val ADMIN = "ADMIN"
        const val MODERATOR = "MODERATOR"
        const val SUPPORT_ADMIN = "SUPPORT_ADMIN"

        val ALL_ROLES = listOf(SUPER_ADMIN, ADMIN, MODERATOR, SUPPORT_ADMIN)
    }

    // Role permission checkers
    fun canManageAdmins(role: String): Boolean = role == Roles.SUPER_ADMIN

    fun canManageUsers(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN, Roles.MODERATOR)

    fun canDeleteUser(role: String): Boolean = role == Roles.SUPER_ADMIN

    fun canManageSessions(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canManageSubjects(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canManageReports(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN, Roles.MODERATOR)

    fun canManageAnnouncements(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canManageNotifications(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canManageChallenges(role: String): Boolean =
        role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canManageSettings(role: String): Boolean = role == Roles.SUPER_ADMIN

    fun canViewAuditLogs(role: String): Boolean = role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canExportData(role: String): Boolean = role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)

    fun canViewSystemHealth(role: String): Boolean = role in listOf(Roles.SUPER_ADMIN, Roles.ADMIN)
}
