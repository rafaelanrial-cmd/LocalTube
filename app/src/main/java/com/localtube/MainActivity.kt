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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeStartDebugNavigation()
        setContent {
            LocalTubeTheme {
                MainScreen(debugPlayerId = debugPlayerId)
            }
        }
    }

    /**
     * TODO(Этап 12): УДАЛИТЬ — если в externalFilesDir лежит файл-флаг,
     * создаём тестовое видео и передаём его id для автоперехода в плеер.
     */
    private fun maybeStartDebugNavigation() {
        if (!DebugFlags.flagFile(this).exists()) return
        lifecycleScope.launch {
            val appContainer = LocalTubeApp.instance.container
            val id = LibraryViewModel.createTestVideo(appContainer.context, appContainer.videoRepository)
            debugPlayerId.value = id
        }
    }
}
