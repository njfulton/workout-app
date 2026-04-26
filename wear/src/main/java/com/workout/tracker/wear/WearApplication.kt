package com.workout.tracker.wear

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Registers a runtime MessageClient listener as soon as the wear process
 * starts. This is the primary receive path — the manifest
 * [WearDataService] is a secondary fallback.
 *
 * On Pixel Watch 3 (and other current Wear OS builds) the
 * WearableListenerService binding is blocked by
 * BIND_WEARABLE_LISTENER_SERVICE permission denial, so relying on it
 * exclusively means messages silently drop. Runtime listeners are
 * registered from inside the app's own process and don't need that
 * permission to fire.
 */
class WearApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val listener = MessageClient.OnMessageReceivedListener { event ->
        val payload = runCatching { String(event.data) }.getOrDefault("")
        Log.d(TAG, "Runtime listener: ${event.path} -> $payload")
        WatchState.onAnyMessage(event.path)
        handleMessage(event.path, payload, event.sourceNodeId)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Wearable.getMessageClient(this).addListener(listener)
            Log.d(TAG, "MessageClient listener registered")
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

    private fun handleMessage(path: String, payload: String, sourceNodeId: String) {
        val json = runCatching { JSONObject(payload) }.getOrNull()
        when (path) {
            "/workout/start" -> WatchState.onWorkoutStart(
                name = json?.optString("name").orEmpty(),
                exercise = json?.optString("exercise").orEmpty(),
                setsDone = json?.optInt("setsDone", 0) ?: 0,
                totalSets = json?.optInt("totalSets", 0) ?: 0,
                targetWeight = json?.optDouble("targetWeight")?.takeIf { !it.isNaN() },
                targetReps = json?.optInt("targetReps", 0)?.takeIf { it > 0 }
            )
            "/workout/update" -> WatchState.onWorkoutUpdate(
                exercise = json?.optString("exercise").orEmpty(),
                setsDone = json?.optInt("setsDone", 0) ?: 0,
                totalSets = json?.optInt("totalSets", 0) ?: 0,
                lastSet = json?.optString("lastSet").orEmpty(),
                targetWeight = json?.optDouble("targetWeight")?.takeIf { !it.isNaN() },
                targetReps = json?.optInt("targetReps", 0)?.takeIf { it > 0 }
            )
            "/workout/timer" -> WatchState.onRestTimer(
                seconds = json?.optInt("seconds", 0) ?: 0,
                running = json?.optBoolean("running", false) ?: false
            )
            "/workout/end" -> WatchState.onWorkoutEnd()
            "/workout/ping" -> {
                // Diagnostic: reply with a pong straight back to the sender.
                scope.launch {
                    runCatching {
                        Wearable.getMessageClient(this@WearApplication)
                            .sendMessage(sourceNodeId, "/workout/pong", payload.toByteArray())
                            .await()
                        Log.d(TAG, "Sent pong to $sourceNodeId")
                    }.onFailure { Log.w(TAG, "Pong send failed: ${it.message}") }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WearApplication"
    }
}
