package com.workout.tracker.watch

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * Phone-side diagnostic state for the Wear OS link. Powered by
 * [WatchSyncManager] (which records every send attempt) and
 * [PhoneWearListenerService] (which records every received pong).
 *
 * The diagnostics screen reads this + calls [snapshotNodes] to refresh
 * the connected/capable node lists on demand.
 */
data class WatchNodeInfo(
    val id: String,
    val name: String,
    val isNearby: Boolean,
    val hasCapability: Boolean
)

data class WatchDiagnosticSnapshot(
    val connectedNodes: List<WatchNodeInfo> = emptyList(),
    val capableNodes: List<WatchNodeInfo> = emptyList(),
    val snapshotTs: Long = 0L,
    val snapshotError: String? = null
)

data class WatchSendRecord(
    val path: String,
    val ts: Long,
    val success: Boolean,
    val targetCount: Int,
    val error: String? = null
)

object WatchDiagnostics {
    private const val WEAR_CAPABILITY = "workout_tracker_wear"

    private val _nodeSnapshot = MutableStateFlow(WatchDiagnosticSnapshot())
    val nodeSnapshot: StateFlow<WatchDiagnosticSnapshot> = _nodeSnapshot

    private val _lastSend = MutableStateFlow<WatchSendRecord?>(null)
    val lastSend: StateFlow<WatchSendRecord?> = _lastSend

    private val _lastPongTs = MutableStateFlow(0L)
    val lastPongTs: StateFlow<Long> = _lastPongTs

    fun recordSend(path: String, success: Boolean, targetCount: Int, error: String? = null) {
        _lastSend.value = WatchSendRecord(path, System.currentTimeMillis(), success, targetCount, error)
    }

    fun recordPong() {
        _lastPongTs.value = System.currentTimeMillis()
    }

    /** Refreshes connected + capable node lists. Called when the diagnostics screen opens. */
    suspend fun snapshotNodes(context: Context) {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val capabilityClient = Wearable.getCapabilityClient(context)

            val connected = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
            val capableIds = runCatching {
                capabilityClient.getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await().nodes.map { it.id }.toSet()
            }.getOrDefault(emptySet())

            val capableNodes = runCatching {
                capabilityClient.getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await().nodes.map {
                        WatchNodeInfo(it.id, it.displayName, it.isNearby, hasCapability = true)
                    }
            }.getOrDefault(emptyList())

            val connectedInfos = connected.map {
                WatchNodeInfo(it.id, it.displayName, it.isNearby, hasCapability = it.id in capableIds)
            }

            _nodeSnapshot.value = WatchDiagnosticSnapshot(
                connectedNodes = connectedInfos,
                capableNodes = capableNodes,
                snapshotTs = System.currentTimeMillis(),
                snapshotError = null
            )
        } catch (e: Exception) {
            _nodeSnapshot.value = WatchDiagnosticSnapshot(
                snapshotTs = System.currentTimeMillis(),
                snapshotError = e.message ?: e::class.java.simpleName
            )
        }
    }
}
