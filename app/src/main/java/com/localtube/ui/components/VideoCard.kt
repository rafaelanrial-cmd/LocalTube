package com.localtube.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localtube.data.db.VideoEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Карточка видео в библиотеке (Этап 3).
 * Превью из файла через Coil, название, длительность мм:сс,
 * дата публикации dd.MM.yyyy и бейдж «RU», если есть русская дорожка.
 */
@Composable
fun VideoCard(
    video: VideoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                Thumbnail(path = video.thumbnailPath)
                if (video.hasRuTrack) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "RU", // код языка, не переводится
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = metaLine(video)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Строка метаданных: «мм:сс · dd.MM.yyyy» (пустые части не показываются). */
private fun metaLine(video: VideoEntity): String = buildList {
    if (video.durationSec > 0) add(formatDuration(video.durationSec))
    if (video.uploadDateEpoch > 0) add(formatDate(video.uploadDateEpoch))
}.joinToString(" · ")

/** Секунды → «мм:сс» (если больше часа — «ч:мм:сс»). */
fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

/** Байты → «X,X МБ» (одна цифра после запятой; разделитель — запятая). */
fun formatSizeMb(bytes: Long): String =
    "%.1f".format(bytes / 1_048_576.0).replace('.', ',')

/** Секунды epoch → «dd.MM.yyyy» в часовом поясе устройства. */
fun formatDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
