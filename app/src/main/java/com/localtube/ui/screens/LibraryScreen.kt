package com.localtube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.localtube.R
import com.localtube.ui.components.VideoCard
import com.localtube.ui.navigation.Routes

/** Экран «Библиотека» (Этап 3): список скачанных видео. */
@Composable
fun LibraryScreen(
    navController: NavHostController,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory()),
) {
    val videos by viewModel.videos.collectAsState()

    if (videos.isEmpty()) {
        EmptyLibraryContent(
            onGoToAdd = { navController.navigate(Routes.ADD) },
            onAddTestVideo = viewModel::addTestVideo, // TODO(Этап 7): УДАЛИТЬ
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    onClick = {
                        navController.navigate(Routes.details(video.id.toLong()))
                    },
                )
            }
        }
    }
}

/** Пустое состояние: сообщение + кнопка на экран добавления. */
@Composable
private fun EmptyLibraryContent(
    onGoToAdd: () -> Unit,
    onAddTestVideo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_library),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onGoToAdd, modifier = Modifier.padding(top = 16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.library_add_video))
        }
        // TODO(Этап 7): УДАЛИТЬ — временная кнопка тестового видео.
        OutlinedButton(
            onClick = onAddTestVideo,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.library_add_test_video))
        }
    }
}
