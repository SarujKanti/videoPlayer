package com.skd.videoplayer.presentation.splash

import com.skd.videoplayer.MainViewModel
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.domain.repository.SettingsRepository
import com.skd.videoplayer.domain.usecase.GetFavoriteVideosUseCase
import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashGateTest {

    @Test
    fun `stays up until the app is ready`() {
        val gate = SplashGate(startedAtMs = 1_000)
        assertTrue(gate.shouldKeepOnScreen(isReady = false, nowMs = 1_100))
        assertFalse(gate.shouldKeepOnScreen(isReady = true, nowMs = 1_100))
    }

    @Test
    fun `gives up after the maximum hold time`() {
        val gate = SplashGate(startedAtMs = 1_000, maxHoldMs = 1_500)
        assertTrue(gate.shouldKeepOnScreen(isReady = false, nowMs = 2_499))
        assertFalse(gate.shouldKeepOnScreen(isReady = false, nowMs = 2_500))
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Lets the test decide exactly when the saved theme "finishes loading". */
    private class SlowSettings : SettingsRepository {
        val emissions = MutableSharedFlow<ThemeMode>(replay = 1)
        override val themeMode: Flow<ThemeMode> = emissions
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
    }

    @Test
    fun `main screen becomes ready once the saved theme is read`() = runTest(dispatcher) {
        val settings = SlowSettings()
        val videos = FakeVideoRepository()
        val viewModel = MainViewModel(videos, settings, GetFavoriteVideosUseCase(videos, FakeFavoritesRepository()))

        advanceUntilIdle()
        assertFalse(viewModel.isReady)

        settings.emissions.emit(ThemeMode.DARK)
        advanceUntilIdle()
        assertTrue(viewModel.isReady)
    }
}
