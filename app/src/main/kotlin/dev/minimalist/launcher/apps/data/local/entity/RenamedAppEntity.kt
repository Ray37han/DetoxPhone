package dev.minimalist.launcher.apps.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing user-assigned custom names for apps.
 * Separate from the installed app list — this is launcher-local metadata.
 */
@Entity(tableName = "renamed_apps")
data class RenamedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "custom_name")
    val customName: String
)
