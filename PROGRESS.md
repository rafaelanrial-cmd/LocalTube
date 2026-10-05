# LocalTube — PROGRESS

## Этапы
- [x] **Этап 0** — Подготовка ПК к Android-разработке (2026-10-05, полностью завершён)
- [ ] **Этап 1** — Каркас проекта: Gradle + wrapper, манифест, тема, главный экран; assembleDebug
- [ ] **Этап 2** — Установка и запуск на телефоне через adb
- [ ] **Этап 3** — Список видео: разрешение, сканирование папок, сетка с миниатюрами
- [ ] **Этап 4** — Экран плеера (ExoPlayer, play/pause, полноэкранный режим)
- [ ] **Этап 5** — Управление: прогресс-бар, перемотка, ±10 сек, скорость
- [ ] **Этап 6** — Продолжение с места + «Продолжить просмотр»
- [ ] **Этап 7** — Поиск и фильтры
- [ ] **Этап 8** — Настройки (тема, скорость, папка)
- [ ] **Этап 9** — Избранное и плейлисты
- [ ] **Этап 10** — Полировка: иконка, сплэш, ошибки, пустые состояния
- [ ] **Этап 11** — Производительность: миниатюры, память, тесты на телефоне
- [ ] **Этап 12** — Release-сборка (APK), финальная проверка

## Этап 0 — детали (2026-10-05)
- [x] Диск F: существует и доступен на запись
- [x] Проверка окружения ДО установки: git, java, ANDROID_HOME/пути SDK, adb, gradle
- [x] JDK 17 установлен (Temurin 17.0.20.101, winget)
- [x] Gradle 8.14 установлен (`C:\gradle\gradle-8.14`) — в winget пакета Gradle.Gradle нет, взят официальный zip с services.gradle.org
- [x] Android commandline-tools 15859902 (dl.google.com, SHA-256 сверен) → `C:\Android\cmdline-tools\latest`
- [x] `sdkmanager --licenses` (все приняты) + установлены platform-tools, platforms;android-34, build-tools;34.0.0
- [x] Переменные окружения (уровень пользователя): JAVA_HOME=JDK 17, ANDROID_HOME=C:\Android, PATH += JDK17\bin, gradle\bin, platform-tools
- [x] Перепроверка всех команд в новой оболочке: java 17.0.20.1 ✓, gradle 8.14 (JVM 17) ✓, adb 37.0.1 ✓, git 2.55.0 ✓
- [x] `git init` (ветка main) в папке проекта; локальный git-identity: LocalTube Agent
- [x] **Подключение Xiaomi по USB + разрешение отладки** (2026-10-05): устройство `c985efb2` — Xiaomi 2112123AG (Redmi Note 11 Pro 5G, кодовое имя psyche), MIUI V816, **Android 13** (`ro.build.version.release=13`)

### Что уже было на ПК (не устанавливалось)
- git 2.55.0.windows.5
- winget v1.29.380
- Java: Temurin **21** (`jdk-21.0.9.10-hotspot`) — не подходит под задачу, JDK 17 поставлен рядом
- Диск F: — существует, доступен на запись
- Android SDK / adb / Gradle / Android Studio — нигде не найдены

### Что доустановлено в Этапе 0
- Eclipse Temurin JDK 17.0.20.101 (winget) → `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
- Gradle 8.14 (официальный zip, services.gradle.org) → `C:\gradle\gradle-8.14`
- Android commandline-tools 15859902 (dl.google.com) → `C:\Android\cmdline-tools\latest`
- Пакеты SDK: platform-tools (adb 37.0.1), platforms;android-34, build-tools;34.0.0 → `C:\Android`
- Переменные окружения уровня пользователя: JAVA_HOME, ANDROID_HOME, PATH

### Замечания
- Сборка в Этапе 0 не выполняется: проекта ещё нет (первая сборка — Этап 1).
- Установочные zip-файлы удалены после успешной установки.
