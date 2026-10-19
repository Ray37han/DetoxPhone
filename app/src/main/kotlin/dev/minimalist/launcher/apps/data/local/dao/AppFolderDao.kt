package dev.minimalist.launcher.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.apps.data.local.entity.AppFolderEntity
import dev.minimalist.launcher.apps.data.local.entity.AppFolderMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFolderDao {

    // ── Folders ──

    @Query("SELECT * FROM app_folders ORDER BY position ASC")
    fun getAllFolders(): Flow<List<AppFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(entity: AppFolderEntity): Long

    @Query("UPDATE app_folders SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, name: String)

    @Query("DELETE FROM app_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    // ── Members ──

    @Query("SELECT * FROM app_folder_members WHERE folder_id = :folderId")
    fun getMembersInFolder(folderId: Long): Flow<List<AppFolderMemberEntity>>

    @Query("SELECT folder_id FROM app_folder_members WHERE package_name = :packageName")
    suspend fun getFolderIdForApp(packageName: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(entity: AppFolderMemberEntity)

    @Query("DELETE FROM app_folder_members WHERE package_name = :packageName")
    suspend fun removeMember(packageName: String)

    /** Remove all members when folder deleted (backup for CASCADE). */
    @Query("DELETE FROM app_folder_members WHERE folder_id = :folderId")
    suspend fun removeAllMembers(folderId: Long)
}
