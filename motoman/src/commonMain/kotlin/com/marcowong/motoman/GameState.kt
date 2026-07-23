package com.marcowong.motoman

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GameState(
    val speedKmh: Float = 0f,
    val gear: Int = 1,
    val cornerNotification: String? = null
)

class GameStateFlow {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    fun update(speedKmh: Float, gear: Int, cornerNotification: String?) {
        _state.value = GameState(speedKmh, gear, cornerNotification)
    }
}
