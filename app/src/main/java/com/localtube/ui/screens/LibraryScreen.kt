package com.localtube.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R
import com.localtube.ui.components.StubScreen

/** Экран «Библиотека» (Этап 1 — заглушка). */
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    StubScreen(
        title = stringResource(R.string.title_library),
        message = stringResource(R.string.empty_library),
        modifier = modifier,
    )
}
