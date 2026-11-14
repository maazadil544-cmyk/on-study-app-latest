package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private data class StarterPromptItem(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistIqScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isChatbotThinking.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingAiReply.collectAsStateWithLifecycle()
    val isDetailedMode by viewModel.assistIqDetailedMode.collectAsStateWithLifecycle()
    val pendingPrompt by viewModel.pendingChatPrompt.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showModeDropdown by remember { mutableStateOf(false) }
    var showQuickPromptsDrawer by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Auto-refresh Gemini API key from Firebase Cloud when opening AssistIQ
    LaunchedEffect(Unit) {
        viewModel.refreshGeminiApiKeyFromCloud()
    }

    // Handle any prompt passed during navigation (e.g. from Reader or Home card)
    LaunchedEffect(pendingPrompt) {
        pendingPrompt?.let { prompt ->
            if (prompt.isNotBlank()) {
                inputText = prompt
                viewModel.pendingChatPrompt.value = null
            }
        }
    }

    // Filter out the initial legacy static welcome message when in clean Hero mode
    val conversationMessages = remember(messages) {
        val hasUserMsg = messages.any { it.isUser }
        if (!hasUserMsg && messages.size <= 1 && messages.firstOrNull()?.text?.contains("AssistIQ online hai") == true) {
            emptyList()
        } else {
            messages
        }
    }

    val isInConversation = conversationMessages.isNotEmpty() || isThinking || !streamingReply.isNullOrEmpty()

    // Smooth auto-scroll on new message or while streaming grows
    LaunchedEffect(conversationMessages.size, isThinking, streamingReply?.length ?: 0) {
        if (isInConversation) {
            val totalItems = conversationMessages.size + if (isThinking || streamingReply != null) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    val quickTopicChips = remember {
        listOf(
            "📐 Math Formulas" to "Mathematics 10th/12th ke important formulas step-by-step likhein.",
            "⚡ Physics Numericals" to "Physics numericals solve karne ka systematic method samjhao.",
            "🧪 Chemistry Reactions" to "Important chemical reactions aur balancing ka asaan tareeqa batayein.",
            "🇵🇰 Pak Studies" to "Pakistan Studies ke important questions ke structured points dein.",
            "📝 English Essay" to "Matric/Inter English essay ke liye comprehensive outline aur quotations dein.",
            "🧬 Biology Concepts" to "Biology ke main diagrams aur processes ki clear summary dein.",
            "💻 CS Logic" to "Computer Science programming concepts aur flowchart logic samjhao.",
            "🎯 Study Timetable" to "Rozana 3 hours ka high-efficiency revision time-table banao."
        )
    }

    val starterCards = remember {
        listOf(
            StarterPromptItem(
                title = "Solve a Math or Physics problem step-by-step",
                subtitle = "Formulas, derivations & numerical breakdowns",
                prompt = "Newton's Second Law of Motion aur Quadratic Formula step-by-step samjhao.",
                icon = Icons.Outlined.Calculate
            ),
            StarterPromptItem(
                title = "Prepare a board exam English Essay outline",
                subtitle = "High-scoring headings & quotations",
                prompt = "Matric/Inter board exam ke liye 'Role of Technology in Education' essay ki outline aur key quotations likhein.",
                icon = Icons.Outlined.HistoryEdu
            ),
            StarterPromptItem(
                title = "Summarize a Science or Pak Studies chapter",
                subtitle = "Quick revision bullet points for tests",
                prompt = "Photosynthesis aur Respiration ka farq easy comparison table mein samjhao.",
                icon = Icons.Outlined.Science
            ),
            StarterPromptItem(
                title = "Create a smart daily board study timetable",
                subtitle = "Balanced schedule for all subjects",
                prompt = "Matric/FSc exams ki tayyari ke liye 4 ghantay ka smart daily study timetable banao.",
                icon = Icons.Outlined.Timeline
            )
        )
    }

    val submitMessage: (String) -> Unit = { rawQuery ->
        val trimmed = rawQuery.trim()
        if (trimmed.isNotBlank() && !isThinking) {
            inputText = ""
            showQuickPromptsDrawer = false
            keyboardController?.hide()
            viewModel.sendChatMessage(trimmed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isInConversation) {
                        listOf(Color.White, Color.White, Color(0xFFF8FAFC))
                    } else {
                        listOf(Color.White, Color(0xFFFAFDFC), Emerald50.copy(alpha = 0.85f))
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AssistIqMinimalTopBar(
                    isDetailedMode = isDetailedMode,
                    showModeDropdown = showModeDropdown,
                    hasMessages = isInConversation,
                    onToggleModeDropdown = { showModeDropdown = !showModeDropdown },
                    onDismissModeDropdown = { showModeDropdown = false },
                    onSelectMode = { detailed ->
                        viewModel.setAssistIqDetailedMode(detailed)
                        showModeDropdown = false
                    },
                    onBack = { viewModel.navigateBack() },
                    onNewChat = {
                        if (isInConversation) {
                            showClearConfirmDialog = true
                        } else {
                            Toast.makeText(context, "Already on a fresh chat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenInfo = { showInfoDialog = true }
                )
            },
            bottomBar = {
                AssistIqFloatingInputDock(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    isThinking = isThinking,
                    showQuickPrompts = showQuickPromptsDrawer,
                    onToggleQuickPrompts = { showQuickPromptsDrawer = !showQuickPromptsDrawer },
                    quickTopics = quickTopicChips,
                    onQuickTopicClick = { prompt ->
                        inputText = prompt
                        showQuickPromptsDrawer = false
                    },
                    onSend = { submitMessage(inputText) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                ) {
                    if (!isInConversation) {
                        AssistIqHeroEmptyState(
                            starterCards = starterCards,
                            onPromptSelected = { prompt -> submitMessage(prompt) }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
                        ) {
                            itemsIndexed(
                                items = conversationMessages,
                                key = { _, msg -> msg.id }
                            ) { index, message ->
                                val isLatestAi = !message.isUser &&
                                    index == conversationMessages.lastIndex &&
                                    !isThinking &&
                                    streamingReply == null

                                GeminiStyleChatRow(
                                    message = message,
                                    showDisclaimer = isLatestAi && message.status != "ERROR",
                                    onCopyText = { text ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AssistIQ Solution", text))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    onShareText = { text ->
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "AssistIQ Study Solution")
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Solution"))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Unable to open share menu", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSaveAsNote = { text ->
                                        scope.launch {
                                            val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: "AssistIQ Solution"
                                            val cleanTitle = firstLine.replace("#", "").replace("*", "").trim()
                                            val title = if (cleanTitle.length > 42) cleanTitle.take(39) + "..." else cleanTitle
                                            viewModel.saveNote(
                                                NoteEntity(
                                                    title = title,
                                                    content = text,
                                                    subject = "AssistIQ AI",
                                                    colorIndex = 0
                                                )
                                            )
                                            Toast.makeText(context, "Saved to Study Notes!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onRetry = {
                                        val lastUserMsg = conversationMessages.lastOrNull { it.isUser }
                                        if (lastUserMsg != null) {
                                            viewModel.retryAssistIqPrompt(lastUserMsg.text)
                                        }
                                    }
                                )
                            }

                            // Live Streaming AI Response or Instant Thinking Indicator
                            if (isThinking || streamingReply != null) {
                                item(key = "streaming_ai_item") {
                                    AssistIqStreamingMessageItem(
                                        partialText = streamingReply.orEmpty(),
                                        isDetailedMode = isDetailedMode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear / New Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Start a New Chat?",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "Kya aap purani conversation clear karke ek fresh AssistIQ chat shuru karna chahtay hain?",
                    color = Slate600,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearChatHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("New Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Emerald600, Color(0xFF0284C7)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AssistIQ Study AI",
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Fast, clean & built for Pakistani students.",
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald700,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Fast Mode: Instant concise answers & formulas without waiting.",
                        color = Slate700,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Step-by-Step Mode: Complete derivations, essays & board exam explanations.",
                        color = Slate700,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Supports Punjab, Sindh, KPK, Balochistan & FBISE syllabi in Roman Urdu & English.",
                        color = Slate700,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Got it")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AssistIqMinimalTopBar(
    isDetailedMode: Boolean,
    showModeDropdown: Boolean,
    hasMessages: Boolean,
    onToggleModeDropdown: () -> Unit,
    onDismissModeDropdown: () -> Unit,
    onSelectMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpenInfo: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.94f),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Slate100.copy(alpha = 0.85f))
                    .testTag("assistiq_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Slate800,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Gemini-inspired Title + Mode Switcher Pill
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggleModeDropdown)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AssistIQ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDetailedMode) "Detailed" else "Fast",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Emerald700,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select AI speed mode",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModeDropdown,
                    onDismissRequest = onDismissModeDropdown,
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "Fast",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Instant answers for quick doubts",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (!isDetailedMode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Emerald600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = { onSelectMode(false) }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "Step-by-Step Detailed",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Deep explanations, numericals & outlines",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (isDetailedMode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Emerald600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = { onSelectMode(true) }
                    )
                }
            }

            // New Chat Button (matches Gemini Compose / EditNote icon)
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("assistiq_clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = "New Chat",
                    tint = if (hasMessages) Slate800 else Slate400,
                    modifier = Modifier.size(22.dp)
                )
            }

            // About / Info Button
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("assistiq_info_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About AssistIQ",
                    tint = Slate600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistIqHeroEmptyState(
    starterCards: List<StarterPromptItem>,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Centered Gemini-inspired Welcome Sparkle + Prompt Headline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Emerald500, Color(0xFF0284C7))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AssistIQ Sparkle",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Aaj kya parhna hai?",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    fontSize = 26.sp,
                    letterSpacing = (-0.4).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ask any board question, numerical, essay, or concept",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate500,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // "Jump into something:" Starter Cards section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Jump into something:",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Slate800,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            starterCards.forEach { card ->
                Surface(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200.copy(alpha = 0.9f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onPromptSelected(card.prompt) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Emerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = card.icon,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.title,
                                color = Slate900,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = card.subtitle,
                                color = Slate500,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ask",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiStyleChatRow(
    message: ChatMessageEntity,
    showDisclaimer: Boolean,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.isUser
    val isError = !isUser && message.status == "ERROR"
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    val formattedAiText = remember(message.text) { formatAssistIqMarkdown(message.text) }

    if (isUser) {
        // Right-aligned light soft pill matching Gemini mobile style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 6.dp
                ),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.text,
                    color = Slate900,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                )
            }
        }
    } else {
        // Unboxed clean editorial AI response
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                            else Brush.linearGradient(listOf(Emerald500, Color(0xFF0284C7)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "AssistIQ",
                    color = if (isError) Color(0xFFDC2626) else Slate800,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Text(
                    text = "• $formattedTime",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            if (isError) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = message.text,
                            color = Color(0xFF991B1B),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry with Cloud Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = formattedAiText,
                    color = Slate800,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                )

                // Subtle Gemini-style action bar (Share, Copy, Save as Note)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onCopyText(message.text) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy response",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSaveAsNote(message.text) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = "Save to Notes",
                            tint = Slate500,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { onShareText(message.text) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share solution",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showDisclaimer) {
                    Text(
                        text = "AssistIQ is AI and can make mistakes. Verify exam formulas.",
                        color = Slate400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistIqStreamingMessageItem(
    partialText: String,
    isDetailedMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StreamingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Emerald500, Color(0xFF0284C7)))
                    )
                    .alpha(pulseAlpha),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = if (partialText.isBlank()) {
                    if (isDetailedMode) "Preparing detailed explanation..." else "Thinking..."
                } else {
                    "AssistIQ"
                },
                color = Slate600,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (partialText.isNotBlank()) {
            val formattedPartial = remember(partialText) { formatAssistIqMarkdown(partialText) }
            Text(
                text = formattedPartial,
                color = Slate800,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(start = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun AssistIqFloatingInputDock(
    inputText: String,
    onInputChange: (String) -> Unit,
    isThinking: Boolean,
    showQuickPrompts: Boolean,
    onToggleQuickPrompts: () -> Unit,
    quickTopics: List<Pair<String, String>>,
    onQuickTopicClick: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = inputText.isNotBlank() && !isThinking

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.92f),
                        Color.White
                    )
                )
            )
            .imePadding()
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Collapsible Quick Topics Strip
        AnimatedVisibility(
            visible = showQuickPrompts,
            enter = fadeIn(tween(180)) + expandVertically(spring()),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(160))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTopics.forEach { (label, prompt) ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Slate200),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onQuickTopicClick(prompt) }
                    ) {
                        Text(
                            text = label,
                            color = Slate800,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Floating Pill Input Box (matching Gemini bottom bar)
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onToggleQuickPrompts,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = if (showQuickPrompts) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Quick Study Topics",
                        tint = Slate700,
                        modifier = Modifier.size(22.dp)
                    )
                }

                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = "Ask AssistIQ...",
                            color = Slate400,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistiq_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Emerald600
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSend()
                        }
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isThinking -> Emerald50
                                canSend -> Emerald600
                                else -> Slate100
                            }
                        )
                        .testTag("assistiq_send_button")
                ) {
                    if (isThinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Emerald600,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight Markdown-to-AnnotatedString formatter for clean AI replies:
 * - Converts leading `### ` / `## ` headings to bold dark text
 * - Converts `- ` or `* ` bullets to clean `• `
 * - Parses **bold**, *italic*, and `code` spans cleanly without raw asterisks
 */
private fun formatAssistIqMarkdown(raw: String): AnnotatedString {
    val lines = raw.lines()
    return buildAnnotatedString {
        lines.forEachIndexed { lineIdx, originalLine ->
            var line = originalLine
            var isHeading = false

            val trimmedStart = line.trimStart()
            when {
                trimmedStart.startsWith("### ") -> {
                    line = trimmedStart.removePrefix("### ")
                    isHeading = true
                }
                trimmedStart.startsWith("## ") -> {
                    line = trimmedStart.removePrefix("## ")
                    isHeading = true
                }
                trimmedStart.startsWith("# ") -> {
                    line = trimmedStart.removePrefix("# ")
                    isHeading = true
                }
                trimmedStart.startsWith("- ") -> {
                    val indent = line.length - trimmedStart.length
                    line = " ".repeat(indent) + "• " + trimmedStart.removePrefix("- ")
                }
                trimmedStart.startsWith("* ") -> {
                    val indent = line.length - trimmedStart.length
                    line = " ".repeat(indent) + "• " + trimmedStart.removePrefix("* ")
                }
            }

            if (isHeading) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp))
                appendInlineMarkdown(line.replace("**", ""))
                pop()
            } else {
                appendInlineMarkdown(line)
            }

            if (lineIdx < lines.lastIndex) {
                append('\n')
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var i = 0
    val len = text.length
    while (i < len) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Slate100,
                            color = Emerald800
                        )
                    )
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '*' && (i + 1 < len && text[i + 1] != ' ') -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
