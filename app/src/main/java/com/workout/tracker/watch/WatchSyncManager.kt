package com.workout.tracker.watch

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sends workout state from the phone to any paired Wear OS watches.
 * Uses the Wearable MessageClient. Failures are logged and swallowed so
 * missing/disconnected watches never break the phone workout flow.
 */
object WatchSyncManager {
    private const val TAG = "WatchSyncManager"
    const val PATH_WORKOUT_START = "/workout/start"
    const val PATH_WORKOUT_UPDATE = "/workout/update"
    const val PATH_WORKOUT_TIMER = "/workout/timer"
    const val PATH_WORKOUT_END = "/workout/end"

    suspend fun sendWorkoutStart(
        context: Context,
        workoutName: String,
        exerciseName: String?,
        setsDone: Int,
        totalSets: Int?
    ) {
        val payload = JSONObject().apply {
            put("name", workoutName)
            put("exercise", exerciseName ?: "")
            put("setsDone", setsDone)
            put("totalSets", totalSets ?: 0)
        }
        sendMessage(context, PATH_WORKOUT_START, payload.toString())
    }

    suspend fun sendWorkoutUpdate(
        context: Context,
        exerciseName: String?,
        setsDone: Int,
        totalSets: Int?,
        lastSet: String?
    ) {
        val payload = JSONObject().apply {
            put("exercise", exerciseName ?: "")
            put("setsDone", setsDone)
            put("totalSets", totalSets ?: 0)
            put("lastSet", lastSet ?: "")
        }
        sendMessage(context, PATH_WORKOUT_UPDATE, payload.toString())
    }

    suspend fun sendRestTimer(context: Context, seconds: Int, running: Boolean) {
        val payload = JSONObject().apply {
            put("seconds", seconds)
            put("running", running)
        }
        sendMessage(context, PATH_WORKOUT_TIMER, payload.toString())
    }

    suspend fun sendWorkoutEnd(context: Context) {
        sendMessage(context, PATH_WORKOUT_END, "")
    }

    private suspend fun sendMessage(context: Context, path: String, data: String) {
        try {
            withContext(Dispatchers.IO) {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, data.toByteArray()).await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send $path to watch: ${e.message}")
        }
    }
}
