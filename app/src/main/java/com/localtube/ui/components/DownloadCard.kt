package com.localtube.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localtube.R
import com.localtube.data.db.DownloadStatus
import com.localtube.data.db.DownloadTaskRow
import com.localtube.util.Formats

/**
 * Карточка задачи скачивания (Этап 7): название, полоса прогресса, проценты,
 * скорость, ETA, чек-лист (метаданные / превью / видео / комментарии), «Отмена».
 */
@Composable
fun DownloadCard(
    task: DownloadTaskRow,
    onCancel: ((Int) -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            when (task.status) {
                DownloadStatus.QUEUED -> Text(
                    text = stringResource(R.string.dl_queued),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                DownloadStatus.RUNNING -> {
                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${task.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (task.speedBps > 0L) {
                            Text(
                                text = stringResource(R.string.dl_speed, Formats.speedMb(task.speedBps)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (task.etaSec > 0L) {
                            val total = task.etaSec.toInt()
                            Text(
                                text = if (total < 60) {
                                    stringResource(R.string.dl_eta_sec, total)
                                } else {
                                    stringResource(R.string.dl_eta_min, total / 60, total % 60)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                DownloadStatus.FAILED -> Text(
                    text = task.errorMsg ?: stringResource(R.string.dl_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                DownloadStatus.CANCELED -> Text(
                    text = stringResource(R.string.dl_canceled),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                DownloadStatus.DONE -> Unit
            }
            ChecklistRow(task)
            if (onCancel != null &&
                (task.status == DownloadStatus.QUEUED || task.status == DownloadStatus.RUNNING)
            ) {
                TextButton(
                    onClick = { onCancel(task.id) },
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(stringResource(R.string.dl_cancel))
                }
            }
        }
    }
}

/** Чек-лист: ☑ метаданные ☑ превью ▶ видео 45% ☐ комментарии. */
@Composable
private fun ChecklistRow(task: DownloadTaskRow) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Метаданные получены при добавлении (Этап 6) — всегда отмечено.
        CheckItem(checked = true, label = stringResource(R.string.dl_meta))
        CheckItem(checked = task.thumbOk, label = stringResource(R.string.dl_thumb))
        VideoCheckItem(task)
        CommentsCheckItem(task)
    }
}

@Composable
private fun CheckItem(checked: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = {}, // индикатор, не кликабельный
            enabled = false,
            modifier = Modifier.size(20.dp),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Видео: ▶ с процентами во время скачивания, ☑ после. */
@Composable
private fun VideoCheckItem(task: DownloadTaskRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (task.status == DownloadStatus.RUNNING) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.dl_video_progress,
                    stringResource(R.string.dl_video),
                    task.progressPercent,
                ),
                style = MaterialTheme.typography.labelSmall,
            )
        } else {
            Checkbox(
                checked = task.videoOk,
                onCheckedChange = {}, // индикатор, не кликабельный
                enabled = false,
                modifier = Modifier.size(20.dp),
            )
            Text(text = stringResource(R.string.dl_video), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Комментарии: скачиваются с Этапа 8; «—», если пользователь не ставил галочку. */
@Composable
private fun CommentsCheckItem(task: DownloadTaskRow) {
    if (!task.withComments) {
        Text(
            text = "—",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.commentsOk,
                onCheckedChange = {}, // индикатор, не кликабельный
                enabled = false,
                modifier = Modifier.size(20.dp),
            )
            Text(text = stringResource(R.string.dl_comments), style = MaterialTheme.typography.labelSmall)
        }
    }
}
