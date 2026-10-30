package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun NewsScreen(viewModel: StudyViewModel) {
    val newsList by viewModel.allNews.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingWithFirebase.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNewsCount.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Alerts", "System Notify", "Students Notify", "Others")
    val filteredList = remember(newsList, selectedFilter) {
        if (selectedFilter == "All") newsList
        else newsList.filter {
            val type = it.notificationType.ifBlank { it.category }
            type.equals(selectedFilter, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "Notifications & Updates",
                subtitle = if (unreadCount > 0) "$unreadCount New Notifications" else "Official Announcements & Alerts",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        },
        bottomBar = {
            GeometricBottomNavigation(viewModel = viewModel, currentScreen = Screen.NEWS)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            // Header Action Banner: Online Sync & Mark All Read
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isOnline) Color.White else Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOnline) Slate200 else Color(0xFFFDE68A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Emerald600 else Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) {
                                    if (isSyncing) "Syncing with cloud..." else "Live Cloud Sync Active"
                                } else {
                                    "Offline: Viewing saved updates"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isOnline) Slate700 else Color(0xFF92400E)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadCount > 0) {
                                TextButton(
                                    onClick = { viewModel.markAllNewsAsRead() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("mark_all_read_button")
                                ) {
                                    Text(
                                        text = "Mark read",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald600,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            if (isOnline) {
                                IconButton(
                                    onClick = { viewModel.syncWithFirebase() },
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("sync_news_button")
                                        .background(Slate100, CircleShape)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Emerald600
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Sync",
                                            tint = Emerald600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notification Type Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { option ->
                        val isSelected = selectedFilter == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = option },
                            label = {
                                Text(
                                    text = option,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald600,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate800
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Emerald600 else Slate300
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Campaign,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedFilter == "All") "No Notifications Yet" else "No $selectedFilter Notifications",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Important announcements and updates will appear here.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate400,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val notifType = item.notificationType.ifBlank { item.category }
                    val (categoryColor, categoryBg, categoryIcon) = when (notifType) {
                        "System Notify" -> Triple(Color(0xFF7C3AED), Color(0xFFF3E8FF), Icons.Default.Dns)
                        "Alerts" -> Triple(Color(0xFFDC2626), Color(0xFFFEE2E2), Icons.Default.WarningAmber)
                        "Others" -> Triple(Color(0xFF2563EB), Color(0xFFDBEAFE), Icons.Default.Campaign)
                        else -> Triple(Emerald700, Emerald100, Icons.Default.School) // Students Notify
                    }

                    Surface(
                        onClick = { viewModel.markNewsAsRead(item.id) },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = if (item.isUnread) 4.dp else 1.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            if (item.isUnread) 1.5.dp else 1.dp,
                            if (item.isUnread) categoryColor.copy(alpha = 0.5f) else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("news_card_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = categoryBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon,
                                            contentDescription = null,
                                            tint = categoryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = notifType,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = categoryColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.isUnread) {
                                        Surface(
                                            color = Color(0xFFEF4444),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "NEW",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(
                                        text = item.date,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Slate900,
                                    letterSpacing = (-0.3).sp
                                )
                            )

                            if (item.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Slate600,
                                        lineHeight = 22.sp,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            if (item.isUnread) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Emerald50,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Tap to mark read",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Emerald700,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

