package com.workout.tracker.watch

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sends workout state from the phone to any paired Wear OS watches.
 * Uses capability-based node discovery so messages are delivered reliably
 * even though the phone and watch apps have different package names.
 * Failures are logged and swallowed so a missing/disconnected watch
 * never breaks the phone workout flow.
 */
object WatchSyncManager {
    private const val TAG = "WatchSyncManager"
    private const val WEAR_CAPABILITY = "workout_tracker_wear"

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
                val messageClient = Wearable.getMessageClient(context)
                val nodeIds = findTargetNodeIds(context)
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No reachable watch nodes for $path")
                    return@withContext
                }
                for (nodeId in nodeIds) {
                    try {
                        messageClient.sendMessage(nodeId, path, data.toByteArray()).await()
                        Log.d(TAG, "Sent $path to $nodeId")
                    } catch (e: Exception) {
                        Log.w(TAG, "sendMessage failed for $nodeId: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send $path to watch: ${e.message}")
        }
    }

    /**
     * Prefer nodes that advertise the watch app's capability; fall back to all
     * reachable connected nodes so something still gets delivered during setup
     * before the capability has been discovered.
     */
    private suspend fun findTargetNodeIds(context: Context): List<String> {
        val capabilityClient = Wearable.getCapabilityClient(context)
        val capable = try {
            capabilityClient
                .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
                .map { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "Capability lookup failed: ${e.message}")
            emptyList()
        }
        if (capable.isNotEmpty()) return capable

        return try {
            Wearable.getNodeClient(context).connectedNodes.await().map { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "connectedNodes fallback failed: ${e.message}")
            emptyList()
        }
    }
}
