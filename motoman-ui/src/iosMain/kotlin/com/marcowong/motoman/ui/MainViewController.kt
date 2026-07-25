package com.marcowong.motoman.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
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
        val state by host.app.gameStateFlow.state.collectAsState()
        CommonGameOverlay(state = state, inputState = host.uiInputState)
    }
}
