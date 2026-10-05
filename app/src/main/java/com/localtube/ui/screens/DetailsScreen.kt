package com.localtube.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.localtube.R
import com.localtube.ui.components.StubScreen
import com.localtube.ui.navigation.Routes

/** Экран «Детали видео» (заглушка; полностью переделывается в Этапе 5). */
@Composable
fun DetailsScreen(
    id: Long,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        StubScreen(
            title = stringResource(R.string.title_details),
            message = stringResource(R.string.details_stub, id),
            modifier = Modifier.weight(1f),
        )

        // TODO(Этап 5): кнопка «Смотреть» — временный мост к плееру (введён в Этапе 4).
        Button(
            onClick = { navController.navigate(Routes.player(id.toLong())) },
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.details_watch))
        }
    }
}
