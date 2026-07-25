package com.marcowong.motoman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcowong.motoman.GameState
import com.marcowong.motoman.InputState

/**
 * Steering travel for full lock, as a fraction of the overlay's width. Derived from the
 * surface the gesture actually happens on rather than passed in per platform, so a drag of
 * the same relative distance steers the same amount on a phone, a tablet, and a desktop
 * window — and so Android and iOS cannot drift apart.
 */
private const val FULL_LOCK_WIDTH_FRACTION = 0.25f

@Composable
fun CommonGameOverlay(state: GameState, inputState: InputState) {
    Box(
        Modifier.fillMaxSize()
    ) {
        // Touch-drag steering (the only steering input): press anywhere and slide.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val fullLockPx = size.width * FULL_LOCK_WIDTH_FRACTION
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        inputState.steer = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            val dx = change.position.x - down.position.x
                            inputState.steer = (dx / fullLockPx).coerceIn(-1f, 1f)
                            change.consume()
                        }
                        inputState.steer = 0f
                    }
                }
        )

        MotomanHUD(state = state)

        // Boost button, lower-left: full throttle for a limited time.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .size(96.dp)
                .background(Color(0x66FFFFFF), CircleShape)
                .clickable {
                    inputState.boostPressed = true
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "BOOST",
                style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}
