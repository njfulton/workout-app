package com.workout.tracker.watch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Simple event bus for messages coming in from the watch so that
 * [PhoneWearListenerService] can hand them off to any active
 * WorkoutViewModel without the service needing a direct reference.
 */
object WatchEventBus {
    sealed interface Event {
        object LogSetRequested : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events

    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}
