package dev.minimalist.launcher.apps.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity tracking which apps the user has hidden from the main list.
 */
@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String
)
