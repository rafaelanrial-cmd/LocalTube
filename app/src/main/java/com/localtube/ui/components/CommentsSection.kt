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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localtube.R
import com.localtube.ui.screens.CommentNode
import com.localtube.util.Formats

/**
 * Блок «Комментарии» на экране деталей (Этап 8, 2026-10-08; ответы по тапу —
 * 2026-10-09): раскладка как на YouTube — счётчик сверху, закреплённый первым.
 * Ответы СКРЫТЫ: под топовым комментарием кнопка «N ответов», тап раскрывает
 * тред отступом под текстом родителя; вложенные ответы внутри треда — первые
 * [REPLY_INLINE_LIMIT] + «Показать ещё N». Отступ ограничен, чтобы глубокие
 * треды не уезжали за правый край (жалоба пользователя 2026-10-09).
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
        // Этап 8 (2026-10-09): скачиваем ВСЕ комментарии (лимит снят), поэтому
        // топовых может быть тысячи. Список не-lazy: композим видимую порцию и
        // раскрываем дальше по кнопке — иначе все элементы разом уйдут в память.
        var visibleTop by remember { mutableStateOf(TOP_INITIAL) }
        val shownTop = comments.take(visibleTop)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            shownTop.forEach { CommentNodeView(node = it, depth = 0) }
            if (comments.size > visibleTop) {
                TextButton(onClick = { visibleTop += TOP_STEP }) {
                    Text(stringResource(R.string.comments_show_more))
                }
            }
        }
    }
}

/** Комментарий + его ответы; отступ — по [indentFor] (ограничен сверху). */
@Composable
private fun CommentNodeView(node: CommentNode, depth: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentFor(depth)), // как на YouTube: под текстом родителя
    ) {
        CommentItem(comment = node.comment)
        if (node.replies.isEmpty()) return
        if (depth == 0) {
            // Топовый уровень (2026-10-09): ответы СКРЫТЫ, как на YouTube —
            // кнопка «N ответов», тап раскрывает/скрывает тред.
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) {
                        stringResource(R.string.comments_hide_replies)
                    } else {
                        stringResource(
                            R.string.comments_reply_count,
                            node.replies.size,
                            stringResource(replyWordRes(node.replies.size)),
                        )
                    },
                )
            }
            if (expanded) ReplyList(replies = node.replies)
        } else {
            // Ответ внутри раскрытого треда: первые [REPLY_INLINE_LIMIT] сразу,
            // остальные — за «Показать ещё N» (тред может быть длинным).
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
}

/** Прямые ответы раскрытого треда (отступ уже даёт CommentNodeView depth=1). */
@Composable
private fun ReplyList(replies: List<CommentNode>) {
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        replies.forEach { CommentNodeView(it, depth = 1) }
    }
}

/** Сколько ответов видно до кнопки «Показать ещё» внутри раскрытого треда. */
private const val REPLY_INLINE_LIMIT = 3

/** Отступ как на YouTube: прямой ответ под текстом родителя (аватар + зазор). */
private const val REPLY_BASE_INDENT = 52

/** Шаг для вложенных уровней внутри треда и его потолок — глубокие треды не
 *  должны уезжать за правый край (max отступ: 52 + 4*16 = 116 dp). */
private const val NEST_STEP = 16
private const val MAX_NEST_STEPS = 4

/** Отступ по глубине: 0 — топовый; дальше базовый + шаг за уровень, с потолком. */
private fun indentFor(depth: Int): Dp = when {
    depth <= 0 -> 0.dp
    else -> (REPLY_BASE_INDENT + minOf(depth - 1, MAX_NEST_STEPS) * NEST_STEP).dp
}

/** Этап 8 (2026-10-09): сколько топовых комментариев видно изначально и по сколько
 *  добавляет кнопка «Показать ещё комментарии» (список не-lazy — см. выше). */
private const val TOP_INITIAL = 50
private const val TOP_STEP = 100

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
