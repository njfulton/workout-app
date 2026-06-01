package com.workout.tracker.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import com.workout.tracker.wear.ui.WearApp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Signals whether the watch is currently in ambient (low-power) mode.
 * UI composables can choose to render a simpler high-contrast layout
 * when ambient is true.
 */
val LocalAmbientState = staticCompositionLocalOf { false }

class WearMainActivity : ComponentActivity() {

    private var ambient by mutableStateOf(false)

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                ambient = true
            }

            override fun onExitAmbient() {
                ambient = false
            }

            override fun onUpdateAmbient() {
                // Called once per minute while ambient. Our UI pulls fresh
                // state on recompose so nothing to do here.
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(ambientObserver)

        // Drive both screen-on and activity lifecycle from the watch state:
        //  - While a workout or rest timer is active, keep the screen on
        //    so you can read the display without tapping the wrist.
        //  - When the active session ends (transition active->idle), close
        //    the activity so the user returns to the watch face instead of
        //    being left on the idle "Start a workout on your phone" screen.
        //  - If the user opens the app manually without an active workout,
        //    nothing changes — screen dims normally, no auto-close.
        lifecycleScope.launch {
            var wasActive = false
            WatchState.state
                .map { it.isActive || it.restRunning }
                .distinctUntilChanged()
                .collect { active ->
                    if (active) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (wasActive) finish()
                    }
                    wasActive = active
                }
        }

        setContent {
            CompositionLocalProvider(LocalAmbientState provides ambient) {
                WearApp()
            }
        }
    }
}
