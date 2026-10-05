# LocalTube — PROGRESS

> **ВАЖНО (2026-10-05):** в Этапе 1 проект переопределён. Старый концепт
> («плеер локальных папок», RecyclerView/ViewBinding) заменён на «локальный
> оффлайн-YouTube»: скачивание по ссылкам через yt-dlp (youtubedl-android + ffmpeg),
> Compose, Room, ручной DI. Актуальный план — в MASTER_PLAN.md.
> Окружение из Этапа 0 осталось без изменений и подходит под новый стек.

## Карта этапов (13)
- [x] **Этап 0** — Окружение: JDK 17, Gradle, Android SDK, adb, git, телефон
- [x] **Этап 1** — Проект + навигация: скелет Compose, wrapper 8.9, 4 экрана-заглушки (сборка ✓; установка на телефон ждёт MIUI-разрешения, см. ПРОБЛЕМА)
- [ ] **Этап 2** — Room: VideoEntity, DownloadTaskEntity, CommentEntity
- [ ] **Этап 3** — Библиотека: список скачанных видео
- [ ] **Этап 4** — Плеер: Media3 ExoPlayer
- [ ] **Этап 5** — Детали + удаление
- [ ] **Этап 6** — Добавление: метаданные
- [ ] **Этап 7** — Скачивание + прогресс
- [ ] **Этап 8** — Комментарии
- [ ] **Этап 9** — RU-дорожка
- [ ] **Этап 10** — Настройки + обновление движка
- [ ] **Этап 11** — Ошибки + MIUI
- [ ] **Этап 12** — Полировка + финал

### ПРОБЛЕМА: установка на телефон заблокирована MIUI (2026-10-05)
- Ошибка: `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` — повторилась 3 раза
  (`gradlew.bat installDebug` ×2, прямой `adb install -r app-debug.apk` ×1).
- Причина: MIUI (Xiaomi) по умолчанию запрещает установку APK через USB.
- Уже пробовал: повторные установки, прямой adb install. С ПК обойти нельзя —
  это системный переключатель безопасности Xiaomi.
- **Нужно действие на телефоне:** Настройки → Дополнительные параметры → Для
  разработчиков → включить «Установка по USB» (Install via USB). После этого:
  `gradlew.bat installDebug`, затем `adb shell monkey -p com.localtube 1` и
  `adb logcat -d *:E` (проверка FATAL). Сборка APK уже готова:
  `app/build/outputs/apk/debug/app-debug.apk`.

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
- [ ] Установка на телефон + monkey + logcat — заблокировано MIUI (см. ПРОБЛЕМА выше).

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
