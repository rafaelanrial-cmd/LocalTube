package com.localtube

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.localtube.service.DownloadService
import com.localtube.ui.navigation.MainScreen
import com.localtube.ui.theme.LocalTubeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private companion object {
        const val NOTIF_PERMISSION_REQUEST_CODE = 71
    }

    // TODO(Этап 12): УДАЛИТЬ — отладочный автопереход в плеер (MIUI блокирует input).
    private val debugPlayerId = MutableStateFlow<Long?>(null)

    // TODO(Этап 12): УДАЛИТЬ — отладочный автопереход в детали + автотест удаления (Этап 5).
    private val debugDetailsId = MutableStateFlow<Long?>(null)

    // TODO(Этап 12): УДАЛИТЬ — отладочная автопроверка ссылки на экране «Добавить» (Этап 6).
    private val debugAddUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission() // Этап 7: уведомление о скачивании
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
     * Этап 7: если MIUI убил сервис скачиваний, пока процесс приложения жив,
     * возвращаем его к жизни при возврате пользователя в приложение
     * (onStartCommand идемпотентен; осиротевшие RUNNING-задачи сервис
     * вернёт в очередь сам).
     */
    override fun onResume() {
        super.onResume()
        DownloadService.start(this)
    }

    /**
     * Этап 7: разрешение на уведомления (Android 13+), запрашивается при
     * первом старте; без него уведомление о скачивании не будет видно.
     */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQUEST_CODE)
    }

    /**
     * TODO(Этап 12): УДАЛИТЬ — если в externalFilesDir лежит файл-флаг,
     * автопереход в плеер / на экран «Добавить» (механизм тестового видео удалён в Этапе 7).
     */
    private fun maybeStartDebugNavigation() {
        val appContainer = LocalTubeApp.instance.container
        // TODO(Этап 12): УДАЛИТЬ — флаг «плеер по youtubeId»: открыть плеер скачанного видео (автотест Этапа 7).
        val playerYtid = DebugFlags.readPlayerYtid(this)
        if (playerYtid != null) {
            lifecycleScope.launch {
                val video = appContainer.videoRepository.getByYoutubeId(playerYtid)
                if (video != null) debugPlayerId.value = video.id.toLong()
            }
            return
        }
        // TODO(Этап 12): УДАЛИТЬ — флаг «добавить»: ссылка из файла, автопереход в «Добавить».
        if (DebugFlags.addUrlFlagFile(this).exists()) {
            debugAddUrl.value = DebugFlags.readAddUrlFlag(this)
            return
        }
    }
}
