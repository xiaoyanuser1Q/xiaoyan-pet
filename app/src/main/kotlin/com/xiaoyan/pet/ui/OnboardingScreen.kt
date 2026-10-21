package com.xiaoyan.pet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.game.Story

/** 开场: 起名 + 想找回的东西 */
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var thing by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("小桃养成记", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(Story.GREET, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))

        Text("给它起个名字", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(12) },
            label = { Text("名字", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text("你最想找回的东西", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(4.dp))
        Text("它会一直记着。", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = thing,
            onValueChange = { thing = it.take(30) },
            label = { Text("想找回的东西", fontSize = 12.sp) },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            text = "开始相伴",
            enabled = name.isNotBlank(),
            onClick = {
                PetApp.audio.playSfx("sfx_levelup")
                PetApp.startGame(name.trim(), thing.trim())
            },
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** 深色背景上的输入框配色 */
@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.10f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
    focusedBorderColor = Color(0xFFFFB7D5),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = Color(0xFFFFB7D5),
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
)
