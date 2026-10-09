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
- [x] **Этап 4** — Плеер: Media3 ExoPlayer (сборка ✓, проверка на телефоне ✓: play/pause/seek + пауза при уходе в фон и возобновление)
- [x] **Этап 5** — Детали + удаление (сборка ✓, проверка на телефоне ✓: автотест удаления, du до/после, карточка исчезла)
- [x] **Этап 6** — Добавление: метаданные (сборка ✓, проверка на телефоне по реальной ссылке ✓: FETCH_OK + карточка с верными данными)
- [x] **Этап 7** — Скачивание + прогресс (сборка ✓, проверка на телефоне ✓: реальный ролик DONE 100% с прогрессом/скоростью, оффлайн-плеер ✓, отмена ✓; 403 решён — клиент android + ByeDPI)
- [x] **Этап 8** — Комментарии с ответами (сборка ✓, **проверка на телефоне ✓: ответы появляются — подтверждено пользователем 2026-10-09**)
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

## Этап 8: комментарии с ответами + новый yt-dlp в assets + разбивка VideoDownloader (2026-10-09)
- **Задача (пользователь, 2026-10-09)**: «сделай готовое приложение с рабочими комментариями
  (и обязательно чтобы были ответы тоже, то есть комментарии с другими комментариями),
  которые скачиваются с видео. Я сам тестирую приложение на телефоне» — решение
  «ПОКА НЕ ДЕЛАЕМ» (2026-10-08) отменено.
- **Корень «нет ответов»**: AAR youtubedl-android 0.18.1 бандлит yt-dlp 2025.11.12,
  который в новом формате ответа YouTube не извлекает ответы вообще (токен
  продолжения лежит в `subThreads` — старая версия его не ходит) → в info.json
  оказывались только топовые комментарии.
- **Решение**:
  - `app/src/main/assets/ytdlp` — проверенная сборка yt-dlp **2026.08.19** (zipapp,
    2,9 МБ; промежуточные файлы сборки `_build_ytdlp/` и кэш `pip/` — gitignored);
  - новый `data/downloader/YtDlpAssets.kt`: `ensureUpToDate()` вызывается в
    YtDlpManager ДО `YoutubeDL.init()` — если установленного бинарника нет или он
    старше assets, перезаписываем; версия читается из `yt_dlp/version.py` внутри
    zipapp (сравнение YYYY.MM.DD); более новую версию (updater Этапа 10) НЕ откатываем;
  - цепочка ответов: парсер (`parent`-поле → `CommentEntity.parentId`, у топовых
    "root"; в плоском списке yt-dlp родитель всегда раньше своих детей — один проход),
    `buildTree` в DetailsViewModel (дерево без ограничения глубины, закреплённые
    первыми), UI `CommentsSection`/`CommentItem` (раскладка как на YouTube: ответы
    отступом под родителем, первые 3 сразу + «Показать ещё N», счётчик «N комментариев»).
- **Рефакторинг (правило: файлы ≤ 300 строк)**: `VideoDownloader.kt` разбит с 401 →
  295 строк; новые файлы `YtDlpProgress.kt` (ProgressHolder), `YtDlpStderr.kt`
  (хелперы stderr), `YtDlpFormats.kt` (селекторы форматов Этапа 9), `YtDlpDebugLog.kt`
  (лог-маркеры автотестов). Логика этапов не переписывалась — только перенос.
- **Ошибка «Unresolved reference takeBefore» (диагностика с доказательствами)**:
  `kotlin.text.takeBefore`/`takeAfter` **не существуют в Kotlin stdlib** (есть только
  substringBefore/substringAfter). Проверено: полный скан ВСЕХ классов официальных
  jar kotlin-stdlib 1.9.24 и 2.0.20 (SHA1 совпадают с Maven Central — кэш не битый),
  разбор константного пула StringsKt__StringsKt.class, доки (страница take-before → 404
  и в текущих, и в старых). Пугающий «Argument type mismatch: 'T?' vs 'K'» — просто то,
  как K2 отчитывается о провале цепочки `?.`, указывая на её начало; бага компилятора нет.
  Исправлено: `substringAfter("'").substringBefore("'")`.
- **Проверено агентом**: `assembleDebug` — BUILD SUCCESSFUL; APK установлен на телефон
  (c985efb2, `adb install -r` = Success); приложение стартует, процесс жив, FATAL в
  logcat нет (единственное — от стороннего com.drweb.antivirus). Временные файлы
  диагностики (`Scratch.kt`, `_init_cp.gradle`, `_cp_list.txt`) удалены.
- **Подтверждено пользователем на телефоне (2026-10-09): ответы на комментарии
  появляются — сработало.** Что именно решило проблему: замена бандлового yt-dlp
  2025.11.12 (из AAR) на сборку **2026.08.19** из `assets/ytdlp` через
  `YtDlpAssets.ensureUpToDate()` — новый yt-dlp ходит в `subThreads` формата ответа
  YouTube и извлекает ответы; парсер/UI (parentId → дерево) были готовы заранее.
  Этап 8 закрыт.

## Этап 8 (дополнение 2): ответы как на YouTube — по тапу (2026-10-09)
- **Задача (сообщил пользователь)**: «работает, но сделай как на YouTube: ответы
  показывались бы по нажатию; а то они смещаются вправо и дальше не видны».
  До этого ответы ВСЕГДА рендерились под родителем с отступом 52 dp за уровень —
  глубокие вложения уезжали за правый край экрана.
