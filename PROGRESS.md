# LocalTube — PROGRESS

> **ВАЖНО (2026-10-05):** в Этапе 1 проект переопределён. Старый концепт
> («плеер локальных папок», RecyclerView/ViewBinding) заменён на «локальный
> оффлайн-YouTube»: скачивание по ссылкам через yt-dlp (youtubedl-android + ffmpeg),
> Compose, Room, ручной DI. Актуальный план — в MASTER_PLAN.md.
> Окружение из Этапа 0 осталось без изменений и подходит под новый стек.

## Карта этапов (13)
- [x] **Этап 0** — Окружение: JDK 17, Gradle, Android SDK, adb, git, телефон
- [x] **Этап 1** — Проект + навигация: скелет Compose, wrapper 8.9, 4 экрана-заглушки (сборка ✓, установка и запуск на телефоне ✓)
- [x] **Этап 2** — Room: сущности, DAO, AppDatabase v1, репозитории (сборка ✓)
- [x] **Этап 3** — Библиотека: LibraryViewModel + LazyColumn карточек (Coil, мм:сс, dd.MM.yyyy, бейдж RU) (сборка ✓, проверка на телефоне ✓)
- [ ] **Этап 4** — Плеер: Media3 ExoPlayer
- [ ] **Этап 5** — Детали + удаление
- [ ] **Этап 6** — Добавление: метаданные
- [ ] **Этап 7** — Скачивание + прогресс
- [ ] **Этап 8** — Комментарии
- [ ] **Этап 9** — RU-дорожка
- [ ] **Этап 10** — Настройки + обновление движка
- [ ] **Этап 11** — Ошибки + MIUI
- [ ] **Этап 12** — Полировка + финал

### Решено: установка на телефон (2026-10-05)
- Было: `INSTALL_FAILED_USER_RESTRICTED` ×3 — MIUI просит подтверждение установки.
- Решение: пользователь нажал «ОК» на телефоне при повторной установке →
  `adb install -r app-debug.apk` = **Success**.
- Запуск проверен: `adb shell monkey -p com.localtube 1` → MainActivity стартовала,
  процесс (PID 29413) жив, в logcat **0 FATAL EXCEPTION / Fatal signal**
  (шум AppSearchIcing — системная индексация MIUI, не ошибка приложения).
- В будущих этапах: при `installDebug`/`adb install` ждать подтверждения на телефоне.

## Этап 3 — детали (2026-10-05)
- [x] **LibraryViewModel** (`ui/screens/LibraryViewModel.kt`): `videos: StateFlow<List<VideoEntity>>`
      из `VideoRepository.observeAll()` (= VideoDao.getAll()), `stateIn(WhileSubscribed(5s))`.
      Фабрика `factory()` тянет репозиторий из `LocalTubeApp.instance.container` (ручной DI).
- [x] **LibraryScreen** (`ui/screens/LibraryScreen.kt`, заглушка удалена): LazyColumn карточек
      (contentPadding 16dp, spacing 12dp), клик → `details/{id}`. Пустое состояние:
      «Здесь появятся скачанные видео» + Button «Добавить видео» (→ add) + OutlinedButton
      «+ тестовое видео». Строки — в strings.xml.
- [x] **VideoCard** (`ui/components/VideoCard.kt`): ElevatedCard, превью 160dp через Coil
      `AsyncImage(File(thumbnailPath))` (ContentScale.Crop); если файла нет — заглушка с иконкой
      PlayCircle. Бейдж «RU» (Surface primaryContainer) в правом верхнем углу превью, если hasRuTrack.
      Метастрока: «мм:сс · dd.MM.yyyy» (formatDuration/formatDate; пустые части не показываются).
