# LocalTube / Локальный YouTube

**Оффлайн-YouTube для Android.** Вставьте ссылку на видео — скачайте его с русской озвучкой и смотрите без интернета: встроенный плеер, библиотека, комментарии.

**Offline YouTube for Android.** Paste a video link — download it (optionally with the Russian dub) and watch without internet: built-in player, library, comments.

## Возможности / Features

- **Добавление по ссылке** — YouTube, метаданные (название, канал, длительность, обложка) подтягиваются автоматически
  / **Add by link** — YouTube metadata (title, channel, duration, thumbnail) is fetched automatically
- **Скачивание в фоне** — движок yt-dlp + ffmpeg работает прямо на устройстве, без серверов; прогресс, скорость, отмена
  / **Background downloads** — the yt-dlp + ffmpeg engine runs on-device, no servers; progress, speed, cancel
- **Аудиодорожки** — выбор: оригинал / русская озвучка / обе дорожки сразу (mkv с двумя треками, подписанными языком)
  / **Audio tracks** — choose original / Russian dub / both at once (mkv with two language-tagged tracks)
- **Библиотека** — все скачанные видео с обложками, длительностью и датой; детали и удаление
  / **Library** — all downloaded videos with thumbnails, duration and date; details screen and delete
- **Оффлайн-плеер** (Media3/ExoPlayer) — запоминание позиции, полноэкранный режим, переключение аудиодорожек, пауза при уходе в фон
  / **Offline player** (Media3/ExoPlayer) — position memory, fullscreen, audio track switching, pause in background
- **Комментарии** — как на YouTube: с ответами, по тапу «N ответов»
  / **Comments** — like on YouTube: with replies, expanded by tapping "N replies"
- **Всё локально** — данные и видео хранятся только на вашем устройстве
  / **Everything is local** — data and videos are stored only on your device

## Скриншоты / Screenshots

| Экран деталей (онлайн) / Details screen (online) |
|---|
| <img src="docs/screenshots/details_online.png" width="320" alt="Экран деталей видео"> |

## Установка / Installation

### Готовый APK / Ready-made APK

Скачайте `LocalTube1.0.apk` со страницы [Releases](https://github.com/Rafael/LocalTube/releases) и установите
(при первом запуске Android попросит разрешить «Установку из неизвестных источников»).

Download `LocalTube1.0.apk` from the [Releases](https://github.com/Rafael/LocalTube/releases) page and install it
(on first launch Android will ask you to allow "Install unknown apps").

### Сборка из исходников / Build from source

Требования: **JDK 17**, **Android SDK** (platform 34, build-tools 34.0.0), Gradle не нужен — используется wrapper.

Requirements: **JDK 17**, **Android SDK** (platform 34, build-tools 34.0.0); no Gradle install needed — the wrapper is used.

```bat
:: Windows
gradlew.bat assembleDebug
```

```sh
# Linux / macOS
./gradlew assembleDebug
```

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`.
The resulting APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Как это работает / How it works

APK содержит бандл **yt-dlp 2026.08.19** и **ffmpeg** (через библиотеку [youtubedl-android](https://github.com/yausername/youtubedl-android)).
Скачивание, слияние видео+аудио и тегирование дорожек выполняются локально на устройстве — приложение не использует никаких сторонних серверов.

The APK bundles **yt-dlp 2026.08.19** and **ffmpeg** (via the [youtubedl-android](https://github.com/yausername/youtubedl-android) library).
Downloading, video+audio merging and track tagging all happen locally on the device — the app uses no third-party servers.

## Структура проекта / Project structure

```
app/src/main/java/com/localtube/
├── data/        # Room (Video/DownloadTask/Comment), репозитории, DI-контейнер
│   └── downloader/  # yt-dlp: инициализация, форматы, скачивание, прогресс
├── ui/          # Compose: library, downloads, add, settings, details, player
└── LocalTubeApp.kt
```

## Лицензия / License

Код приложения распространяется под **MIT** — см. [LICENSE](LICENSE).
Сторонние компоненты (yt-dlp, ffmpeg, youtubedl-android) и их лицензии — в [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

The app code is released under the **MIT** license — see [LICENSE](LICENSE).
Third-party components (yt-dlp, ffmpeg, youtubedl-android) and their licenses are listed in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
