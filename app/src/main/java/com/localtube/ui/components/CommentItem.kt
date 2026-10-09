package com.localtube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localtube.R
import com.localtube.data.db.CommentEntity
import com.localtube.util.Formats
import java.io.File

/**
 * Один комментарий в стиле YouTube (Этап 8, 2026-10-08): круглый аватар слева,
 * имя (+ бейдж «Автор» у владельца видео), относительная дата справа, текст,
 * лайки компактным числом. Закреплённый — с пометкой «Закреплено».
 */
@Composable
fun CommentItem(comment: CommentEntity, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthorAvatar(
            author = comment.author,
            thumbnailPath = comment.authorThumbnailPath,
            size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.authorIsUploader) {
                    Spacer(Modifier.width(6.dp))
                    AuthorBadge()
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Formats.relativeDate(context, comment.publishedAtEpoch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (comment.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 1.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.comments_pinned),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (comment.likeCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formats.compactRu(comment.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Круглый аватар; если файла нет — цветной круг с первой буквой имени. */
@Composable
fun AuthorAvatar(author: String, thumbnailPath: String, size: Dp) {
    val file = if (thumbnailPath.isNotBlank()) File(thumbnailPath) else null
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AvatarPalette.colorFor(author)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = author.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

/** Бейдж «Автор» рядом с именем владельца видео (как на YouTube). */
@Composable
private fun AuthorBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = stringResource(R.string.comments_badge_author),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

/** Палитра фолбэк-аватаров: цвет стабилен по имени. */
private object AvatarPalette {
    private val colors = listOf(
        Color(0xFF7B61C4), Color(0xFFE35D5D), Color(0xFF2F9E8F), Color(0xFF3B7DD8),
        Color(0xFFC98A2B), Color(0xFF9C4D9C), Color(0xFF4CAF50), Color(0xFFD14D8B),
    )

    fun colorFor(name: String): Color =
        colors[(name.hashCode() and Int.MAX_VALUE) % colors.size]
}
