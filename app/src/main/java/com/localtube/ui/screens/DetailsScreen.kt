package com.localtube.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R
import com.localtube.ui.components.StubScreen

/** Экран «Детали видео» (Этап 1 — заглушка). */
@Composable
fun DetailsScreen(id: Long, modifier: Modifier = Modifier) {
    StubScreen(
        title = stringResource(R.string.title_details),
        message = stringResource(R.string.details_stub, id),
        modifier = modifier,
    )
}
