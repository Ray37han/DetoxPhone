package dev.minimalist.launcher.apps.domain.repository

import dev.minimalist.launcher.apps.domain.model.AppFolder
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing and managing launcher application data.
 *
 * Implementations combine two data sources:
 * 1. [android.content.pm.PackageManager] — live list of installed apps.
 * 2. Room database — launcher metadata (custom names, hidden state, folders, favorites).
 *
 * All methods return [Flow] for reactive updates (e.g., app installed/removed broadcasts)
 * or [Result] for one-shot operations that can fail.
 *
 * Owner: apps/data/AppRepositoryImpl (Stage 2)
 */
interface AppRepository {

    /**
     * Emits the current list of visible (non-hidden) apps, sorted alphabetically by
     * [LauncherApp.displayName], re-emitting on any package or metadata change.
     */
    fun getVisibleApps(): Flow<List<LauncherApp>>

    /**
     * Emits ALL apps including hidden ones. Used by settings/hidden-app management.
     */
    fun getAllApps(): Flow<List<LauncherApp>>

    /**
     * Emits apps that are marked as favorites, in user-defined order.
     */
    fun getFavoriteApps(): Flow<List<LauncherApp>>

    /**
     * Returns all apps belonging to [folderId].
     */
    fun getAppsInFolder(folderId: Long): Flow<List<LauncherApp>>

    /**
     * Returns all user-defined folders, ordered by [AppFolder.position].
     */
    fun getFolders(): Flow<List<AppFolder>>

    /**
     * Sets a custom display name for [packageName].
     * Pass null to revert to the system label.
     */
    suspend fun setCustomName(packageName: String, name: String?): Result<Unit>

    /** Toggles hidden state for [packageName]. */
    suspend fun setHidden(packageName: String, hidden: Boolean): Result<Unit>

    /** Toggles favorite state for [packageName]. */
    suspend fun setFavorite(packageName: String, favorite: Boolean): Result<Unit>

    /** Moves [packageName] to [folderId]. Pass null to remove from folder. */
    suspend fun moveToFolder(packageName: String, folderId: Long?): Result<Unit>

    /** Creates a new folder. Returns the new folder's ID on success. */
    suspend fun createFolder(name: String): Result<Long>

    /** Renames an existing folder. */
    suspend fun renameFolder(folderId: Long, name: String): Result<Unit>

    /** Deletes a folder. Apps in the folder are moved back to the root (folderId = null). */
    suspend fun deleteFolder(folderId: Long): Result<Unit>

    /**
     * Checks whether [packageName] is launchable on the current device.
     * Returns false if the app was uninstalled or disabled after the list was cached.
     */
    suspend fun isAppLaunchable(packageName: String): Boolean
}
