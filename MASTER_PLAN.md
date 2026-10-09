# LocalTube — локальный оффлайн-YouTube (Xiaomi 12X, Android 13)
Папка проекта: F:\DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD\LocalTube
Цепочка: вставил ссылку → приложение скачало (видео, RU-аудио если есть, превью,
описание, комментарии) → смотришь оффлайн → удаляешь для освобождения памяти.

## Стек и версии (не менять без причину)
Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20 (+ plugin.compose), KSP 2.0.20-1.0.25,
compileSdk 34, minSdk 31, targetSdk 34, applicationId com.localtube.
Compose BOM 2024.09.03 (Material3), Navigation Compose 2.8.0, Room 2.6.1,
DataStore 1.1.1, Media3 1.4.1, Coil 2.7.0, kotlinx-serialization 1.7.2.
Скачивание: io.github.junkfood02.youtubedl-android:library + :ffmpeg
(актуальную версию проверь: curl https://repo1.maven.org/maven2/io/github/junkfood02/youtubedl-android/library/maven-metadata.xml).
ОБЯЗАТЕЛЬНО в app/build.gradle.kts: packaging { jniLibs { useLegacyPackaging = true } }
Разрешение INTERNET добавить сразу. DI — ручной AppContainer, без Hilt.
Файлы храним в context.getExternalFilesDir(null), подпапки videos/, thumbs/.
Тексты UI на русском.

## Структура
data/db, data/repo, data/downloader, service,
ui/screens/{library,downloads,add,details,player,settings}, ui/components

## Сущности Room
VideoEntity(id, youtubeId, title, description, uploadDateEpoch, durationSec,
 thumbnailPath, videoFilePath, sizeBytes, audioLangs, hasRuTrack)
DownloadTaskEntity(id, videoId, url, status QUEUED|RUNNING|DONE|FAILED|CANCELED,
 progressPercent, speedBps, etaSec, videoOk, thumbOk, commentsOk, errorMsg)
CommentEntity(id, videoId, author, text, likeCount, publishedAtEpoch, parentId)

## Карта этапов
0 окружение · 1 проект+навигация · 2 Room · 3 библиотека · 4 плеер ·
5 детали+удаление · 6 добавление: метаданные · 7 скачивание+прогресс ·
8 комментарии (дизайн как на YouTube: ответы, аватары, закрепление — 2026-10-08) · 9 RU-дорожка ·
10 настройки+обновление движка ·
11 ошибки+MIUI · 12 полировка+финал

## Правила
- yt-dlp всегда вызывать с --no-playlist.
- Прогресс и статус — только через DownloadTaskEntity, UI читает Flow из Room.
- Конец каждого этапа: assembleDebug без ошибок + PROGRESS.md + git commit.
