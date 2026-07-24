package com.marcowong.motoman.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import com.marcowong.motoman.IosGameHost
import platform.UIKit.UIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import androidx.compose.ui.viewinterop.UIKitInteropProperties

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
fun MainViewController(gameViewController: UIViewController, host: IosGameHost): UIViewController = ComposeUIViewController {
    Box(Modifier.fillMaxSize()) {
        UIKitViewController(
            factory = { gameViewController },
            modifier = Modifier.fillMaxSize(),
            update = { },
            properties = UIKitInteropProperties(
                isInteractive = false,
                isNativeAccessibilityEnabled = false,
            ),
        )
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                val fullLockPx = size.width / 4f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    host.setSteer(0f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        val dx = change.position.x - down.position.x
                        host.setSteer((dx / fullLockPx).coerceIn(-1f, 1f))
                        change.consume()
                    }
                    host.setSteer(0f)
                }
            }
        )
        val state by host.app.gameStateFlow.state.collectAsState()
        MotomanHUD(state)
    }
}

fun HudViewController(host: IosGameHost): UIViewController = ComposeUIViewController {
    val state by host.app.gameStateFlow.state.collectAsState()
    MotomanHUD(state)
}
