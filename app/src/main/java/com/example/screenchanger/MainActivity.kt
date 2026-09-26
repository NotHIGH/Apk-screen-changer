package com.example.screenchanger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private data class ScreenRatio(val label: String, val value: Float)

private val ratioPresets = listOf(
    ScreenRatio("4:3", 4f / 3f),
    ScreenRatio("5:4", 5f / 4f),
    ScreenRatio("3:2", 3f / 2f),
    ScreenRatio("16:10", 16f / 10f),
    ScreenRatio("16:9", 16f / 9f),
    ScreenRatio("18:9", 2f),
    ScreenRatio("19.5:9", 19.5f / 9f),
    ScreenRatio("20:9", 20f / 9f)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ScreenChangerApp() }
    }
}

@androidx.compose.runtime.Composable
private fun ScreenChangerApp() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val currentRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val current = ScreenRatio("Поточне", currentRatio)
    val options = ratioPresets.filter { it.value <= currentRatio + 0.02f } + current
    var selected by remember { mutableStateOf(current) }
    var applied by remember { mutableStateOf(current) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7F4)) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Screen Changer", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Налаштуйте співвідношення робочої області застосунку",
                    color = Color(0xFF53605A)
                )
                RatioPreview(selected, screenWidth, screenHeight)
                Text("Співвідношення", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options.distinctBy { it.label }) { option ->
                        FilterChip(
                            selected = selected.label == option.label,
                            onClick = { selected = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Зараз застосовано", color = Color(0xFF53605A), fontSize = 13.sp)
                        Text("${applied.label}  ${dimensions(applied, screenWidth, screenHeight)}")
                    }
                    Button(onClick = { applied = selected }) { Text("Застосувати") }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RatioPreview(ratio: ScreenRatio, width: Int, height: Int) {
    val previewRatio = ratio.value.coerceIn(1f, 2.5f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(previewRatio)
                .background(Color(0xFF18342A), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ratio.label, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(dimensions(ratio, width, height), color = Color(0xFFB8D3C4))
            }
        }
        Text("Preview робочої області", color = Color(0xFF53605A), fontSize = 13.sp)
    }
}

private fun dimensions(ratio: ScreenRatio, width: Int, height: Int): String {
    val resultWidth: Int
    val resultHeight: Int
    if (width.toFloat() / height.toFloat() >= ratio.value) {
        resultWidth = width
        resultHeight = (width / ratio.value).roundToInt()
    } else {
        resultHeight = height
        resultWidth = (height * ratio.value).roundToInt()
    }
    return "${resultWidth} x ${resultHeight} dp"
}