- **Что сделано (CommentsSection.kt, без новых зависимостей)**:
  - Топовый комментарий: ответы СКРЫТЫ; под ним кнопка «N ответов»
    (`comments_reply_count` + формы слова). Тап раскрывает тред, повторный —
    сворачивает (кнопка становится «Скрыть ответы», `comments_hide_replies`).
  - Раскрытый тред: прямые ответы отступом под текстом родителя (REPLY_BASE_INDENT=52 dp),
    внутри треда вложенные ответы — первые 3 + «Показать ещё N» (как было).
  - Отступ ОГРАНИЧЕН: indentFor(depth) = 52 + min(depth−1, 4)*16 dp → максимум
    116 dp; глубокие треды больше не уезжают за край.
  - Бонус: до раскрытия тред НЕ композится — при тысячах комментариев стартовый
    рендер списка стал легче (ранее ответы всех топовых композились сразу).
- **Проверено агентом**: `assembleDebug` ✓, установка на телефон ✓, запуск ✓,
  FATAL нет.
- **Подтверждено пользователем на телефоне (2026-10-09): ВСЁ РАБОТАЕТ** — ответы
  раскрываются по тапу «N ответов» (повторный тап сворачивает), вложенные ответы
  не уезжают за правый край экрана (отступ ограничен 116 dp). Задача «как на
  YouTube» закрыта окончательно.

## Этап 8 (дополнение): ВСЕ комментарии — лимит снят (2026-10-09)
- **Задача (сообщил пользователь)**: не все комментарии с видео сохранялись;
  нужно скачивать по возможности всё, что есть в видео.
- **Причина**: мы сами передавали yt-dlp `--extractor-args "youtube:max_comments=1000,150,600,20"`
  (константа MAX_COMMENTS_ARG). Семантика подтверждена по исходникам нашего
  yt-dlp 2026.08.19 (zipapp из assets, `yt_dlp/extractor/youtube/_video.py:2578`):
  это список до 5 слотов `[всего, топовые, все ответы, на тред, глубина]`,
  пустой слот = без лимита. Второй слот = **150** → у видео с >150 топовыми
  комментариями остальные молча отбрасывались.
- **Исправление**: `max_comments` из --extractor-args УБРАН полностью —
  по умолчанию все слоты безлимитные, yt-dlp обходит ВСЕ страницы continuations
  и берёт всё, что отдаёт YouTube (по возможности: при обрыве continuation'а
  на середине останется то, что успело прийти). Комментарий в VideoDownloader
  обновлён; константа MAX_COMMENTS_ARG удалена.
- **UI (обязательное следствие снятия лимита)**: CommentsSection — пагинация
  топовых: первые TOP_INITIAL=50, кнопка «Показать ещё комментарии»
  (+TOP_STEP=100 за нажатие). Без этого тысячи элементов с аватарами в не-lazy
  списке экрана деталей (verticalScroll) композились бы разом — память/ANR.
  Ответы внутри треда как были: 3 сразу + «Показать ещё N».
- **Нюанс для пользователя**: извлечение комментариев идёт ДО скачивания файла
  видео; у видео с тысячами комментариев прогресс будет стоять на 0% несколько
  минут (yt-dlp пишет «Downloading comment API JSON page N»), info.json может
  весить десятки МБ. Это цена «всех комментариев».
- **Проверено агентом**: `assembleDebug` ✓, установка на телефон ✓, запуск ✓,
  FATAL нет.
- **Подтверждено пользователем на телефоне (2026-10-09): ВСЁ РАБОТАЕТ** —
  комментарии скачиваются полностью (лимит max_comments снят).

## Этап 8 — ЗАКРЫТ ОДНОВРЕМЕННО (2026-10-09)
- Пользователь подтвердил на телефоне: «всё работает».
- Итог по всем частям Этапа 8:
  1. **Ответы на комментарии** — появляются (yt-dlp 2026.08.19 из `assets/ytdlp`
     через `YtDlpAssets.ensureUpToDate()` ходит в `subThreads`); подтверждено.
  2. **ВСЕ комментарии без лимита** — `max_comments` убран, yt-dlp обходит все
     страницы continuations; подтверждено.
  3. **UI как на YouTube** — ответы скрыты до тапа по «N ответов», отступ
     ограничен (116 dp), глубокие треды не уезжают за край; подтверждено.
- Дальнейшая работа над комментариями в Этапе 8 НЕ требуется.

## Фикс бага: автопереход в «Добавить» и самозагрузка ссылки при запуске (2026-10-08)
- **Баг (сообщил пользователь)**: при открытии приложения оно сразу улетало в экран
  «Добавить», подставляло ссылку и начинало проверку/скачку. Ожидаемое поведение:
  старт — Библиотека, поле ссылки пустое; проверка — только по нажатию «Проверить»;
  скачка — только после выбора опций (аудио/качество/комментарии) и нажатия «Скачать».
- **Причина**: отладочный механизм автотестов (файл-флаг `debug_add_url` в
  externalFilesDir, помечен TODO(Этап 12)). MainActivity читала его при КАЖДОМ
  запуске: автопереход в «Добавить» + AddViewModel сам подставлял ссылку и жмал
  «Проверить». Флаг от прошлых автотестов остался на телефоне → каждое ручное
  открытие запускало автосценарий. Автоскачивание гейтилось только по `debug_auto_download`
  без привязки к автосценарию.
