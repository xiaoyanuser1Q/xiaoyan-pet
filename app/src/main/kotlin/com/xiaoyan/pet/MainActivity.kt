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
    // Simple pink ball with two small leaves
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(color = Color(0xFFFFB7D5), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Eyes
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier
                .size(12.dp)
                .background(color = Color.Black, shape = CircleShape))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier
                .size(12.dp)
                .background(color = Color.Black, shape = CircleShape))
        }
    }
}