- [x] Временное тестовое видео (TODO(Этап 7): УДАЛИТЬ в ViewModel и на экране): фиксированный
      youtubeId `localtube-test-video` (REPLACE — дублей нет), title «Тестовое видео», 254 с (4:14),
      дата = сейчас, audioLangs "en,ru", hasRuTrack=true; превью PNG 640×360 (#1565C0 + текст)
      генерируется в `getExternalFilesDir(null)/thumbs/test_video.png`, чтобы проверить Coil по файлу.
- [x] `gradlew.bat assembleDebug` — **BUILD SUCCESSFUL** (10 c). Новые зависимости: нет
      (Coil 2.7.0 и lifecycle-viewmodel-compose уже были в дереве).
- [x] Телефон: `adb install -r` = Success (без подтверждения MIUI), запуск, дамп UI:
      карточка видна — «Тестовое видео», «4:14 · 05.10.2026», бейдж «RU»; скриншот + пиксельная
      проверка подтвердили, что превью отрисовано из файла (≈105k синих пикселей #1565C0).
      Клик по карточке → экран деталей («Видео с id: 1»). `logcat -d *:E`: ошибок com.localtube нет
      (только системный шум MIUI: AppSearchIcing, ActivityManagerWrapper; единственный FATAL —
      в чужом приложении com.wb.courier).

### Замечания для следующих этапов (2026-10-05)
- **MIUI блокирует `adb shell input tap/keyevent`** (SecurityException INJECT_EVENTS) — нужен
  «Отладка по USB (безопасность)» в настройках разработчика. Обход: тапать через
  `adb shell monkey -p com.localtube 1` (случайные события, но права есть) и проверять результат
  через `uiautomator dump`.
- **Git Bash конвертирует аргументы вида `/sdcard/...`** в локальные пути: перед adb-командами
  с device-путями ставить `export MSYS2_ARG_CONV_EXCL='*'` (иначе `adb shell uiautomator dump /sdcard/ui.xml`
  ломается).
- Скриншот телефона: `adb exec-out screencap -p > shot.png`; анализ пикселей — Python+PIL.

## Этап 2 — детали (2026-10-05)
- [x] `data/db`: **VideoEntity** (`videos`, уникальный индекс по `youtubeId`; audioLangs —
      строка с кодами через запятую, например "en,ru"), **DownloadTaskEntity** (`download_tasks`,
      FK→videos.id, onDelete=CASCADE; статус — enum `DownloadStatus` QUEUED|RUNNING|DONE|
      FAILED|CANCELED, Room хранит имя значения), **CommentEntity** (`comments`, FK→videos.id,
      CASCADE; parentId для ответов в Этапе 8).
- [x] DAO: **VideoDao** (getAll — Flow, сортировка по uploadDateEpoch DESC; getById suspend;
      insert REPLACE → Long; @Delete), **DownloadTaskDao** (Flow всех, getById, update; + insert
      сверх списка задачи — без него нельзя создать задачу в Этапе 7), **CommentDao**
      (getByVideoId — Flow, likeCount DESC; insert; deleteByVideoId).
- [x] **AppDatabase** v1, exportSchema=false; билдер в AppContainer с
      `fallbackToDestructiveMigration()`; имя файла `localtube.db`.
- [x] Репозитории (обычные классы, экземпляры в AppContainer через `by lazy`):
      **VideoRepository** (observeAll/getById/insert/delete), **DownloadRepository**
      (задачи: observeTasks/getTask/insertTask/updateTask; комментарии: observeComments/
      insertComment/deleteComments — CommentRepository в плане нет, поэтому операции
      комментариев живут здесь).
- [x] Зависимости новые не добавлял: Room 2.6.1 + KSP уже были объявлены в Этапе 1.
- [x] `gradlew.bat assembleDebug` — **BUILD SUCCESSFUL** (17 c), kspDebugKotlin прошёл
      без ошибок и предупреждений по схеме.

## Этап 1 — детали (2026-10-05)
- [x] MASTER_PLAN.md переписан под новый концепт (оффлайн-YouTube, yt-dlp, Compose+Room);
      старый концепт «плеер локальных папок» отменён.
- [x] Проект создан вручную: settings/build.gradle.kts, gradle/libs.versions.toml,
      манифест (INTERNET), LocalTubeApp + ручной AppContainer (без Hilt),
      MainActivity, тема Material3 (динамические цвета), адаптивная иконка без PNG.
- [x] Wrapper: `gradle wrapper --gradle-version 8.9` (локальный Gradle 8.14).
- [x] Все зависимости плана сразу: Compose BOM 2024.09.03, Navigation 2.8.0,
      Room 2.6.1 (+KSP), DataStore 1.1.1, Media3 1.4.1, Coil 2.7.0,
      kotlinx-serialization 1.7.2, youtubedl-android **0.18.1** (library+ffmpeg;
      версия проверена по maven-metadata.xml), `useLegacyPackaging = true`.
- [x] Навигация: library, downloads, add, settings, details/{id}, player/{id};
      нижняя панель Библиотека/Загрузки/Добавить/Настройки; экраны — заглушки
      с русскими заголовками (ui/components/StubScreen.kt).
- [x] `gradlew.bat assembleDebug` — BUILD SUCCESSFUL (JDK 17).
- [x] Установка на телефон (`adb install -r`, подтверждение «ОК» на телефоне) +
      `monkey -p com.localtube 1` + `logcat`: приложение открылось, FATAL нет.

### Среда сборки (важно для следующих этапов)
- Команда: `cd F:\...\LocalTube && JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot' ./gradlew.bat assembleDebug`
  (в оболочках Unsloth Studio JAVA_HOME пустой; без него Gradle берёт JDK 21 —
  лучше явно ставить JDK 17).
- local.properties: `sdk.dir=C:/Android` (прямые слэши; экранированные `\\:` не работают в AGP).
- Material3 1.3.0 (BOM 2024.09.03): `NavigationBarItem` — только внутри
  `NavigationBar { }` (это RowScope-функция), отдельный composable-хелпер не работает.

## Этап 0 — детали (2026-10-05, окружение не менялось)
- JDK 17: `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
- Gradle 8.14: `C:\gradle\gradle-8.14` (для генерации wrapper; сам проект собирается на Gradle 8.9 из плана)
- Android SDK: `C:\Android` — platform-tools (adb 37.0.1), platforms;android-34, build-tools;34.0.0, cmdline-tools/latest
- git 2.55.0; репозиторий `git init` на ветке main; identity: LocalTube Agent
- Телефон: Xiaomi 2112123AG (Redmi Note 11 Pro 5G, «psyche»), MIUI V816, Android 13, adb-серийник `c985efb2`

### Замечания для следующих этапов
- В оболочках Unsloth Studio JAVA_HOME/ANDROID_HOME могут быть пустыми: перед сборкой ставить `JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'`.
- adb — `C:\Android\platform-tools\adb.exe`; устройство уже видно в `adb devices`.