- **Исправление (минимальное, рабочий код этапов не переписывался)**:
  - launch-флаги (`debug_details_ytid`, `debug_player_ytid`, `debug_add_url`) стали
    ОДНОРАЗОВЫМИ: MainActivity удаляет файл сразу после чтения → забытый флаг не
    перехватывает следующий запуск (MainActivity.maybeStartDebugNavigation);
  - `DebugFlags.sessionActive` — процессовый флаг автосценария: лог-маркеры и
    дамп-файлы (YtDlpManager, VideoDownloader, AddViewModel) гейтятся по сессии,
    а не по существованию файла (файл к моменту проверки уже удалён);
  - автоскачивание в AddViewModel срабатывает только когда `autoCheckUrl != null`
    (ссылка подставлена из флага = именно автосценарий) И есть флаг
    `debug_auto_download`; после срабатывания флаги `debug_auto_download`,
    `debug_no_comments`, `debug_audio_choice` удаляются;
  - ручной сценарий: пустое поле → ввёл ссылку → «Проверить» → опции → «Скачать»;
    скачка без нажатия «Скачать» теперь невозможна.
- **Проверено агентом**: `assembleDebug` ✓, установка ✓; запуск без флагов — старт
  в Библиотеке, поле пустое, маркеров LocalTubeDebug нет, FATAL нет; регрессия
  автотеста: флаг `debug_add_url` → `FETCH_OK id=dQw4w9WgXcQ`, флаг-файл приложение
  удалило само (одноразовость работает).
- **Подтверждено пользователем на телефоне (2026-10-08): баг исчез** — при входе
  поле пустое, скачка начинается только вручную по «Скачать». Доказательство ручного
  сценария: пользователь сам скачал `gIK3TVIwXQc` («НЕ ИДИ В ЯНДЕКС ДОСТАВКУ…»)
  — 84 874 076 байт, задача DONE 100%; в БД только это видео (тестовый мусор
  dQw4w9WgXcQ отсутствует). **Это пользовательское видео — не удалять при чистке.**
- **Очистка после теста**: debug-дампы (`debug_fetch.json`, `debug_ytdlp_thumb-1.txt`,
  `debug_ytdlp_video-1.txt`) удалены с телефона; файлов `debug_*` на телефоне
  не осталось (проверено ls).
- **Правило для будущих автотестов**: launch-флаги одноразовые (приложение само их
  удаляет); перед ручным тестом всё равно проверять `adb shell ls /sdcard/Android/data/com.localtube/files`
  — флагов быть не должно.

## Фикс бага: плеер запоминает позицию воспроизведения (2026-10-08)
- **Задача (сообщил пользователь)**: плеер должен запоминать, на каком месте
  остановилось видео, и при следующем запуске начинать с того же места.
- **Реализация**:
  - `VideoEntity.positionMs` (последнее место, мс); AppDatabase v3 + явная миграция
    2→3 в AppContainer (без неё fallbackToDestructiveMigration стёрла бы БД пользователя);
  - `VideoDao.updatePosition` / `VideoRepository.updatePosition`;
  - новый `ui/screens/PlayerPosition.kt`: объект `PlaybackPosition` (точка старта:
    сохранённое место; если остановились ближе 10 с к концу — старт с начала) +
    композабл `PlaybackPositionMemory` (запись в Room каждые 5 с, пока открыт плеер,
    и на ON_STOP);
  - старт: после prepare() — seekTo(сохранённая позиция).
- **Подтверждено пользователем на телефоне (2026-10-08): видео продолжается верно**
  (закрыл приложение → открыл снова → воспроизведение с того же места).

### Доработка: поворот экрана не сбрасывает позицию (2026-10-08)
- **Баг (сообщил пользователь)**: при повороте телефона (горизонталь ↔ вертикаль)
  видео откатывалось на старое сохранённое место.
- **Причина**: ExoPlayer создавался в remember() внутри Compose-композиции — при
  смене ориентации композиция пересоздаётся, старый игрок release()-ится, а новый
  стартует с чекпоинта из Room (сохранение каждые 5 с, асинхронное).
- **Исправление**: ExoPlayer перенесён в `PlayerViewModel` — ViewModel переживает
  смену конфигурации, поэтому при поворте тот же игрок продолжает воспроизведение:
  - создание/prepare/play/seekTo(сохранённое место) — в init ViewModel;
  - ON_STOP → пауза + флаг shouldResume (в ViewModel) + сохранение позиции;
    ON_START → play(), если shouldResume;
  - выход с экрана: пауза, release + финальное сохранение — в onCleared();
  - `viewModel(key = "player-$id")` — при смене видео не переиспользуется чужой игрок.
- **Важно**: фикс был в APK сборки 19:48, но на телефоне оставалась старая сборка
  (19:35, без фикса) → переустановлен `adb install -r` → Success; debug-флагов на
  телефоне перед проверкой не было (в files/ только thumbs/ и videos/).
- **Подтверждено пользователем на телефоне (2026-10-08): баг исчез** — при повороте
  в горизонталь и обратно видео продолжает с того же места, без отката.

