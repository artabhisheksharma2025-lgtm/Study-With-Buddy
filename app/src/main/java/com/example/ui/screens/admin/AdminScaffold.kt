package com.example.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminSection
import com.example.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    onExitToUserApp: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentSection by adminViewModel.currentSection.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showSearchDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val successMessage by adminViewModel.successMessage.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val loadingText by adminViewModel.loadingText.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessages()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 768.dp

        if (isWide) {
            // Tablet / Desktop Layout with Persistent Sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                AdminSidebarContent(
                    admin = admin,
                    currentSection = currentSection,
                    onSectionSelect = { adminViewModel.selectSection(it) },
                    onExitToUserApp = onExitToUserApp,
                    onOpenExport = { showExportDialog = true },
                    modifier = Modifier.width(260.dp)
                )

                // Main Content Area
                Scaffold(
                    topBar = {
                        AdminTopAppBar(
                            admin = admin,
                            title = currentSection.title,
                            showMenuButton = false,
                            onOpenMenu = {},
                            onOpenSearch = { showSearchDialog = true },
                            onRefresh = { adminViewModel.refreshDashboard() },
                            onExit = onExitToUserApp
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        content()

                        if (isLoading) {
                            LoadingOverlay(loadingText = loadingText)
                        }
                    }
                }
            }
        } else {
            // Mobile Layout with Modal Drawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(280.dp),
                        drawerContainerColor = Color(0xFF0F172A)
                    ) {
                        AdminSidebarContent(
                            admin = admin,
                            currentSection = currentSection,
                            onSectionSelect = {
                                adminViewModel.selectSection(it)
                                scope.launch { drawerState.close() }
                            },
                            onExitToUserApp = onExitToUserApp,
                            onOpenExport = {
                                showExportDialog = true
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        AdminTopAppBar(
                            admin = admin,
                            title = currentSection.title,
                            showMenuButton = true,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onOpenSearch = { showSearchDialog = true },
                            onRefresh = { adminViewModel.refreshDashboard() },
                            onExit = onExitToUserApp
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        content()

                        if (isLoading) {
                            LoadingOverlay(loadingText = loadingText)
                        }
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        AdminGlobalSearchDialog(
            adminViewModel = adminViewModel,
            onDismiss = { showSearchDialog = false },
            onSelectSection = { section ->
                adminViewModel.selectSection(section)
                showSearchDialog = false
            }
        )
    }

    if (showExportDialog) {
        AdminExportDialog(
            adminViewModel = adminViewModel,
            onDismiss = { showExportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopAppBar(
    admin: AdminEntity,
    title: String,
    showMenuButton: Boolean,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    onExit: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "System Operational",
                        fontSize = 11.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            if (showMenuButton) {
                IconButton(onClick = onOpenMenu, modifier = Modifier.testTag("admin_drawer_menu_button")) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("admin_global_search_button")) {
                Icon(Icons.Filled.Search, contentDescription = "Search System")
            }
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("admin_refresh_button")) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
            }
            // Admin Profile Badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = admin.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = admin.role.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun AdminSidebarContent(
    admin: AdminEntity,
    currentSection: AdminSection,
    onSectionSelect: (AdminSection) -> Unit,
    onExitToUserApp: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F172A))
            .padding(14.dp)
    ) {
        // App Title & Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "STUDY TRACKER",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Admin Management Area",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }

        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))

        // Navigation Menu
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminSection.values().forEach { section ->
                // Filter out Admin Management if not super admin
                if (section == AdminSection.ADMIN_MANAGEMENT && !SecurityUtils.canManageAdmins(admin.role)) {
                    return@forEach
                }

                val isSelected = currentSection == section
                val icon = getSectionIcon(section)

                Surface(
                    onClick = {
                        if (section == AdminSection.EXPORT) {
                            onOpenExport()
                        } else {
                            onSectionSelect(section)
                        }
                    },
                    color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_nav_${section.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = section.title,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

        // Exit button
        OutlinedButton(
            onClick = onExitToUserApp,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Student App", fontSize = 12.sp)
        }
    }
}

private fun getSectionIcon(section: AdminSection): ImageVector = when (section) {
    AdminSection.DASHBOARD -> Icons.Filled.Dashboard
    AdminSection.USERS -> Icons.Filled.People
    AdminSection.SESSIONS -> Icons.Filled.Timer
    AdminSection.SUBJECTS -> Icons.Filled.Book
    AdminSection.FRIENDSHIPS -> Icons.Filled.Diversity3
    AdminSection.FRIEND_REQUESTS -> Icons.Filled.PersonAdd
    AdminSection.REPORTS -> Icons.Filled.Report
    AdminSection.ANNOUNCEMENTS -> Icons.Filled.Campaign
    AdminSection.NOTIFICATIONS -> Icons.Filled.Notifications
    AdminSection.CHALLENGES -> Icons.Filled.MilitaryTech
    AdminSection.ANALYTICS -> Icons.Filled.Analytics
    AdminSection.SYSTEM_HEALTH -> Icons.Filled.HealthAndSafety
    AdminSection.ERROR_LOGS -> Icons.Filled.BugReport
    AdminSection.AUDIT_LOGS -> Icons.Filled.HistoryEdu
    AdminSection.ADMIN_MANAGEMENT -> Icons.Filled.AdminPanelSettings
    AdminSection.APP_SETTINGS -> Icons.Filled.Settings
    AdminSection.PROFILE -> Icons.Filled.AccountCircle
    AdminSection.EXPORT -> Icons.Filled.Download
}

@Composable
private fun LoadingOverlay(loadingText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
