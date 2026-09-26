package com.example.screenchanger

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScreenRatioUserService : IDisplayRatioService.Stub() {
    override fun applyRatio(ratio: String): String {
        val targetRatio = when (ratio) {
            "16:9" -> 16f / 9f
            "4:3" -> 4f / 3f
            else -> return "ERROR|Невідоме співвідношення сторін"
        }

        val physicalOutput = runWm("size")
        if (physicalOutput.exitCode != 0) return "ERROR|${physicalOutput.output}"

        val physicalSize = Regex("Physical size:\\s*(\\d+)x(\\d+)")
            .find(physicalOutput.output)
            ?: return "ERROR|Не вдалося прочитати роздільність екрана"
        val physicalWidth = physicalSize.groupValues[1].toInt()
        val physicalHeight = physicalSize.groupValues[2].toInt()
        val shortSide = min(physicalWidth, physicalHeight)
        val longSide = max(physicalWidth, physicalHeight)
        val currentRatio = longSide.toFloat() / shortSide

        val targetLongSide: Int
        val targetShortSide: Int
        if (currentRatio > targetRatio) {
            targetShortSide = shortSide
            targetLongSide = (shortSide * targetRatio).roundToInt()
        } else {
            targetLongSide = longSide
            targetShortSide = (longSide / targetRatio).roundToInt()
        }

        val targetWidth = if (physicalWidth <= physicalHeight) targetShortSide else targetLongSide
        val targetHeight = if (physicalWidth <= physicalHeight) targetLongSide else targetShortSide
        val result = runWm("size ${targetWidth}x$targetHeight")
        return if (result.exitCode == 0) {
            "OK|${targetWidth}x$targetHeight"
        } else {
            "ERROR|${result.output}"
        }
    }

    override fun resetResolution(): String {
        val result = runWm("size reset")
        return if (result.exitCode == 0) "OK|reset" else "ERROR|${result.output}"
    }

    override fun destroy() {
        System.exit(0)
    }

    private fun runWm(arguments: String): CommandResult {
        val process = ProcessBuilder("/system/bin/sh", "-c", "wm $arguments")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return CommandResult(process.waitFor(), output)
    }

    private data class CommandResult(val exitCode: Int, val output: String)
}