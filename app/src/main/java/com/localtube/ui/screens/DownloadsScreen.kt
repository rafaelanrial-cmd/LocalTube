package com.localtube.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R
import com.localtube.ui.components.StubScreen

/** Экран «Загрузки» (Этап 1 — заглушка). */
@Composable
fun DownloadsScreen(modifier: Modifier = Modifier) {
    StubScreen(
        title = stringResource(R.string.title_downloads),
        message = stringResource(R.string.empty_downloads),
        modifier = modifier,
    )
}
