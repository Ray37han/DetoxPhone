package dev.minimalist.launcher.apps.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity tracking which apps are marked as favorites.
 * Sort position determines display order in the favorites row.
 */
@Entity(tableName = "favorite_apps")
data class FavoriteAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "sort_position")
    val sortPosition: Int = 0
)
