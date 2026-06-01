package com.workout.tracker.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Receives messages from the phone and updates [WatchState] so the UI reflects
 * the phone's active workout in real time.
 */
class WearDataService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataService"
        const val PATH_WORKOUT_START = "/workout/start"
        const val PATH_WORKOUT_UPDATE = "/workout/update"
        const val PATH_WORKOUT_END = "/workout/end"
        const val PATH_TIMER_CONTROL = "/workout/timer"
        const val PATH_PING = "/workout/ping"
        const val PATH_PONG = "/workout/pong"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val payload = runCatching { String(messageEvent.data) }.getOrDefault("")
        Log.d(TAG, "Message received: ${messageEvent.path} -> $payload")
        WatchState.onAnyMessage(messageEvent.path)

        when (messageEvent.path) {
            PATH_WORKOUT_START -> {
                val json = parse(payload)
                WatchState.onWorkoutStart(
                    name = json?.optString("name").orEmpty(),
                    exercise = json?.optString("exercise").orEmpty(),
                    setsDone = json?.optInt("setsDone", 0) ?: 0,
                    totalSets = json?.optInt("totalSets", 0) ?: 0,
                    targetWeight = json?.optDouble("targetWeight")?.takeIf { !it.isNaN() },
                    targetReps = json?.optInt("targetReps", 0)?.takeIf { it > 0 }
                )
            }
            PATH_WORKOUT_UPDATE -> {
                val json = parse(payload)
                WatchState.onWorkoutUpdate(
                    exercise = json?.optString("exercise").orEmpty(),
                    setsDone = json?.optInt("setsDone", 0) ?: 0,
                    totalSets = json?.optInt("totalSets", 0) ?: 0,
                    lastSet = json?.optString("lastSet").orEmpty(),
                    targetWeight = json?.optDouble("targetWeight")?.takeIf { !it.isNaN() },
                    targetReps = json?.optInt("targetReps", 0)?.takeIf { it > 0 }
                )
            }
            PATH_TIMER_CONTROL -> {
                val json = parse(payload)
                WatchState.onRestTimer(
                    seconds = json?.optInt("seconds", 0) ?: 0,
                    running = json?.optBoolean("running", false) ?: false
                )
            }
            PATH_WORKOUT_END -> {
                WatchState.onWorkoutEnd()
            }
            PATH_PING -> {
                // Round-trip test: reply immediately with a pong to the sender.
                val senderId = messageEvent.sourceNodeId
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderId, PATH_PONG, payload.toByteArray())
                            .await()
                        Log.d(TAG, "Sent pong to $senderId")
                    }.onFailure { Log.w(TAG, "Pong send failed: ${it.message}") }
                }
            }
        }
    }

    private fun parse(data: String): JSONObject? = runCatching { JSONObject(data) }.getOrNull()
}