## Функция: полноэкранный режим плеера (2026-10-08)
- **Задача (сообщил пользователь)**: видео можно открыть на весь экран.
- **Реализация**:
  - новый `ui/screens/PlayerFullscreen.kt`: `PlayerSystemBarsFullscreen(isFullscreen)` —
    скрытие/показ системных панелей (статус-бар + навигация) через WindowInsetsControllerCompat,
    immersive sticky: после свайпа панели на миг появляются и снова прячутся; при выходе с
    экрана панели возвращаются. Приложение уже edge-to-edge (enableEdgeToEdge в MainActivity).
  - `FullscreenExitButton` — полупрозрачная кнопка выхода поверх видео (справа сверху).
  - PlayerScreen: кнопка «На весь экран» в шапке; в полном режиме шапка приложения скрыта,
    видео без отступов на весь экран; `isFullscreen` в rememberSaveable — переживает поворот
    экрана; BackHandler(enabled = isFullscreen) — «назад» сначала выходит из полного режима.
  - strings.xml: +2 строки («На весь экран», «Выйти из полноэкранного режима»). 
  - Автотест Этапа 4 (play/pause/seek) вынесен в PlayerAudioSwitchTest.kt как
    `PlayerDebugAutoTest` — чтобы PlayerScreen не превышал 300 строк; логика без изменений.
- **Новый debug-флаг** (TODO(Этап 12)): `debug_fullscreen` — одноразовый: в плеере сам
  включает полноэкранный режим и удаляется после срабатывания; маркер FULLSCREEN_ON в logcat.
- **Сборка**: assembleDebug ✓, установка на телефон ✓ (adb install -r → Success).
- **Важно**: перед установкой приложения не было на телефоне (было удалено) — БД и
  скачанные видео стёрты, библиотека пустая; для проверки плеера нужно сначала скачать видео.
- **Проверка агентом**: запуск после обновления — процесс жив, FATAL нет, debug-флагов в
  files/ нет (проверено ls).
- **Подтверждено пользователем (2026-10-08): работает.**

## Фикс бага: кнопки плеера поверх видео, правый нижний угол (2026-10-08)
- **Баг (сообщил пользователь)**: полноэкранный режим работает («да работает»), но кнопку
  «На весь экран» надо скрывать вместе с интерфейсом плеера (он прячется при тапе по экрану);
  кнопку поставить в правый нижний угол видео, а шестерёнку настроек (аудиодорожки) — левее её.
- **Исправление**:
  - PlayerScreen: обе кнопки убраны из шапки приложения (TopAppBar остался «Назад» + заголовок).
  - Новый `ui/screens/PlayerOverlayControls.kt`: `VideoOverlayControls` — ряд кнопок в правом
    нижнем углу поверх видео: шестерёнка аудиодорожек (иконка заменена на Settings) левее,
    «На весь экран» правее. В полном режиме кнопка «На весь экран» скрыта (выход — кнопка
    справа сверху или «назад»).
  - Синхронизация с интерфейсом плеера: `PlayerView.setControllerVisibilityListener`
    (явный SAM-интерфейс — у метода два оверлода, лямбда неоднозначна) → состояние
    `showController`: при тапе по экрану и авто-скрытии через пару секунд кнопки (и выход ✕)
    прячутся вместе с контроллерами, при повторном тапе появляются.
  - Проверено по исходникам media3 1.4.1 (AAR из кэша Gradle + GitHub tag 1.4.1): встроенные
    кнопки fullscreen (`exo_fullscreen`/`exo_minimal_fullscreen`) видны только при установленном
    FullscreenButtonClickListener — мы его не ставим, конфликта в углу нет.
- **Сборка**: assembleDebug ✓ (PlayerScreen.kt = 300 строк), установка на телефон ✓,
  запуск ✓, FATAL нет.
- **Подтверждено пользователем (2026-10-08): работает.**

## Фикс бага: шестёрка — старое меню media3 + серые иконки (2026-10-08)
- **Баг**: шестёрка открывала только диалог аудиодорожек (нужно старое меню со скоростью); иконки сливались с тёмным видео.
- **Исправление**: шестёрка — программный клик по встроенной exo_settings (performClick) → родное меню media3 (скорость + дорожки), фолбэк — наш диалог; кнопки в углу: нулевой размер + INVISIBLE (шестёрка — якорь попапа); иконки серые Color(0xFF9E9E9E), шестёрка всегда видна, строка player_settings.
- **Сборка** ✓, установка на телефон ✓, FATAL нет.
- **Подтверждено пользователем (2026-10-08): всё работает отлично.**

## Этап 6 — детали (2026-10-06)
- [x] **README** (`docs/ytdlp-readme.md`): URL из задачи (`…/main/README.md`) отдаёт **404** — дефолтная ветка
      репозитория `JunkFood02/youtubedl-android` это **`master`**, не `main`. Актуальный URL:
      `https://raw.githubusercontent.com/JunkFood02/youtubedl-android/master/README.md`. API сверено с
      исходниками 0.18.x: `YoutubeDL.init(context)`, `FFmpeg.init(context)`,
      `YoutubeDL.execute(request, null, null)` → `YoutubeDLResponse(out, err, exitCode)`.
- [x] **YtDlpManager** (`data/downloader/YtDlpManager.kt`): `startInit()` — FFmpeg.init + YoutubeDL.init
      в фоне (SupervisorJob+IO, CompletableDeferred), вызывается один раз в `LocalTubeApp.onCreate`
      (инициализация при старте приложения). `suspend fetchInfo(url)`: `YoutubeDLRequest(url)` +
      `--no-playlist` + `-J`, парсинг JSON через kotlinx-serialization. Проверка `exitCode != 0` →
      ошибка с последней непустой строкой stderr (иначе битая ссылка давала бы «не удалось разобрать
      JSON»). `ensureInit` бросает понятную ошибку, если инициализация ещё не запущена.
