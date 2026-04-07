package com.workout.tracker.watch

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives messages from the paired watch and routes them through [WatchEventBus]
 * so the active WorkoutViewModel can react.
 */
class PhoneWearListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "PhoneWearListener"
        const val PATH_LOG_SET = "/workout/log_set"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Received ${messageEvent.path} from watch")
        when (messageEvent.path) {
            PATH_LOG_SET -> WatchEventBus.emit(WatchEventBus.Event.LogSetRequested)
        }
    }
}
