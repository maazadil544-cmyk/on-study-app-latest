package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookmarkEntity
import com.example.data.model.BookType
import com.example.ui.components.AppTopBar
import com.example.ui.components.BookCoverImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.UiMessage

@Composable
fun BookmarksScreen(viewModel: StudyViewModel) {
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()

    var selectedBookmarkForPreview by remember { mutableStateOf<BookmarkEntity?>(null) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "Saved Bookmarks",
                subtitle = "${bookmarks.size} Saved Materials",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        },
        bottomBar = {
            GeometricBottomNavigation(viewModel = viewModel, currentScreen = Screen.BOOKMARKS)
        }
    ) { innerPadding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF08A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = Color(0xFFCA8A04),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Bookmarks Saved",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the bookmark icon on any guide or chapter while studying to quick-save it here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    val matchedBook = allBooks.firstOrNull { it.id == bookmark.bookId }

                    Surface(
                        onClick = {
                            selectedBookmarkForPreview = bookmark
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Slate200)
                            .testTag("bookmark_item_${bookmark.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (matchedBook != null) {
                                BookCoverImage(
                                    coverImage = matchedBook.coverImage,
                                    title = matchedBook.title,
                                    subject = matchedBook.subject,
                                    bookType = BookType.fromString(matchedBook.bookType),
                                    fileLink = matchedBook.fileLink,
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(58.dp),
                                    cornerRadius = 8.dp,
                                    elevation = 2.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEF9C3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = Color(0xFFCA8A04),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.bookTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (matchedBook != null && matchedBook.publisher.isNotBlank()) {
                                    Text(
                                        text = "Publisher: ${matchedBook.publisher}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate700,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                }
                                Text(
                                    text = "${bookmark.provinceCode.uppercase()} • ${if (bookmark.classLevel == 0) "Grammar" else "Class ${bookmark.classLevel}"} • Page ${bookmark.pageNumber}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Emerald600,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bookmark.chapterTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate600,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (bookmark.noteSnippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${bookmark.noteSnippet}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate500,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.deleteBookmark(bookmark.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Slate100)
                                    .testTag("delete_bookmark_${bookmark.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete bookmark",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bookmark Saved Message Preview Dialog (shown before redirecting to specific page)
    selectedBookmarkForPreview?.let { bookmark ->
        val matchedBook = allBooks.firstOrNull { it.id == bookmark.bookId }

        AlertDialog(
            onDismissRequest = { selectedBookmarkForPreview = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved Bookmark",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${bookmark.pageNumber} • ${bookmark.provinceCode.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Emerald700,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Book Summary Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (matchedBook != null) {
                                BookCoverImage(
                                    coverImage = matchedBook.coverImage,
                                    title = matchedBook.title,
                                    subject = matchedBook.subject,
                                    bookType = BookType.fromString(matchedBook.bookType),
                                    fileLink = matchedBook.fileLink,
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(56.dp),
                                    cornerRadius = 8.dp,
                                    elevation = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.bookTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (bookmark.classLevel == 0) "Grammar & Composition" else "Class ${bookmark.classLevel}"} • ${bookmark.subject}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate600,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald100
                                ) {
                                    Text(
                                        text = bookmark.chapterTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Emerald800,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Saved Note / Message Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (bookmark.noteSnippet.isNotBlank()) Color(0xFFFFFBEB) else Slate50,
                        border = BorderStroke(
                            1.dp,
                            if (bookmark.noteSnippet.isNotBlank()) Color(0xFFFDE68A) else Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = null,
                                    tint = if (bookmark.noteSnippet.isNotBlank()) Color(0xFFD97706) else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Saved Message",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (bookmark.noteSnippet.isNotBlank()) Color(0xFF92400E) else Slate700
                                    )
                                )
                            }

                            Text(
                                text = if (bookmark.noteSnippet.isNotBlank()) {
                                    bookmark.noteSnippet
                                } else {
                                    "No custom message added. Saved at Page ${bookmark.pageNumber} (${bookmark.chapterTitle})."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (bookmark.noteSnippet.isNotBlank()) Slate900 else Slate600,
                                    lineHeight = 20.sp,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedBookmarkForPreview = null
                        if (matchedBook != null) {
                            viewModel.openReader(matchedBook, bookmark.pageNumber)
                        } else {
                            viewModel.uiMessage.tryEmit(
                                UiMessage("Book '${bookmark.bookTitle}' is no longer in the catalog.", isError = true)
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("bookmark_preview_open_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open (Page ${bookmark.pageNumber})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedBookmarkForPreview = null },
                    modifier = Modifier.testTag("bookmark_preview_close_button")
                ) {
                    Text("Close", color = Slate600, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

