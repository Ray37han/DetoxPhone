package dev.minimalist.launcher.apps.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.apps.data.local.dao.AppFolderDao
import dev.minimalist.launcher.apps.data.local.dao.FavoriteAppDao
import dev.minimalist.launcher.apps.data.local.dao.HiddenAppDao
import dev.minimalist.launcher.apps.data.local.dao.RenamedAppDao
import dev.minimalist.launcher.apps.data.local.entity.AppFolderEntity
import dev.minimalist.launcher.apps.data.local.entity.AppFolderMemberEntity
import dev.minimalist.launcher.apps.data.local.entity.FavoriteAppEntity
import dev.minimalist.launcher.apps.data.local.entity.HiddenAppEntity
import dev.minimalist.launcher.apps.data.local.entity.RenamedAppEntity
import dev.minimalist.launcher.apps.domain.model.AppFolder
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.apps.domain.repository.AppRepository
import dev.minimalist.launcher.core.common.Result
import dev.minimalist.launcher.core.common.runCatchingResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines PackageManager queries with Room metadata to produce
 * the full launcher app list with custom names, hidden/favorite state, and folders.
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renamedAppDao: RenamedAppDao,
    private val hiddenAppDao: HiddenAppDao,
    private val favoriteAppDao: FavoriteAppDao,
    private val appFolderDao: AppFolderDao
) : AppRepository {

    private val pm: PackageManager get() = context.packageManager

    /**
     * Emits installed launchable apps, re-emitting on package install/remove/update.
     */
    private fun installedAppsFlow(): Flow<List<LauncherApp>> = callbackFlow {
        fun queryApps(): List<LauncherApp> {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            return resolveInfos.mapNotNull { ri ->
                val activityInfo = ri.activityInfo ?: return@mapNotNull null
                LauncherApp(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                    originalName = ri.loadLabel(pm).toString()
                )
            }.distinctBy { it.packageName }
        }

        trySend(queryApps())

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(pkg: String, user: UserHandle) { trySend(queryApps()) }
            override fun onPackageChanged(pkg: String, user: UserHandle) { trySend(queryApps()) }
            override fun onPackageRemoved(pkg: String, user: UserHandle) { trySend(queryApps()) }
            override fun onPackagesAvailable(pkgs: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(queryApps()) }
            override fun onPackagesUnavailable(pkgs: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(queryApps()) }
        }
        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    /**
     * Merges installed apps with Room metadata (custom names, hidden, favorite, folder).
     */
    private fun mergedAppsFlow(): Flow<List<LauncherApp>> = combine(
        installedAppsFlow(),
        renamedAppDao.getAll(),
        hiddenAppDao.getAll(),
        favoriteAppDao.getAll(),
    ) { installed, renamed, hidden, favorites ->
        val renameMap = renamed.associate { it.packageName to it.customName }
        val hiddenSet = hidden.map { it.packageName }.toSet()
        val favMap = favorites.associate { it.packageName to it.sortPosition }

        installed.map { app ->
            app.copy(
                customName = renameMap[app.packageName],
                isHidden = app.packageName in hiddenSet,
                isFavorite = app.packageName in favMap,
                sortPosition = favMap[app.packageName] ?: 0
            )
        }
    }

    override fun getVisibleApps(): Flow<List<LauncherApp>> =
        mergedAppsFlow().map { list ->
            list.filter { !it.isHidden }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun getAllApps(): Flow<List<LauncherApp>> =
        mergedAppsFlow().map { list ->
            list.sortedBy { it.displayName.lowercase() }
        }

    override fun getFavoriteApps(): Flow<List<LauncherApp>> =
        mergedAppsFlow().map { list ->
            list.filter { it.isFavorite }
                .sortedBy { it.sortPosition }
        }

    override fun getAppsInFolder(folderId: Long): Flow<List<LauncherApp>> = combine(
        mergedAppsFlow(),
        appFolderDao.getMembersInFolder(folderId)
    ) { apps, members ->
        val memberPkgs = members.map { it.packageName }.toSet()
        apps.filter { it.packageName in memberPkgs }
            .sortedBy { it.displayName.lowercase() }
    }

    override fun getFolders(): Flow<List<AppFolder>> =
        appFolderDao.getAllFolders().map { list ->
            list.map { AppFolder(id = it.id, name = it.name, position = it.position) }
        }

    override suspend fun setCustomName(packageName: String, name: String?): Result<Unit> = runCatchingResult {
        if (name.isNullOrBlank()) {
            renamedAppDao.delete(packageName)
        } else {
            renamedAppDao.upsert(RenamedAppEntity(packageName, name))
        }
    }

    override suspend fun setHidden(packageName: String, hidden: Boolean): Result<Unit> = runCatchingResult {
        if (hidden) {
            hiddenAppDao.insert(HiddenAppEntity(packageName))
        } else {
            hiddenAppDao.delete(packageName)
        }
    }

    override suspend fun setFavorite(packageName: String, favorite: Boolean): Result<Unit> = runCatchingResult {
        if (favorite) {
            val nextPos = favoriteAppDao.getMaxPosition() + 1
            favoriteAppDao.upsert(FavoriteAppEntity(packageName, nextPos))
        } else {
            favoriteAppDao.delete(packageName)
        }
    }

    override suspend fun moveToFolder(packageName: String, folderId: Long?): Result<Unit> = runCatchingResult {
        if (folderId == null) {
            appFolderDao.removeMember(packageName)
        } else {
            appFolderDao.insertMember(AppFolderMemberEntity(packageName, folderId))
        }
    }

    override suspend fun createFolder(name: String): Result<Long> = runCatchingResult {
        appFolderDao.insertFolder(AppFolderEntity(name = name))
    }

    override suspend fun renameFolder(folderId: Long, name: String): Result<Unit> = runCatchingResult {
        appFolderDao.renameFolder(folderId, name)
    }

    override suspend fun deleteFolder(folderId: Long): Result<Unit> = runCatchingResult {
        appFolderDao.removeAllMembers(folderId)
        appFolderDao.deleteFolder(folderId)
    }

    override suspend fun isAppLaunchable(packageName: String): Boolean = try {
        pm.getLaunchIntentForPackage(packageName) != null
    } catch (_: Exception) {
        false
    }
}
