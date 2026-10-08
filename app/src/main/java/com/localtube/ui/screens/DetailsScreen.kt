package com.localtube.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.localtube.DebugFlags
import com.localtube.R
import com.localtube.data.db.VideoEntity
import com.localtube.ui.components.CommentsSection
import com.localtube.ui.components.Thumbnail
import com.localtube.ui.components.formatDate
import com.localtube.ui.components.formatDuration
import com.localtube.ui.components.formatSizeMb
import com.localtube.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Экран «Детали видео» (Этап 5): превью, метаданные, описание, комментарии (Этап 8), удаление. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    id: Long,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = viewModel(factory = DetailsViewModel.factory(id)),
) {
    val uiState by viewModel.uiState.collectAsState()
    // Этап 8: комментарии из Room (Flow), читаются офлайн.
    val comments by viewModel.comments.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            DetailsUiState.Loading -> Unit // мгновение до загрузки записи из Room

            DetailsUiState.NotFound -> NotFoundContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            is DetailsUiState.Ready -> {
                DebugAutoDeleteTest(id, viewModel, navController)
                DetailsContent(
                    video = state.video,
                    deleting = state.deleting,
                    comments = comments,
                    onWatch = { navController.navigate(Routes.player(id)) },
                    onDeleteConfirmed = {
                        scope.launch {
                            if (viewModel.deleteVideo()) navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }
        }
    }
}

/** Содержимое экрана: превью, название, метаданные, кнопки, описание, комментарии. */
@Composable
private fun DetailsContent(
    video: VideoEntity,
    deleting: Boolean,
    comments: List<CommentNode>,
    onWatch: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var descriptionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Thumbnail(
            path = video.thumbnailPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        MetaRows(video)

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onWatch, enabled = !deleting) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.details_watch))
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !deleting,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.details_delete))
            }
        }

        if (video.description.isNotBlank()) {
            OutlinedButton(
                onClick = { descriptionExpanded = !descriptionExpanded },
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(
                    stringResource(
                        if (descriptionExpanded) R.string.details_hide_description
                        else R.string.details_show_description,
                    ),
                )
            }
            if (descriptionExpanded) {
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.details_comments), style = MaterialTheme.typography.titleMedium)
        CommentsSection(comments = comments, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.details_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.details_delete_dialog_text, formatSizeMb(video.sizeBytes)),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.details_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.details_cancel))
                }
            },
        )
    }
}

/** Строки метаданных: дата, длительность, размер (пустые значения не показываются). */
@Composable
private fun MetaRows(video: VideoEntity) {
    val rows = buildList {
        if (video.uploadDateEpoch > 0) {
            add(stringResource(R.string.details_label_date) to formatDate(video.uploadDateEpoch))
        }
        if (video.durationSec > 0) {
            add(stringResource(R.string.details_label_duration) to formatDuration(video.durationSec))
        }
        if (video.sizeBytes > 0) {
            add(stringResource(R.string.details_label_size) to "${formatSizeMb(video.sizeBytes)} МБ")
        }
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    "$label: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Экран «Видео не найдено»: запись удалена или id из маршрута неверный. */
@Composable
private fun NotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(
            stringResource(R.string.details_not_found),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** TODO(Этап 12): УДАЛИТЬ — автотест удаления без тапов (MIUI блокирует input-события):
 * при включённом флаге ждём загрузки записи и запускаем удаление; маркеры — в logcat. */
@Composable
private fun DebugAutoDeleteTest(
    id: Long,
    viewModel: DetailsViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    if (!DebugFlags.detailsFlagFile(context).exists()) return

    LaunchedEffect(Unit) {
        delay(4_000) // запас на загрузку записи и анимации перехода
        val state = viewModel.uiState.value
        Log.d(DebugFlags.TAG, "DETAILS_STATE=${state::class.simpleName} id=$id")
        if (state is DetailsUiState.Ready && viewModel.deleteVideo()) {
            Log.d(DebugFlags.TAG, "DEBUG_DELETE_RESULT ok=true")
            navController.popBackStack()
        }
    }
}
