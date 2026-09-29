package com.example.data.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean,
    val isSystemApp: Boolean = false,
    val category: String = "App",
    val isMockInstalled: Boolean = false
)

object FocusModeHelper {

    val DEFAULT_ALLOWED_PACKAGES = setOf(
        "com.google.android.calculator",
        "com.android.calculator2",
        "com.google.android.keep",
        "com.google.android.apps.docs",
        "com.google.android.apps.books",
        "com.android.chrome",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.google.android.calendar",
        "com.android.calendar",
        "com.example.studywithbuddy"
    )

    // Standard popular apps provided so testing in emulator or fresh devices is rich and immediate
    val POPULAR_STUDY_AND_DISTRACTING_APPS = listOf(
        // Study Essentials (Recommended Allowed)
        InstalledAppItem("com.google.android.calculator", "Calculator", isAllowed = true, category = "Study Tools"),
        InstalledAppItem("com.google.android.keep", "Google Keep / Notes", isAllowed = true, category = "Study Tools"),
        InstalledAppItem("com.android.chrome", "Chrome Browser", isAllowed = true, category = "Research & Web"),
        InstalledAppItem("com.google.android.apps.docs", "Google Docs & PDF", isAllowed = true, category = "Study Tools"),
        InstalledAppItem("com.google.android.apps.books", "Google Play Books", isAllowed = true, category = "Reading"),
        InstalledAppItem("com.google.android.calendar", "Calendar", isAllowed = true, category = "Planning"),
        InstalledAppItem("com.google.android.deskclock", "Clock & Timer", isAllowed = true, category = "Utilities"),

        // Distracting Apps (Recommended Not Allowed)
        InstalledAppItem("com.google.android.youtube", "YouTube", isAllowed = false, category = "Entertainment"),
        InstalledAppItem("com.instagram.android", "Instagram", isAllowed = false, category = "Social Media"),
        InstalledAppItem("com.whatsapp", "WhatsApp", isAllowed = false, category = "Messaging"),
        InstalledAppItem("com.facebook.katana", "Facebook", isAllowed = false, category = "Social Media"),
        InstalledAppItem("com.snapchat.android", "Snapchat", isAllowed = false, category = "Social Media"),
        InstalledAppItem("com.twitter.android", "X (Twitter)", isAllowed = false, category = "Social Media"),
        InstalledAppItem("com.reddit.frontpage", "Reddit", isAllowed = false, category = "Entertainment"),
        InstalledAppItem("com.netflix.mediaclient", "Netflix", isAllowed = false, category = "Entertainment"),
        InstalledAppItem("com.spotify.music", "Spotify", isAllowed = false, category = "Music & Audio"),
        InstalledAppItem("com.zhiliaoapp.musically", "TikTok", isAllowed = false, category = "Social Media")
    )

    fun parseAllowedApps(allowedAppsString: String?): Set<String> {
        if (allowedAppsString.isNullOrBlank()) {
            return DEFAULT_ALLOWED_PACKAGES
        }
        return allowedAppsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun serializeAllowedApps(packageNames: Set<String>): String {
        return packageNames.joinToString(",")
    }

    fun isPackageAllowed(packageName: String, allowedPackages: Set<String>): Boolean {
        // Study With Buddy itself is always permitted
        if (packageName == "com.example" || packageName.startsWith("com.aistudio.studytracker")) {
            return true
        }
        return allowedPackages.contains(packageName)
    }

    /**
     * Retrieves all installed launchable applications merged with standard apps.
     */
    fun loadInstalledApps(
        context: Context,
        allowedPackages: Set<String>
    ): List<InstalledAppItem> {
        val result = mutableListOf<InstalledAppItem>()
        val seenPackages = mutableSetOf<String>()

        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName) continue // Skip this app
                if (!seenPackages.add(pkg)) continue

                val appName = try {
                    ri.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg
                }
                val isSystem = (ri.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isAllowed = allowedPackages.contains(pkg)

                val category = when {
                    appName.contains("Calculator", ignoreCase = true) -> "Study Tools"
                    appName.contains("Note", ignoreCase = true) || appName.contains("Doc", ignoreCase = true) -> "Study Tools"
                    appName.contains("Browser", ignoreCase = true) || appName.contains("Chrome", ignoreCase = true) -> "Research & Web"
                    appName.contains("Clock", ignoreCase = true) || appName.contains("Timer", ignoreCase = true) -> "Utilities"
                    appName.contains("Camera", ignoreCase = true) || appName.contains("Gallery", ignoreCase = true) -> "Media"
                    isSystem -> "System App"
                    else -> "Installed App"
                }

                result.add(
                    InstalledAppItem(
                        packageName = pkg,
                        appName = appName,
                        isAllowed = isAllowed,
                        isSystemApp = isSystem,
                        category = category,
                        isMockInstalled = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FocusModeHelper", "Error loading installed applications", e)
        }

        // Add standard study and popular apps if not already present from device
        for (item in POPULAR_STUDY_AND_DISTRACTING_APPS) {
            if (seenPackages.add(item.packageName)) {
                val isAllowed = allowedPackages.contains(item.packageName)
                result.add(item.copy(isAllowed = isAllowed, isMockInstalled = true))
            }
        }

        return result.sortedWith(
            compareByDescending<InstalledAppItem> { it.isAllowed }
                .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Attempt to launch an app if allowed, or invoke onRestricted callback.
     */
    fun launchAppOrCheckRestriction(
        context: Context,
        app: InstalledAppItem,
        isFocusModeActive: Boolean,
        onRestricted: (InstalledAppItem) -> Unit
    ) {
        if (isFocusModeActive && !app.isAllowed) {
            onRestricted(app)
            return
        }

        // Launch app via Intent if physically installed
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                // If not installed on this specific device, show simulated launch or inform user
                android.widget.Toast.makeText(
                    context,
                    "Opening ${app.appName} (Allowed by Focus Mode)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.w("FocusModeHelper", "Cannot launch ${app.packageName}", e)
        }
    }
}