- [x] **VideoInfoData** (`data/downloader/VideoInfoData.kt`): id, title, description, upload_date
      ("YYYYMMDD" → epoch UTC), duration, thumbnail, уникальные языки аудио из `formats[].language`,
      `hasRuTrack = "ru" in langs`. Парсинг с `ignoreUnknownKeys` (сотни лишних полей игнорируются).
- [x] **AppContainer**: `val ytDlpManager: YtDlpManager by lazy { YtDlpManager(context) }`.
- [x] **AddScreen** (`ui/screens/AddScreen.kt`, заглушка удалена): поле ссылки с валидацией
      (regex: `youtube.com/watch…v=<11 симв>` и `youtu.be/<11 симв>`, isError + supportingText),
      кнопка «Проверить» (spinner при Checking, disabled на невалидной ссылке). После проверки —
      **AddVideoCard** (`ui/components/AddVideoCard.kt`, новый): превью 200dp через Coil по https-ссылке
      (заглушка с иконкой если нет), название (3 строки), «dd.MM.yyyy · мм:сс». Секция «Аудио»:
      радиокнопки «Оригинал» / «Русская» (видна только если hasRuTrack) / «Обе дорожки». «Качество»:
      сегментные кнопки 720p/1080p. Чекбокс «Комментарии» (вкл по умолчанию). Кнопка «Скачать».
- [x] **AddViewModel** (`ui/screens/AddViewModel.kt`): состояния Idle/Checking/Checked(info)/CheckFailed;
      опции сбрасываются под новое видео; `download()` — создаёт VideoEntity (метаданные из yt-dlp,
      пути к файлам пустые — заполнит Этап 7) + DownloadTask(QUEUED), возвращает true → экран
      переходит на Загрузки (`popUpTo startDestination { saveState } launchSingleTop restoreState`).
- [x] **strings.xml**: +13 строк экрана «Добавить» (все тексты UI на русском), удалена неиспользуемая
      `empty_add`.
- [x] **Отладочный механизм** (TODO(Этап 12): УДАЛИТЬ — MIUI блокирует input): файл-флаг
      `externalFilesDir/debug_add_url`, содержимое = URL → MainActivity читает, MainScreen автопереходит
      в «Добавить», AddViewModel подставляет ссылку и сам жмёт «Проверить». Маркеры logcat (тег
      `LocalTubeDebug`): `FETCH_OK id=… langs=… hasRu=…`, `FETCH_FAIL`, `TASK_QUEUED videoId=… audio=…
      quality=… comments=…`. Флаг удалён после тестов.
- [x] **Проверка на телефоне** (критерий этапа): реальная ссылка `dQw4w9WgXcQ` (Rick Astley —
      Never Gonna Give You Up). Ground truth получен с телефона через innertube
      (`/youtubei/v1/player`): title «Rick Astley - Never Gonna Give You Up (Official Video)
      (4K Remaster)», lengthSeconds=213, publishDate 2009-10-24T23:57:33−07:00 (UTC → 25.10.2009),
      аудио только en. Результат: `FETCH_OK id=dQw4w9WgXcQ langs=en hasRu=false` — совпадает.
      uiautomator dump карточки: название точно как в ground truth, строка «**25.10.2009 · 3:33**»,
      секции Аудио (Оригинал/Обе дорожки — **«Русская» отсутствует**, т.к. hasRu=false) / Качество
      (720p выбрано, 1080p) / чекбокс «Комментарии» checked=true. Пиксельный анализ скриншота:
      84 539 уникальных цветов в области превью — кадр видео реально загружен Coil по https
      (заглушка дала бы <50). `TASK_QUEUED videoId=1 audio=ORIGINAL quality=720 comments=true`
      (кнопка «Скачать» сработала от случайного события monkey) → VideoEntity + DownloadTask(QUEUED)
      созданы, переход на Загрузки. FATAL/AndroidRuntime по com.localtube — нет.
- [x] **Очистка после теста**: `run-as com.localtube rm database/localtube.db` (тестовое видео +
      QUEUED-задача удалены — Этап 7 стартует с чистой БД), флаг `debug_add_url` удалён.
- [x] `gradlew.bat assembleDebug` — **BUILD SUCCESSFUL** (13 c). Новые зависимости: нет
      (youtubedl-android 0.18.1 + ffmpeg и kotlinx-serialization объявлены в Этапе 1).

### Замечания для следующих этапов (2026-10-06)
- **Material3 1.3.0**: `SegmentedButtonDefaults.itemShape` принимает `(index, count)`, а не
  `(first, last)` (это добавилось в более новых версиях). При апгрейде BOM — проверить.
- Ссылки на YouTube с ПК: watch-страница и innertube API **недоступны** (DPI рвёт TLS/EOF),
  работает только oembed. Ground truth для тестов брать **с телефона**: `adb shell curl -X POST
  'https://www.youtube.com/youtubei/v1/player' …` (curl на телефоне есть, YouTube доступен).
- yt-dlp 2026.08.19 установлен на ПК через pip (Python 3.11) — но к YouTube с ПК не долбит;
  для оффлайн-сайтов пригодится.
- MIUI агрессивно убивает фоновые процессы: приложение может закрыться через ~1–2 минуты после
  запуска — для скриншотов/дампов запускать и снимать быстро (окно ~30 с после FETCH_OK).

