package dev.minimalist.launcher.apps.domain.model

/**
 * Domain model representing a folder that groups [LauncherApp]s.
 *
 * Folders are launcher-local constructs — they do not correspond to anything
 * in Android's package system. Apps reference folders via [LauncherApp.folderId].
 *
 * Invariant: apps are NOT stored inside a folder object.
 * The relationship is folder → apps via [LauncherApp.folderId] to avoid
 * maintaining duplicate app records. Query apps by folderId when needed.
 *
 * @property id       Stable database-assigned identifier.
 * @property name     User-visible folder label.
 * @property position Sort order among folders on the home screen.
 */
data class AppFolder(
    val id: Long,
    val name: String,
    val position: Int = 0
)
