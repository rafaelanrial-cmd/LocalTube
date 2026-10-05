package com.localtube.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Превью видео из файла (Coil) — общий для карточки библиотеки и экрана деталей.
 * Если файла нет — заглушка с иконкой.
 */
@Composable
fun Thumbnail(path: String, modifier: Modifier = Modifier) {
    val file = path.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
    if (file != null) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