## Этап 7 — детали (2026-10-06)
- [x] **DownloadService** (`service/DownloadService.kt`, foreground, `foregroundServiceType="dataSync"`):
      разрешения FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS;
      запрос права на уведомления при первом старте (MainActivity.maybeRequestNotificationPermission,
      Android 13+). Очередь: QUEUED-задачи по одной FIFO (`getNextQueued`); прогресс в БД ~каждые
      400 мс; уведомление обновляется не чаще раза в секунду.
- [x] **VideoDownloader** (`data/downloader/VideoDownloader.kt`): превью
      `--skip-download --write-thumbnail --convert-thumbnails jpg -o thumbs/{id}.jpg`; видео+аудио
      `-f "bv*[height<=X]+ba/b" --merge-output-format mp4 -o videos/{id}.mp4`, ffmpeg из состава
      библиотеки. Прогресс: ProgressHolder (колбэк progress/ETA + «всего байт» из строки
      «of X MiB»), скорость = дельта байты/время, lastBytes/lastTimeMs — в DownloadTaskEntity;
      всё пишется в БД, UI читает Flow.
- [x] **Отмена**: cancelledFlag + `YoutubeDL.destroyProcessById`, удаление partial-файлов
      (.part/.f*/.ytdl и превью), статус CANCELED. Проверено на телефоне: задача CANCELED,
      файлы удалены (2026-10-06).
- [x] **DownloadsScreen** (`ui/screens/DownloadsScreen.kt`): активные задачи — карточка с
      названием, LinearProgressIndicator, процентами, скоростью МБ/с, ETA и чек-листом
      ☑ метаданные ☑ превью ▶ видео % ☐ комментарии (комментарии — Этап 8), кнопка «Отмена»;
      выполненные — снизу, FAILED — с текстом ошибки.
- [x] **DONE**: пути файлов + sizeBytes записаны в VideoEntity — видео появляется в библиотеке.
      Проверено: Rick Astley dQw4w9WgXcQ DONE, 21 МБ, mp4 лежит в videos/.
- [x] Тестовая кнопка Этапа 3 удалена: `TestData.kt` удалён, ветки debug-навигации с
      addTestVideo/createTestThumbnail вычищены из MainActivity; БД очищена (тестовые строки 4–6
      удалены, остался только Rick Astley + его DONE-задача).

### Финальная проверка и «прод» (2026-10-06) — этап завершён
- [x] **Осиротевшие задачи** (MIUI убивает сервис, пока процесс приложения жив):
      `DownloadService.onStartCommand` вызывает `resetRunningToQueued()` перед `runQueue` —
      RUNNING-задачи мёртвого экземпляра возвращаются в очередь; `MainActivity.onResume`
      заново запускает сервис (`DownloadService.start`, идемпотентен), если MIUI его убил.
      Проверено: сирота task 10 (RUNNING/0%) после перезапуска приложения дошла до DONE.
- [x] **VideoDownloader**: `catch (e: CancellationException) { throw e }` — при убийстве
      сервиса во время скачивания не пишем FAILED; задачу в очередь вернёт новый экземпляр
      сервиса.
- [x] **Дефолтный клиент tv → android** (`playerClient()`): при работающем ByeDPI именно
      `android` стабильно скачивает без 403 (task 10); default/tv/ios/mweb/android_vr/
      tv_embedded на текущем выходном IP блокируются. Файл-оверрайд debug_player_client с
      телефона удалён — работает «из коробки». Если 403 вернётся: перезапустить ByeDPI
      (новый IP) и/или перебрать клиентов через debug-файл.
