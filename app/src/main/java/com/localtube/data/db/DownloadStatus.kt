package com.localtube.data.db

/**
 * Статус задачи скачивания. Room хранит enum как строку с именем значения.
 */
enum class DownloadStatus {
    QUEUED,   // ждёт в очереди
    RUNNING,  // идёт скачивание
    DONE,     // завершено успешно
    FAILED,   // ошибка (см. errorMsg)
    CANCELED, // отменено пользователем
}
