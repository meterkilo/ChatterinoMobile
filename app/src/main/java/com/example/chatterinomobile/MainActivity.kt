package com.example.chatterinomobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.chatterinomobile.data.repository.EmoteRepository
import com.example.chatterinomobile.ui.theme.ChatterinoMobileTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val emoteRepository: EmoteRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            Log.d("EmoteRepo", "Loading emotes...")
            emoteRepository.loadEmotesForChannel(channelId = null)

            // Test lookups across all three providers:
            // RainTime, PETPET, Clap → 7TV
            // FeelsDankMan, catKISS, KEKWait → BTTV
            // ZreknarF, MonkaS, PogChamp → FFZ
            listOf(
                "RainTime", "PETPET", "Clap",
                "FeelsDankMan", "catKISS",
                "ZreknarF", "MonkaS",
                "DoesNotExist"
            ).forEach { name ->
                val emote = emoteRepository.findEmote(name)
                Log.d("EmoteRepo", "$name → ${emote?.provider ?: "NOT FOUND"} / ${emote?.urls?.x1 ?: "-"}")
            }
        }

        setContent {
            ChatterinoMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChatterinoMobileTheme {
        Greeting("Android")
    }
}