package com.localtube.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.localtube.R
import com.localtube.data.downloader.AudioChoice
import com.localtube.ui.components.AddVideoCard
import com.localtube.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Экран «Добавить» (Этап 6): ссылка → проверка метаданных через yt-dlp,
 * опции (аудио, качество, комментарии), кнопка «Скачать» создаёт задачу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    navController: NavHostController,
    // TODO(Этап 12): УДАЛИТЬ — отладочная автопроверка ссылки из файла-флага.
    debugAutoCheckUrl: String? = null,
    viewModel: AddViewModel = viewModel(factory = AddViewModel.factory(debugAutoCheckUrl)),
) {
    val state by viewModel.uiState.collectAsState()
    val urlText by viewModel.urlText.collectAsState()
    val audioChoice by viewModel.audioChoice.collectAsState()
    val qualityHeight by viewModel.qualityHeight.collectAsState()
    val withComments by viewModel.withComments.collectAsState()
    val downloading by viewModel.downloading.collectAsState()
    // TODO(Этап 12): УДАЛИТЬ — автоскачивание: автопереход на «Загрузки».
    val autoDownloaded by viewModel.autoDownloaded.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(autoDownloaded) {
        if (autoDownloaded) {
            navController.navigate(Routes.DOWNLOADS) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Поле ссылки с валидацией (youtube.com/watch, youtu.be).
        OutlinedTextField(
            value = urlText,
            onValueChange = viewModel::onUrlChanged,
            label = { Text(stringResource(R.string.add_url_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = !urlText.isEmpty() && !AddViewModel.isValidYouTubeUrl(urlText.trim()),
            supportingText = if (!urlText.isEmpty() && !AddViewModel.isValidYouTubeUrl(urlText.trim())) {
                { Text(stringResource(R.string.add_invalid_url)) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = viewModel::onCheckClick,
            enabled = AddViewModel.isValidYouTubeUrl(urlText.trim()) && state !is AddUiState.Checking,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            if (state is AddUiState.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.add_check))
        }

        when (val s = state) {
            is AddUiState.Idle -> Unit
            is AddUiState.Checking -> Unit
            is AddUiState.CheckFailed -> Text(
                text = stringResource(R.string.add_check_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            is AddUiState.Checked -> {
                Spacer(Modifier.height(16.dp))
                AddVideoCard(info = s.info)
                AudioSection(
                    selected = audioChoice,
                    hasRu = s.info.hasRuTrack,
                    onSelected = viewModel::onAudioSelected,
                )
                QualitySection(
                    selectedHeight = qualityHeight,
                    onSelected = viewModel::onQualitySelected,
                )
                CommentsRow(
                    checked = withComments,
                    onCheckedChange = viewModel::onCommentsChanged,
                )
                Button(
                    onClick = {
                        scope.launch {
                            if (viewModel.download()) {
                                navController.navigate(Routes.DOWNLOADS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    enabled = !downloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_download))
                }
            }
        }
    }
}

/** Секция «Аудио»: радиокнопки; строка «Русская» видна только если есть ru-дорожка. */
@Composable
private fun AudioSection(
    selected: AudioChoice,
    hasRu: Boolean,
    onSelected: (AudioChoice) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.add_audio), style = MaterialTheme.typography.labelLarge)
        RadioRow(stringResource(R.string.add_audio_original), selected == AudioChoice.ORIGINAL) {
            onSelected(AudioChoice.ORIGINAL)
        }
        if (hasRu) {
            RadioRow(stringResource(R.string.add_audio_ru), selected == AudioChoice.RU) {
                onSelected(AudioChoice.RU)
            }
        }
        RadioRow(stringResource(R.string.add_audio_both), selected == AudioChoice.BOTH) {
            onSelected(AudioChoice.BOTH)
        }
    }
}

@Composable
private fun RadioRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        RadioButton(selected = checked, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Секция «Качество»: сегментные кнопки 720p / 1080p. */
@Composable
private fun QualitySection(
    selectedHeight: Int,
    onSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.add_quality), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            SegmentedButton(
                selected = selectedHeight == AddViewModel.QUALITY_720,
                onClick = { onSelected(AddViewModel.QUALITY_720) },
                // Material3 1.3.0: itemShape(index, count) — без first/last.
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.add_quality_720)) }
            SegmentedButton(
                selected = selectedHeight == AddViewModel.QUALITY_1080,
                onClick = { onSelected(AddViewModel.QUALITY_1080) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.add_quality_1080)) }
        }
    }
}

/** Чекбокс «Комментарии» (вкл по умолчанию). */
@Composable
private fun CommentsRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = stringResource(R.string.add_comments_label), style = MaterialTheme.typography.bodyLarge)
    }
}
