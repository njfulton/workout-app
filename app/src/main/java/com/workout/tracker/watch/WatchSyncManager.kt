package com.workout.tracker.watch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    const val PATH_PING = "/workout/ping"

    /** Fire-and-forget ping used by the diagnostics screen. */
    suspend fun sendPing(context: Context) {
        sendMessage(context, PATH_PING, System.currentTimeMillis().toString())
    }

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
        // Launch the watch app first so it's running when the message arrives.
        // Failures are swallowed — the user is not blocked by watch availability.
        runCatching { launchWatchApp(context) }
            .onFailure { Log.w(TAG, "launchWatchApp failed: ${it.message}") }
        sendMessage(context, PATH_WORKOUT_START, payload.toString())
    }

    /**
     * Remotely starts the watch activity via an intent URI the watch
     * manifest handles (`workouttracker://start`). Used at workout start
     * so the user doesn't have to manually open the watch app.
     */
    private suspend fun launchWatchApp(context: Context) {
        val helper = RemoteActivityHelper(context, Executors.newSingleThreadExecutor())
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("workouttracker://start"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val nodeIds = findTargetNodeIds(context)
        if (nodeIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            for (nodeId in nodeIds) {
                try {
                    helper.startRemoteActivity(intent, nodeId).awaitUnit()
                    Log.d(TAG, "Launched watch activity on $nodeId")
                } catch (e: Exception) {
                    Log.w(TAG, "startRemoteActivity failed for $nodeId: ${e.message}")
                }
            }
        }
    }

    // RemoteActivityHelper returns a ListenableFuture<Void>; awaitUnit lets us
    // use it from suspend code without pulling in Guava's kotlinx-coroutines
    // bridge.
    private suspend fun ListenableFuture<Void>.awaitUnit() =
        suspendCancellableCoroutine<Unit> { cont ->
            addListener({
                try { get(); cont.resume(Unit) }
                catch (e: Exception) { cont.resumeWithException(e) }
            }, Runnable::run)
            cont.invokeOnCancellation { cancel(true) }
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
                    WatchDiagnostics.recordSend(path, success = false, targetCount = 0, error = "no reachable nodes")
                    return@withContext
                }
                var anySuccess = false
                var lastError: String? = null
                for (nodeId in nodeIds) {
                    try {
                        messageClient.sendMessage(nodeId, path, data.toByteArray()).await()
                        Log.d(TAG, "Sent $path to $nodeId")
                        anySuccess = true
                    } catch (e: Exception) {
                        Log.w(TAG, "sendMessage failed for $nodeId: ${e.message}")
                        lastError = e.message ?: e::class.java.simpleName
                    }
                }
                WatchDiagnostics.recordSend(path, success = anySuccess, targetCount = nodeIds.size, error = lastError)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send $path to watch: ${e.message}")
            WatchDiagnostics.recordSend(path, success = false, targetCount = 0, error = e.message)
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
                .also { nodes ->
                    nodes.forEach { Log.d(TAG, "Capable node: id=${it.id} name=${it.displayName} nearby=${it.isNearby}") }
                }
                .map { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "Capability lookup failed: ${e.message}")
            emptyList()
        }
        if (capable.isNotEmpty()) return capable

        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
                .also { nodes ->
                    nodes.forEach { Log.d(TAG, "Connected node: id=${it.id} name=${it.displayName}") }
                }
                .map { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "connectedNodes fallback failed: ${e.message}")
            emptyList()
        }
    }
}
