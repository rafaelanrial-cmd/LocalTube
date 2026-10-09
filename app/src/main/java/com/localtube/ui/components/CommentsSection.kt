package com.localtube.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localtube.R
import com.localtube.ui.screens.CommentNode
import com.localtube.util.Formats

/**
 * Блок «Комментарии» на экране деталей (Этап 8, 2026-10-08): раскладка как на
 * YouTube — счётчик сверху, закреплённый первым, ответы отступом под родителем.
 * Ответы: первые [REPLY_INLINE_LIMIT] показаны сразу, остальные — за кнопкой
 * «Показать ещё N» (экран деталей не lazy — так список остаётся лёгким).
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
    Column(modifier = modifier.fillMaxWidth()) {
        // Счётчик «N комментариев» — заголовок секции (как на YouTube).
        val total = comments.totalWithReplies()
        Text(
            text = stringResource(R.string.comments_count, total, stringResource(commentWordRes(total))),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            comments.forEach { CommentNodeView(node = it, depth = 0) }
        }
    }
}

/** Топовый комментарий + его ответы (рекурсия); отступ — по глубине. */
@Composable
private fun CommentNodeView(node: CommentNode, depth: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 52).dp), // как на YouTube: ширина аватара + зазор
    ) {
        CommentItem(comment = node.comment)
        if (node.replies.isEmpty()) return
        var showAll by remember { mutableStateOf(false) }
        val shown = if (showAll) node.replies else node.replies.take(REPLY_INLINE_LIMIT)
        Column(modifier = Modifier.padding(top = 10.dp)) {
            shown.forEachIndexed { index, reply ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                CommentNodeView(reply, depth + 1)
            }
            if (!showAll && node.replies.size > REPLY_INLINE_LIMIT) {
                TextButton(onClick = { showAll = true }) {
                    val hidden = node.replies.size - REPLY_INLINE_LIMIT
                    Text(
                        stringResource(R.string.comments_more_replies, hidden, stringResource(replyWordRes(hidden))),
                    )
                }
            }
        }
    }
}

/** Сколько ответов видно до кнопки «Показать ещё». */
private const val REPLY_INLINE_LIMIT = 3

/** Всего комментариев в дереве: топовые + все ответы. */
private fun List<CommentNode>.totalWithReplies(): Int = sumOf { it.total() }

private fun CommentNode.total(): Int = 1 + replies.sumOf { it.total() }

/** Русские формы слова «комментарий». */
private fun commentWordRes(count: Int): Int = Formats.pluralWordRes(
    count, R.string.comments_word_comment_one, R.string.comments_word_comment_few, R.string.comments_word_comment_many,
)

/** Русские формы слова «ответ» для кнопки «Показать ещё N …». */
private fun replyWordRes(count: Int): Int = Formats.pluralWordRes(
    count, R.string.comments_word_one, R.string.comments_word_few, R.string.comments_word_many,
)
