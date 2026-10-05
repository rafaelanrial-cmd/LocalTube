package com.localtube.ui.navigation

/** Маршруты навигации (Navigation Compose). */
object Routes {
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val ADD = "add"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{id}"
    const val PLAYER = "player/{id}"

    fun details(id: Long) = "details/$id"
    fun player(id: Long) = "player/$id"
}
