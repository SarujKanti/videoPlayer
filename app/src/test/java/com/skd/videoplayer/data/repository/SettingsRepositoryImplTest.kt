package com.skd.videoplayer.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skd.videoplayer.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs against a real DataStore file, so persistence is exercised end to end. */
class SettingsRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { folder.root.resolve("settings.preferences_pb") },
    )

    @Test
    fun `theme defaults to system when nothing is saved`() = runTest {
        val repository = SettingsRepositoryImpl(dataStore())
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `saved theme is read back`() = runTest {
        val repository = SettingsRepositoryImpl(dataStore())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `unknown stored value falls back to the default`() = runTest {
        val store = dataStore()
        store.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA" }

        assertEquals(ThemeMode.SYSTEM, SettingsRepositoryImpl(store).themeMode.first())
    }
}
