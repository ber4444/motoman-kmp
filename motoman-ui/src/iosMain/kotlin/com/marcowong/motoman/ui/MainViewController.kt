package com.marcowong.motoman.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.marcowong.motoman.IosGameHost
import platform.UIKit.UIViewController
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun MainViewController(gameViewController: UIViewController, host: IosGameHost): UIViewController = ComposeUIViewController {
    Box {
        UIKitViewController(
            factory = { gameViewController },
            modifier = Modifier.fillMaxSize(),
            update = { }
        )
        val state by host.app.gameStateFlow.state.collectAsState()
        MotomanHUD(state)
    }
}

fun HudViewController(host: IosGameHost): UIViewController = ComposeUIViewController {
    val state by host.app.gameStateFlow.state.collectAsState()
    MotomanHUD(state)
}
