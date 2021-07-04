package com.radiantmood.anagramanimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radiantmood.anagramanimation.ui.theme.AnagramAnimationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnagramAnimationTheme {
                Surface(color = MaterialTheme.colors.background) {
                    // TODO: Textfield that powers input into anagram
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        var input by remember { mutableStateOf("@radiantmood") }
                        AnimatedAnagram(input)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = {
                            input = if (input == "@radiantmood") "atom@android" else "@radiantmood"
                        }) {
                            Text("Flip!")
                        }
                    }
                }
            }
        }
    }
}