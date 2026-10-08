package com.localtube.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localtube.R
import com.localtube.data.db.CommentEntity
import com.localtube.ui.screens.CommentNode

/**
 * Блок «Комментарии» на экране деталей (Этап 8): топовые комментарии по
 * убыванию лайков, ответы свёрнуты за кнопкой «Показать N ответов» —
 * на каждом уровне вложенности. Каждый ответ помечен строкой
 * «Ответ на @автор» + иконка, чтобы было видно, что это ответ.
 */
@Composable
fun CommentsSection(
    comments: List<CommentNode>,
    modifier: Modifier = Modifier,
) {
    if (comments.isEmpty()) {
        Text(
            text = stringResource(R.string.details_comments_disabled),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(top = 8.dp),
        )
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        comments.forEach { CommentNodeView(node = it, depth = 0) }
    }
}

/**
 * Узел дерева: комментарий + его ответы (рекурсия). Каждый уровень ответов
 * свёрнут за своей кнопкой «Показать N ответов»; отступ — по глубине.
 * [parentAuthor] — автор комментария, на который этот узел отвечает
 * (у топовых null — метка «Ответ на …» не показывается).
 */
@Composable
private fun CommentNodeView(node: CommentNode, depth: Int, parentAuthor: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 20).dp),
    ) {
        CommentBody(comment = node.comment, parentAuthor = parentAuthor)
        if (node.replies.isNotEmpty()) {
            if (!expanded) {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        stringResource(
                            R.string.comments_show_replies,
                            node.replies.size,
                            stringResource(replyWordRes(node.replies.size)),
                        ),
                    )
                }
            } else {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    node.replies.forEach {
                        CommentNodeView(it, depth + 1, parentAuthor = node.comment.author)
                    }
                }
            }
        }
    }
}

/**
 * Один комментарий: (у ответов — метка «Ответ на @автор»), автор + дата,
 * текст, лайки. Ответы без вложенных детей.
 */
@Composable
private fun CommentBody(comment: CommentEntity, parentAuthor: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (parentAuthor != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.comments_reply_to, parentAuthor),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.author,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDate(comment.publishedAtEpoch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = comment.likeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Русские формы слова «ответ» для кнопки «Показать N …». */
private fun replyWordRes(count: Int): Int {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod10 == 1 && mod100 != 11 -> R.string.comments_word_one
        mod10 in 2..4 && mod100 !in 12..14 -> R.string.comments_word_few
        else -> R.string.comments_word_many
    }
}
