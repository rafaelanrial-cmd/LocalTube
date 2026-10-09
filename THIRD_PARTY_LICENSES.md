# Сторонние компоненты и их лицензии

LocalTube использует сторонние библиотеки и бинарные компоненты. Ниже — учёт
лицензий, обязательный при дистрибуции (GPL/LGPL-компоненты поставляются как
готовые бинарники от upstream; исходники доступны по ссылкам).

## 1. youtubedl-android — **GNU GPL v3**

- Артефакты: `io.github.junkfood02.youtubedl-android:library:0.18.1`,
  `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1`
- Репозиторий (исходники): https://github.com/yausername/youtubedl-android
- Полная лицензия GPLv3: https://www.gnu.org/licenses/gpl-3.0.txt
- Библиотека бандлит в себе исполняемые файлы yt-dlp и ffmpeg (см. ниже).

## 2. yt-dlp — **BSD-2-Clause**

Бинарник `ytdlp` (lazy extractors build, версия 2026.08.19) включён в APK
в `app/src/main/assets/ytdlp`. Исходники: https://github.com/yt-dlp/yt-dlp

```
Copyright (c) 2013-2026 Yannie de Castillejos and contributors

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
```

## 3. FFmpeg — **LGPL 2.1+ / GPLv3**

Бинарник ffmpeg (сборка для Android от youtubedl-android, исходно Termux)
включён в APK через модуль `ffmpeg` библиотеки youtubedl-android.
Исходники: https://ffmpeg.org/ ; сборка описана в
https://github.com/yausername/youtubedl-android/blob/master/BUILD_FFMPEG.md
Лицензии: https://www.ffmpeg.org/legal.html

## 4. Остальные зависимости Gradle — **Apache-2.0** (и совместимые)

| Компонент | Назначение | Лицензия |
|---|---|---|
| Jetpack Compose BOM 2024.09.03 | UI | Apache-2.0 |
| Navigation Compose 2.8.0 | Навигация | Apache-2.0 |
| Room 2.6.1 (+KSP) | База данных | Apache-2.0 |
| DataStore 1.1.1 | Настройки | Apache-2.0 |
| Media3 (ExoPlayer) 1.4.1 | Плеер | Apache-2.0 |
| Coil 2.7.0 | Загрузка изображений | Apache-2.0 |
| kotlinx-serialization 1.7.2 | JSON | Apache-2.0 |

Полные тексты: https://www.apache.org/licenses/LICENSE-2.0
