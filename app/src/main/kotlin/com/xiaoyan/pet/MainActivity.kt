package com.xiaoyan.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.xiaoyan.pet.ui.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PetApp.init(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 回到前台: 离线衰减 + 恢复 BGM
        PetApp.onEnterApp()
    }

    override fun onDestroy() {
        runCatching { PetApp.audio.release() }
        super.onDestroy()
    }
}
