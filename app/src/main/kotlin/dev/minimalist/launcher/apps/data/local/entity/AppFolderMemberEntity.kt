package dev.minimalist.launcher.apps.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Junction entity mapping apps to folders.
 * Uses a foreign key to [AppFolderEntity] with CASCADE delete.
 */
@Entity(
    tableName = "app_folder_members",
    foreignKeys = [
        ForeignKey(
            entity = AppFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppFolderMemberEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "folder_id", index = true)
    val folderId: Long
)
