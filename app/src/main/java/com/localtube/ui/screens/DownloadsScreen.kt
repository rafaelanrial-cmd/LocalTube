package com.localtube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localtube.R
import com.localtube.data.db.DownloadStatus
import com.localtube.ui.components.DownloadCard
import com.localtube.ui.components.StubScreen

/**
 * Экран «Загрузки» (Этап 7): активные задачи (очередь/скачивание) сверху,
 * завершённые (DONE / FAILED с текстом ошибки / CANCELED) — снизу.
 * Данные: Flow из БД через DownloadsViewModel.
 */
@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.factory()),
) {
    val tasks by viewModel.tasks.collectAsState()

    if (tasks.isEmpty()) {
        StubScreen(
            title = stringResource(R.string.title_downloads),
            message = stringResource(R.string.empty_downloads),
            modifier = modifier,
        )
        return
    }

    val active = tasks.filter {
        it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING
    }
    val finished = tasks - active.toSet()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (active.isNotEmpty()) {
            item(key = "header_active") {
                SectionHeader(stringResource(R.string.dl_section_active))
            }
            items(active, key = { "a-${it.id}" }) { task ->
                DownloadCard(task = task, onCancel = viewModel::cancelTask)
            }
        }
        if (finished.isNotEmpty()) {
            item(key = "header_done") {
                SectionHeader(stringResource(R.string.dl_section_done))
            }
            items(finished, key = { "f-${it.id}" }) { task ->
                DownloadCard(task = task)
            }
        }
    }
}

/** Заголовок секции списка. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
