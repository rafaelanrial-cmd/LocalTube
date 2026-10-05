package com.localtube.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R
import com.localtube.ui.components.StubScreen

/** Экран «Добавить» (Этап 1 — заглушка). */
@Composable
fun AddScreen(modifier: Modifier = Modifier) {
    StubScreen(
        title = stringResource(R.string.title_add),
        message = stringResource(R.string.empty_add),
        modifier = modifier,
    )
}
