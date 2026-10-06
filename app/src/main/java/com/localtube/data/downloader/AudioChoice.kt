package com.localtube.data.downloader

/**
 * Выбор аудиодорожки (опции экрана «Добавить», Этап 6).
 *
 * Живёт в data-слое: с Этапа 7 его читает DownloadTaskEntity,
 * а не только UI. RU-дорожка реально подставляется в формат — Этап 9;
 * до этого все варианты скачивают оригинальную аудио (-f …+ba/b).
 */
enum class AudioChoice { ORIGINAL, RU, BOTH }
