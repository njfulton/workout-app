package com.workout.tracker.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Sends messages from the watch to the phone (currently just "log the next set").
 */
object WatchMessageSender {
    private const val TAG = "WatchMessageSender"
    const val PATH_LOG_SET = "/workout/log_set"

    suspend fun requestLogSet(context: Context) {
        sendMessage(context, PATH_LOG_SET, "")
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
            Log.w(TAG, "Failed to send $path to phone: ${e.message}")
        }
    }
}
