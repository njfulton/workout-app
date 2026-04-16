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
import androidx.wear.ambient.AmbientLifecycleObserver
import com.workout.tracker.wear.ui.WearApp

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

        // Keeps the screen on while the app is the foreground activity.
        // Combined with AmbientLifecycleObserver, this means:
        //   - Active use: screen stays fully bright
        //   - Hand down / timeout: enters ambient (dim) instead of being
        //     replaced by the watch face
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycle.addObserver(ambientObserver)

        setContent {
            CompositionLocalProvider(LocalAmbientState provides ambient) {
                WearApp()
            }
        }
    }
}
