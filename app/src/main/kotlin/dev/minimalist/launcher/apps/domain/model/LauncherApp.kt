package dev.minimalist.launcher.apps.domain.model

/**
 * Domain model representing a launchable application visible to this launcher.
 *
 * This is a pure domain object — no Android framework types, no persistence annotations.
 * It is mapped from [android.content.pm.ResolveInfo] / [android.content.pm.ApplicationInfo]
 * in the data layer (Stage 2).
 *
 * Invariant: [packageName] is the immutable Android package identifier.
 * Custom names, visibility, and folder membership are launcher-local metadata
 * and must NEVER be written back to the installed application or its package.
 *
 * @property packageName     Android package identifier. Immutable.
 * @property activityName    Fully-qualified launcher Activity class name.
 * @property originalName    Label returned by PackageManager at query time.
 * @property customName      User-assigned display name. Null = use [originalName].
 * @property isHidden        If true, app is not shown in the default list.
 * @property isFavorite      If true, app appears in the favorites section.
 * @property folderId        ID of the [AppFolder] this app belongs to. Null = no folder.
 * @property sortPosition    User-defined sort index. Default = alphabetical (0 = unset).
 */
data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val originalName: String,
    val customName: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val folderId: Long? = null,
    val sortPosition: Int = 0
) {
    /** Returns [customName] if set, otherwise [originalName]. */
    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: originalName
}
