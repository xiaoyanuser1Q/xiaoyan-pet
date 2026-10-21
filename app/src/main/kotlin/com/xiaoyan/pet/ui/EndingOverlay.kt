package com.xiaoyan.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyan.pet.PetApp
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.game.Story

/**
 * 结局浮层: 等级到达 100 且 endingSeen = false 时弹出。
 * 结局B 需要先填入「想说的第一句话」才展示最终文案。
 */
@Composable
fun EndingOverlay() {
    val p = PetApp.pet ?: return
    if (p.ended.isEmpty() || p.endingSeen) return

    var word by remember { mutableStateOf("") }
    val needWord = p.ended == "B" && p.endingFirstWord.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            Story.endingTitle(p.ended),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            Story.endingText(p.ended, p.name, p.endingFirstWord),
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
        Spacer(Modifier.height(18.dp))

        if (needWord) {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it.take(20) },
                label = { Text("想说给她的第一句话", fontSize = 12.sp) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = "把这句话交给小桃",
                enabled = word.isNotBlank(),
                onClick = {
                    p.endingFirstWord = word.trim()
                    PetApp.repo.saveState(p)
                    PetApp.audio.playSfx("sfx_levelup")
                    PetApp.bump()
                },
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(
                    text = "继续相伴",
                    onClick = {
                        p.endingSeen = true
                        PetApp.repo.saveState(p)
                        PetApp.audio.playSfx("sfx_tap")
                        PetApp.bump()
                    },
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = "新的相伴",
                    onClick = {
                        PetApp.repo.clearState()
                        PetApp.pet = null
                        PetApp.audio.playSfx("sfx_levelup")
                        PetApp.bump()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
