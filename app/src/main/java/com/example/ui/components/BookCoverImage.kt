package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.BookType
import com.example.data.util.PdfCoverExtractor
import com.example.ui.theme.*
import java.io.File

/**
 * High-craft Book Cover Composable displaying the PDF First Page or styled subject art.
 * Features realistic 3D book spine shadow, rounded corners, Base64 decoding,
 * cross-device sync resilience, and smooth error fallback.
 */
@Composable
fun BookCoverImage(
    coverImage: String,
    title: String,
    subject: String,
    bookType: BookType,
    modifier: Modifier = Modifier,
    fileLink: String = "",
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 4.dp,
    showSpineShadow: Boolean = true
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)

    val resolvedModel: Any? = remember(coverImage, fileLink) {
        val trimmed = coverImage.trim()
        when {
            trimmed.startsWith("data:image", ignoreCase = true) -> {
                val base64Part = trimmed.substringAfter("base64,")
                try {
                    android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            }
            trimmed.startsWith("base64:", ignoreCase = true) -> {
                val base64Part = trimmed.removePrefix("base64:")
                try {
                    android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            }
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                trimmed
            }
            trimmed.startsWith("/data/") -> {
                val localFile = File(trimmed)
                if (localFile.exists() && localFile.length() > 100) {
                    localFile
                } else {
                    // Local file from another device: recover via Google Drive if available
                    val driveId = PdfCoverExtractor.extractGoogleDriveFileId(fileLink)
                    if (driveId != null) {
                        PdfCoverExtractor.getDriveThumbnailUrl(driveId)
                    } else {
                        null
                    }
                }
            }
            trimmed.isNotBlank() -> {
                trimmed
            }
            else -> {
                // Empty coverImage: auto-resolve from fileLink if Google Drive
                val driveId = PdfCoverExtractor.extractGoogleDriveFileId(fileLink)
                if (driveId != null) {
                    PdfCoverExtractor.getDriveThumbnailUrl(driveId)
                } else {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape = shape, spotColor = Slate400.copy(alpha = 0.5f))
            .clip(shape)
            .background(Slate100)
            .border(1.dp, Slate200, shape)
    ) {
        if (resolvedModel != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedModel)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    BookCoverFallback(
                        subject = subject,
                        bookType = bookType,
                        isLoading = true
                    )
                },
                error = {
                    BookCoverFallback(
                        subject = subject,
                        bookType = bookType,
                        isLoading = false
                    )
                }
            )
        } else {
            BookCoverFallback(
                subject = subject,
                bookType = bookType,
                isLoading = false
            )
        }

        // Realistic Left Spine Lighting / Shadow effect
        if (showSpineShadow) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun BookCoverFallback(
    subject: String,
    bookType: BookType,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val badgeColor = bookType.badgeColor

    val subjectIcon = when (subject.lowercase()) {
        "mathematics", "maths" -> Icons.Default.Calculate
        "physics" -> Icons.Default.Bolt
        "chemistry" -> Icons.Default.Science
        "biology" -> Icons.Default.Spa
        "computer science", "computer" -> Icons.Default.Computer
        "english" -> Icons.Default.Translate
        "urdu" -> Icons.Default.MenuBook
        "sindhi" -> Icons.Default.MenuBook
        "pakistan studies" -> Icons.Default.Public
        "general knowledge" -> Icons.Default.Lightbulb
        "health care", "first aid" -> Icons.Default.HealthAndSafety
        else -> Icons.Default.AutoStories
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.18f),
                        badgeColor.copy(alpha = 0.08f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subjectIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLoading) "Loading..." else subject.take(9),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
