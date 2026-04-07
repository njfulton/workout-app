package com.workout.tracker.wear

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/**
 * Registers a runtime MessageClient listener as soon as the wear process
 * starts, so we don't depend solely on the manifest WearableListenerService
 * (which can be slow or unreliable to wake on some Wear OS builds).
 */
class WearApplication : Application() {

    private val listener = MessageClient.OnMessageReceivedListener { event ->
        val payload = runCatching { String(event.data) }.getOrDefault("")
        Log.d(TAG, "Runtime listener: ${event.path} -> $payload")
        handleMessage(event.path, payload)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Wearable.getMessageClient(this).addListener(listener)
            Log.d(TAG, "MessageClient listener registered")
            // Log local node id so we can verify the phone is sending to us
            Wearable.getNodeClient(this).localNode
                .addOnSuccessListener { node ->
                    Log.d(TAG, "Local node id=${node.id} name=${node.displayName}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "localNode failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register listener: ${e.message}")
        }
    }

    private fun handleMessage(path: String, payload: String) {
        val json = runCatching { JSONObject(payload) }.getOrNull()
        when (path) {
            "/workout/start" -> WatchState.onWorkoutStart(
                name = json?.optString("name").orEmpty(),
                exercise = json?.optString("exercise").orEmpty(),
                setsDone = json?.optInt("setsDone", 0) ?: 0,
                totalSets = json?.optInt("totalSets", 0) ?: 0
            )
            "/workout/update" -> WatchState.onWorkoutUpdate(
                exercise = json?.optString("exercise").orEmpty(),
                setsDone = json?.optInt("setsDone", 0) ?: 0,
                totalSets = json?.optInt("totalSets", 0) ?: 0,
                lastSet = json?.optString("lastSet").orEmpty()
            )
            "/workout/timer" -> WatchState.onRestTimer(
                seconds = json?.optInt("seconds", 0) ?: 0,
                running = json?.optBoolean("running", false) ?: false
            )
            "/workout/end" -> WatchState.onWorkoutEnd()
        }
    }

    companion object {
        private const val TAG = "WearApplication"
    }
}
