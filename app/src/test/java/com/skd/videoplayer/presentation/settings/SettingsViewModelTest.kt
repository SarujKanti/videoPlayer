package com.skd.videoplayer.presentation.settings

import com.skd.videoplayer.MainViewModel
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.testutil.FakeSettingsRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the saved theme and saves a new choice`() = runTest(dispatcher) {
        settings.mode.value = ThemeMode.DARK
        val viewModel = SettingsViewModel(settings)
        assertNull(viewModel.uiState.value.themeMode) // not read yet
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, settings.mode.value)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `main screen follows the theme setting`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVideoRepository(), settings)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        settings.mode.value = ThemeMode.DARK
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }
}