- [x] **Критерий этапа** (реальная ссылка, реальное скачивание): task 10 `Id5RNRX30Gk`
      («Elon Just Admitted Tesla's Last Robotaxi Problem») — DONE, 100%; файл
      `videos/Id5RNRX30Gk.mp4` = 27 240 419 байт, валидный контейнер (`ftyp mp42`, атом
      `moov`). Прогресс/скорость/ETA писались в БД во время скачивания (UI читает Flow),
      уведомление обновлялось ≤1 раза/с.
- [x] **Оффлайн-воспроизведение**: флаг debug_player_ytid=Id5RNRX30Gk → приложение само
      открыло плеер; скриншот: 35 693 уникальных цвета в области видео (заглушка дала бы
      <50), средняя яркость 146 — реальный кадр из локального файла. FATAL/AndroidRuntime —
      нет. Плеер читает файл с диска — интернет не нужен.
- [x] **Отмена**: проверена ранее в тот же день — задача CANCELED, partial-файлы
      (.part/.f*/.ytdl + превью) удалены.
- [x] **Wi-Fi не переключаем**: в коде приложения нет WifiManager/setWifiEnabled (проверено
      grep'ом) — ByeDPI работает фоном, скачивание идёт через него «как обычно».
- [x] Очистка после тестов: флаг debug_player_ytid, скриншот lt_player.png и отладочные
      дампы debug_ytdlp_*.txt удалены с телефона.

### ПРОБЛЕМА: HTTP 403 при скачивании видео — РЕШЁНО (2026-10-06)
- **ВАЖНЫЙ ФАКТ (сообщил пользователь): YouTube в этой стране ЗАБЛОКИРОВАН на уровне
      провайдера/DPI. Без VPN приложение ByeDPI (`io.github.romanvht.byedpi`) YouTube НЕ
      открывается вообще — весь доступ к YouTube идёт через ByeDPI. Не отключать ByeDPI
      «для теста»: без него скачивание невозможно.
- **Ошибка**: `ERROR: unable to download video data: HTTP Error 403: Forbidden` — форматы
      извлекаются нормально, но CDN (googlevideo.com) не отдаёт поток. Превью
      (img.youtube.com) скачивается без проблем (code=0).
- **Когда**: 5+ попыток скачать RgKAFK5djSk упали с 403 примерно через час после успешного
      скачивания dQw4w9WgXcQ (task 1 DONE). Причина: выходной IP ByeDPI, через который шли
      все тестовые запросы, получил временную анти-абьюз-разметку YouTube.
- **Что пробовал и не помогло** (все попытки — при работающем ByeDPI):
  - ожидание ~20 минут;
  - player_client `tv` → «The page needs to be reloaded» (бот-чек watch-страницы);
  - `ios` → нужен GVS PO Token, https-форматы отбрасываются («Only images are available»);
  - `mweb` → JS challenge failed;
  - `android_vr` → тот же 403;
  - `tv_embedded` → «YouTube is no longer supported in this application or device»;
  - попытка без ByeDPI (VPN выключен) — бессмысленна: YouTube в стране заблокирован.
- **РЕШЕНИЕ (проверено на task 10)**: клиент `android` скачивает без 403 при работающем
  ByeDPI; дефолт в коде теперь `android`. Если 403 вернётся — перезапустить ByeDPI для
  нового выходного IP (`am force-stop io.github.romanvht.byedpi` + `am start
  io.github.romanvht.byedpi/.activities.MainActivity`) и/или перебрать клиентов через файл
  debug_player_client (метод `playerClient()`, TODO(Этап 12)).
- **Вывод для Этапа 11 (ошибки + MIUI)**: ошибка «403 Forbidden» в errorMsg задачи — это
  не баг приложения, а YouTube/сеть. Показывать пользователю понятный текст: «YouTube
  временно ограничил доступ с этой сети; попробуйте позже или перезапустите VPN».

## Этап 5 — детали (2026-10-06)
- [x] **DetailsScreen** (`ui/screens/DetailsScreen.kt`, заглушка удалена, 299 строк): по `id` из
      маршрута `details/{id}`; TopAppBar «Детали видео» + «Назад». Превью 200dp (общий Thumbnail),
      название, строки метаданных: Дата dd.MM.yyyy · Длительность мм:сс · Размер X,X МБ (пустые
      значения не показываются). Описание раскрывается кнопкой «Показать описание»/«Свернуть
      описание». Секция «Комментарии» + заглушка «Комментарии появятся после скачивания».
      Кнопки: «Смотреть» → `player/{id}`; «Удалить» (красная) → AlertDialog «Удалить видео? /
      Освободится X МБ.» с «Удалить»/«Отмена» → удаление → popBackStack в библиотеку.
      Состояния: Loading / Ready(deleting) / NotFound — «Видео не найдено».
- [x] **DetailsViewModel** (`ui/screens/DetailsViewModel.kt`): `factory(id)` (ручной DI),
      `deleteVideo(): Boolean` — сначала файлы на диске (`videoFilePath`, `thumbnailPath`; отсутствующий
      файл не ошибка, неудаляемый — исключение), затем `repository.delete(video)` — задачи и комментарии
      уходят CASCADE (FK подтверждены в сущностях). Защита от двойного удаления (`deleting`),
      лог-маркеры при включённом отладочном флаге.
- [x] **Thumbnail** (`ui/components/Thumbnail.kt`, новый): превью из файла через Coil вынесено из
      VideoCard в общий компонент (карточка библиотеки теперь его использует, код не дублируется).
      В VideoCard.kt добавлен `formatSizeMb()` («0,9 МБ», разделитель — запятая).
- [x] **strings.xml**: новые строки деталей (метки, описание, комментарии, диалог удаления,
      «Видео не найдено»); удалена неиспользуемая `details_stub`.
- [x] **Отладочный механизм** (TODO(Этап 12): УДАЛИТЬ — MIUI блокирует input-события через adb):
      файл-флаг `externalFilesDir/debug_open_details` → MainActivity создаёт тестовое видео и
      автопереходит в детали (`debugDetailsId` → MainScreen); DetailsScreen через 4 с сам запускает
      удаление; маркеры в logcat под тегом `LocalTubeDebug`: DETAILS_STATE / DELETE_START /
      DELETE_DONE / DEBUG_DELETE_RESULT. Флаг на телефоне удалён после тестов.
- [x] **Тестовый видеофайл**: старый URL blender.org (`trailer_720x405.mov`) теперь отдаёт 404 —
      файлы переложили (вчера скачивалось без проблем). Новый источник: test-videos.co.uk
      `Big_Buck_Bunny_360_10s_1MB.mp4` (H.264/avc1, **без аудиодорожки**, ровно 10,00 с,
      991 017 байт; длительность проверена парсингом mvhd). `createTestVideo`: durationSec=10,
      sizeBytes=991_017. Копия файла сохранена как `test_video.mp4` в рабочей директории агента
      (на случай повторной очистки данных на телефоне).
- [x] **Проверка на телефоне** (критерий этапа): `du -h .../files` **до** = 975K (видео 991 017 B)
      → автосценарий (флаг + monkey) → маркеры: `DETAILS_STATE=Ready id=1`, `DELETE_START`,
      `DELETE_DONE`, `DEBUG_DELETE_RESULT ok=true` → **после**: videos/ и thumbs/ пусты, files/ = 11K
      (только пустые каталоги) — место реально освобождено. uiautomator dump: библиотека в пустом
      состоянии «Здесь появятся скачанные видео» — карточка исчезла (Flow из Room пуст → запись
      удалена из БД). Скриншот деталей за 3 с до автосцены: превью #1565C0 отрисовано сверху
      (~435k пикселей), контент на светлом фоне. logcat: FATAL/AndroidRuntime по com.localtube — нет.
      Второй прогон (id=2) тоже ok=true. `adb install -r` прошёл без подтверждения MIUI.
- [x] `gradlew.bat assembleDebug` — **BUILD SUCCESSFUL**. Новые зависимости: нет.

## Этап 4 — детали (2026-10-05)
- [x] **PlayerScreen** (`ui/screens/PlayerScreen.kt`): по `id` из маршрута `player/{id}`; TopAppBar
      с названием видео и кнопкой «Назад» (ArrowBack → popBackStack). Состояния: Loading / Ready /
      FileNotFound — «Файл не найден» (иконка PlayCircle + текст) вместо плеера, если записи в Room
      нет или файла на диске больше нет.
- [x] **PlayerViewModel** (`ui/screens/PlayerViewModel.kt`): `factory(id)` (ручной DI из AppContainer),
      `uiState: StateFlow<PlayerUiState>` — читает VideoEntity по id, проверяет существование файла,
      выдаёт Ready(file) или FileNotFound.
- [x] **ExoPlayerView** (в PlayerScreen.kt): `remember(file) { ExoPlayer.Builder(context).build() }`;
      DisposableEffect: `setMediaItem(MediaItem.Builder().setUri(file.toUri()).build())` + prepare + play;
      onDispose → **player.release()**. AndroidView с `PlayerView` (showController — контроллеры
      паузы/перемотки из коробки).
- [x] **Lifecycle**: LifecycleEventObserver на activity — ON_STOP: если играл → pause (+shouldResume);
      ON_START: если shouldResume → play. Проверено на телефоне: `ON_STOP_PAUSE pos=21784ms` (открыл
      Настройки) и `ON_START_RESUME pos=21791ms` — возобновление с той же позиции, без сброса в начало.
- [x] **DetailsScreen**: временная кнопка «Смотреть» → `player/{id}` (TODO(Этап 5): УДАЛИТЬ мост;
      экран переделывается в Этапе 5). Новые строки: back, player_file_not_found, details_watch.
- [x] **LibraryViewModel**: тестовое видео вынесено в `companion.createTestVideo(context, repository): Long`
      (suspend, возвращает id — нужно для отладочного автоперехода); у записи теперь реальный
      `videoFilePath = externalFilesDir/videos/test.mp4`, durationSec=33, sizeBytes=7_750_868.
- [x] **Тестовое видео**: URL из задачи (blender.org BigBuckBunny_320x180.mp4) отдаёт 404 — файлы
      переложили в zip → скачан официальный трейлер Big Buck Bunny `trailer_720x405.mov`
      (H.264 720×406 + AAC, 33 с, 7,75 МБ; curl потребовал `--ssl-no-revoke`, иначе schannel падает на
      проверке отзыва сертификата) → `adb push ... /sdcard/Android/data/com.localtube/files/videos/test.mp4`.
- [x] **Отладочный механизм** (TODO(Этап 12): УДАЛИТЬ — `DebugFlags.kt`, MainActivity, MainScreen,
      маркеры в PlayerScreen): MIUI блокирует input-события через adb, поэтому файл-флаг
      `externalFilesDir/debug_goto_player` (`adb shell touch /sdcard/Android/data/com.localtube/files/debug_goto_player`)
      включает автопереход в плеер и сценарий play → pause(через 4 с) → seekTo(20 с); маркеры — в logcat
      под тегом `LocalTubeDebug`. Флаг на телефоне удалён после тестов.
- [x] **Проверка на телефоне** (критерий этапа): автосценарий отработал — `PAUSE pos=1524ms`,
      `SEEK_20S pos=20000ms` (перемотка работает), пауза/возобновление при уходе в фон (см. выше).
      Тест «Файл не найден»: удалил test.mp4 → вместо плеера экран «Файл не найден», затем видео вернул
      (push, 7_750_868 байт на месте). `logcat`: FATAL/AndroidRuntime по com.localtube — нет.
- [x] `gradlew.bat assembleDebug` — **BUILD SUCCESSFUL** (up-to-date; установленный APK совпадает с кодом).
      Новые зависимости: нет (Media3 1.4.1 объявлена в Этапе 1).

### Замечания для следующих этапов (2026-10-05)
- Возврат к остановленному приложению через `am start` требует флагов
  `--activity-clear-top --activity-single-top`, иначе MainActivity пересоздаётся (проверено: без них —
  новый инстанс и автосценарий стартует заново, с ними — повторное использование).
- Автосценарий плеера стартует через 4 с после открытия экрана; до всех маркеров ждать ~15–20 с.

### Замечания для следующих этапов (2026-10-06)
- Данные приложения на телефоне могут очищаться между этапами (MIUI/очистка): перед проверкой
  этапа смотреть `adb shell ls /sdcard/Android/data/com.localtube/files` — если пусто, pushить
  `test_video.mp4` в `files/videos/test.mp4` (копия в рабочей директории агента).
- Автотест удаления (Этап 5): флаг `debug_open_details` → приложение само заходит в детали и через
  4 с удаляет видео; до маркеров ждать ~10–15 с. Скриншот деталей успевать снимать на ~3-й секунде.
- blender.org переложил файлы (старый URL трейлера Big Buck Bunny — 404); для тестовых видео
  работает test-videos.co.uk.

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
