package com.skd.videoplayer.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The unit-test source set has no SQLite engine, so these tests pin down the SQL that
 * [MIGRATION_2_3] runs. Executing it against a real database needs an instrumented test.
 */
class MigrationsTest {

    @Test
    fun `migration goes from version 2 to 3`() {
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }

    @Test
    fun `table sql matches the entity definition`() {
        assertTrue("`video_id` INTEGER NOT NULL" in CREATE_WATCHED_VIDEOS_SQL)
        assertTrue("`first_played_at` INTEGER NOT NULL" in CREATE_WATCHED_VIDEOS_SQL)
        assertTrue("PRIMARY KEY(`video_id`)" in CREATE_WATCHED_VIDEOS_SQL)
    }

    @Test
    fun `seed copies only numeric media keys from playback history`() {
        assertTrue(SEED_WATCHED_VIDEOS_SQL.startsWith("INSERT OR IGNORE INTO `watched_videos`"))
        assertTrue("FROM `playback_positions`" in SEED_WATCHED_VIDEOS_SQL)
        // Rejects any key containing a non-digit (content URIs) and empty keys.
        assertTrue("NOT GLOB '*[^0-9]*'" in SEED_WATCHED_VIDEOS_SQL)
        assertTrue("`media_key` != ''" in SEED_WATCHED_VIDEOS_SQL)
    }
}
