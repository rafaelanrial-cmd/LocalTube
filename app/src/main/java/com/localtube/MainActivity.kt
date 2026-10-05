package com.localtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.localtube.ui.navigation.MainScreen
import com.localtube.ui.screens.LibraryViewModel
import com.localtube.ui.theme.LocalTubeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // TODO(Этап 12): УДАЛИТЬ — отладочный автопереход в плеер (MIUI блокирует input).
    private val debugPlayerId = MutableStateFlow<Long?>(null)

    // TODO(Этап 12): УДАЛИТЬ — отладочный автопереход в детали + автотест удаления (Этап 5).
    private val debugDetailsId = MutableStateFlow<Long?>(null)

    // TODO(Этап 12): УДАЛИТЬ — отладочная автопроверка ссылки на экране «Добавить» (Этап 6).
    private val debugAddUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeStartDebugNavigation()
        setContent {
            LocalTubeTheme {
                MainScreen(
                    debugPlayerId = debugPlayerId,
                    // TODO(Этап 12): УДАЛИТЬ — параметр отладочного автоперехода (Этап 5).
                    debugDetailsId = debugDetailsId,
                    // TODO(Этап 12): УДАЛИТЬ — параметр отладочной автопроверки ссылки (Этап 6).
                    debugAddUrl = debugAddUrl,
                )
            }
        }
    }

    /**
     * TODO(Этап 12): УДАЛИТЬ — если в externalFilesDir лежит файл-флаг,
     * создаём тестовое видео и передаём его id для автоперехода (плеер или детали).
     */
    private fun maybeStartDebugNavigation() {
        val appContainer = LocalTubeApp.instance.container
        // TODO(Этап 12): УДАЛИТЬ — флаг «добавить»: ссылка из файла, автопереход в «Добавить».
        if (DebugFlags.addUrlFlagFile(this).exists()) {
            debugAddUrl.value = DebugFlags.readAddUrlFlag(this)
            return
        }
        if (DebugFlags.flagFile(this).exists()) {
            lifecycleScope.launch {
                debugPlayerId.value = LibraryViewModel.createTestVideo(
                    appContainer.context, appContainer.videoRepository,
                )
            }
            return
        }
        // TODO(Этап 12): УДАЛИТЬ — флаг деталей: автопереход + автотест удаления (Этап 5).
        if (DebugFlags.detailsFlagFile(this).exists()) {
            lifecycleScope.launch {
                debugDetailsId.value = LibraryViewModel.createTestVideo(
                    appContainer.context, appContainer.videoRepository,
                )
            }
        }
    }
}
