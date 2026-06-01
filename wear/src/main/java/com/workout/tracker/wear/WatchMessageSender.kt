package com.workout.tracker.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Sends messages from the watch to the phone. Uses capability-based node
 * discovery so messages are delivered reliably across the different package
 * names of the phone and watch apps.
 */
object WatchMessageSender {
    private const val TAG = "WatchMessageSender"
    private const val PHONE_CAPABILITY = "workout_tracker_phone"
    const val PATH_LOG_SET = "/workout/log_set"

    suspend fun requestLogSet(context: Context) {
        sendMessage(context, PATH_LOG_SET, "")
    }

    private suspend fun sendMessage(context: Context, path: String, data: String) {
        try {
            withContext(Dispatchers.IO) {
                val messageClient = Wearable.getMessageClient(context)
                val nodeIds = findTargetNodeIds(context)
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No reachable phone nodes for $path")
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
            Log.w(TAG, "Failed to send $path to phone: ${e.message}")
        }
    }

    private suspend fun findTargetNodeIds(context: Context): List<String> {
        val capabilityClient = Wearable.getCapabilityClient(context)
        val capable = try {
            capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
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
