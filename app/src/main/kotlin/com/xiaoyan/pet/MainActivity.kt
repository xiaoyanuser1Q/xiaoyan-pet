package com.xiaoyan.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Simple Xiaotao SVG-like shape (pink ball with leaves)
        XiaotaoCompanion()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "小桃养成记",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "小桃说：「啾~」",
            fontSize = 16.sp,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "v1.0.0-mvp · MVP 建设中",
            fontSize = 12.sp,
            color = Color.LightGray,
        )
    }
}

@Composable
fun XiaotaoCompanion() {
    // Pink ball with two leaves on top, two eyes side by side, two rosy cheeks
    Box(
        modifier = Modifier
            .size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Two small leaves on top
        Canvas(modifier = Modifier
            .size(160.dp)) {
            // Left leaf
            val leafW = 24.dp.toPx()
            val leafH = 32.dp.toPx()
            val leftLeafCx = size.width / 2 - 20.dp.toPx()
            val leftLeafCy = 8.dp.toPx()
            drawCircle(
                color = Color(0xFF7BC67E),
                center = Offset(leftLeafCx, leftLeafCy),
                radius = leafW / 2
            )
            // Right leaf
            val rightLeafCx = size.width / 2 + 20.dp.toPx()
            drawCircle(
                color = Color(0xFF7BC67E),
                center = Offset(rightLeafCx, leftLeafCy),
                radius = leafW / 2
            )
        }

        // Pink ball
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(color = Color(0xFFFFB7D5), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Face: two eyes + two cheeks in a Row
            Column(verticalArrangement = Arrangement.Center) {
                // Eyes side by side
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier
                        .size(14.dp)
                        .background(color = Color.Black, shape = CircleShape))
                    Spacer(modifier = Modifier.width(32.dp))
                    Box(modifier = Modifier
                        .size(14.dp)
                        .background(color = Color.Black, shape = CircleShape))
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Cheeks side by side
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier
                        .size(16.dp)
                        .background(color = Color(0xFFFF6994), shape = CircleShape))
                    Spacer(modifier = Modifier.width(40.dp))
                    Box(modifier = Modifier
                        .size(16.dp)
                        .background(color = Color(0xFFFF6994), shape = CircleShape))
                }
            }
        }
    }
}
