package com.example.homebudget.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SupabaseToastType {
    SUCCESS,
    WARNING
}

data class SupabaseToastEvent(
    val message: String,
    val type: SupabaseToastType
)

object SupabaseToastBus {
    private val _events = MutableSharedFlow<SupabaseToastEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var lastMessage: String? = null
    private var lastShownAt: Long = 0L

    fun showSynced() {
        show(
            message = "Supabase: połączono i zsynchronizowano dane.",
            type = SupabaseToastType.SUCCESS
        )
    }

    fun showQueued() {
        show(
            message = "Supabase: brak połączenia. Dane zapisano lokalnie i zostaną wysłane później.",
            type = SupabaseToastType.WARNING
        )
    }

    private fun show(message: String, type: SupabaseToastType) {
        val now = System.currentTimeMillis()
        if (lastMessage == message && now - lastShownAt < 4_000L) return

        lastMessage = message
        lastShownAt = now
        _events.tryEmit(SupabaseToastEvent(message, type))
    }
}
