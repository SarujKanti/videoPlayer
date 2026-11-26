package com.skd.videoplayer.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** SQL that creates the `watched_videos` table; must match the schema Room exports for version 3. */
internal const val CREATE_WATCHED_VIDEOS_SQL =
    "CREATE TABLE IF NOT EXISTS `watched_videos` " +
        "(`video_id` INTEGER NOT NULL, `first_played_at` INTEGER NOT NULL, PRIMARY KEY(`video_id`))"

/**
 * Marks videos that already have a saved position as watched. Library videos use their numeric
 * MediaStore id as `media_key`; keys from files opened in other apps are URIs and are skipped.
 */
internal const val SEED_WATCHED_VIDEOS_SQL =
    "INSERT OR IGNORE INTO `watched_videos` (`video_id`, `first_played_at`) " +
        "SELECT CAST(`media_key` AS INTEGER), `updated_at` FROM `playback_positions` " +
        "WHERE `media_key` != '' AND `media_key` NOT GLOB '*[^0-9]*'"

/**
 * v2 -> v3: adds `watched_videos` and seeds it from playback history so existing users don't
 * see "NEW" on videos they already watched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_WATCHED_VIDEOS_SQL)
        db.execSQL(SEED_WATCHED_VIDEOS_SQL)
    }
}
