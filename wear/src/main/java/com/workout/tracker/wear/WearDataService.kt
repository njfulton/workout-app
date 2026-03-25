package com.workout.tracker.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Service to receive data and messages from the phone app.
 * Handles workout sync between phone and watch.
 */
class WearDataService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataService"
        const val PATH_WORKOUT_START = "/workout/start"
        const val PATH_WORKOUT_UPDATE = "/workout/update"
        const val PATH_WORKOUT_END = "/workout/end"
        const val PATH_TIMER_CONTROL = "/workout/timer"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            Log.d(TAG, "Data changed: ${event.dataItem.uri}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_WORKOUT_START -> {
                // Phone started a workout - update watch UI
                val workoutData = String(messageEvent.data)
                Log.d(TAG, "Workout started: $workoutData")
            }
            PATH_WORKOUT_UPDATE -> {
                // Set logged on phone - update watch display
                val updateData = String(messageEvent.data)
                Log.d(TAG, "Workout updated: $updateData")
            }
            PATH_WORKOUT_END -> {
                Log.d(TAG, "Workout ended")
            }
            PATH_TIMER_CONTROL -> {
                val timerData = String(messageEvent.data)
                Log.d(TAG, "Timer control: $timerData")
            }
        }
    }
}
