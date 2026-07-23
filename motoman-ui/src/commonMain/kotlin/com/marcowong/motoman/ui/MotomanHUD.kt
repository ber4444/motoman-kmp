package com.marcowong.motoman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcowong.motoman.GameState

@Composable
fun MotomanHUD(state: GameState) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Top-left: Corner Notification
        val cornerNotification = state.cornerNotification
        if (cornerNotification != null) {
            Text(
                text = cornerNotification,
                color = Color.Yellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color(0x80000000))
                    .padding(8.dp)
            )
        }

        // Bottom-left: Gear
        Text(
            text = "GEAR: ${state.gear}",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // Bottom-right: Speed
        Text(
            text = "${state.speedKmh.toInt()} KM/H",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
