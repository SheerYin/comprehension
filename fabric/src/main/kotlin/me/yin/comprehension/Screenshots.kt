package me.yin.comprehension

import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import java.nio.file.Files

data class ScreenshotRegion(val x: Int, val y: Int, val width: Int, val height: Int, val scale: Int)

class ScreenshotRequest(val region: ScreenshotRegion?, val result: CompletableDeferred<ByteArray>)

object Screenshots {
    private val waiting = ArrayList<ScreenshotRequest>()
    private val encoder = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cursorShape = listOf(
        "X",
        "XX",
        "X.X",
        "X..X",
        "X...X",
        "X....X",
        "X.....X",
        "X......X",
        "X.......X",
        "X........X",
        "X.........X",
        "X......XXXXX",
        "X...X..X",
        "X..XX..X",
        "X.X  X..X",
        "XX   X..X",
        "X     X..X",
        "      X..X",
        "       XX"
    )

    fun request(region: ScreenshotRegion?): CompletableDeferred<ByteArray> {
        val result = CompletableDeferred<ByteArray>()
        waiting.add(ScreenshotRequest(region, result))
        return result
    }

    @JvmStatic
    fun onFrameRendered() {
        if (waiting.isEmpty()) {
            return
        }
        val requests = waiting.toList()
        waiting.clear()

        val minecraft = Minecraft.getInstance()
        val window = minecraft.window
        val mouse = minecraft.mouseHandler
        val showCursor = !mouse.isMouseGrabbed
        val cursorX = (mouse.xpos() * window.width / window.screenWidth).toInt()
        val cursorY = (mouse.ypos() * window.height / window.screenHeight).toInt()

        Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget()) { frame ->
            encoder.launch {
                frame.use {
                    if (showCursor) {
                        for ((row, line) in cursorShape.withIndex()) {
                            for ((column, cell) in line.withIndex()) {
                                val x = cursorX + column
                                val y = cursorY + row
                                if (cell == ' ' || x !in 0 until frame.width || y !in 0 until frame.height) {
                                    continue
                                }
                                frame.setPixelABGR(x, y, if (cell == 'X') 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                            }
                        }
                    }

                    for (request in requests) {
                        val png = runCatching {
                            val region = request.region
                            val output = if (region == null) {
                                frame
                            } else {
                                val cropped = NativeImage(region.width * region.scale, region.height * region.scale, false)
                                for (y in 0 until cropped.height) {
                                    for (x in 0 until cropped.width) {
                                        cropped.setPixel(x, y, frame.getPixel(region.x + x / region.scale, region.y + y / region.scale))
                                    }
                                }
                                cropped
                            }
                            val file = Files.createTempFile("comprehension-", ".png")
                            try {
                                output.writeToFile(file)
                                Files.readAllBytes(file)
                            } finally {
                                Files.deleteIfExists(file)
                                if (output !== frame) {
                                    output.close()
                                }
                            }
                        }
                        request.result.completeWith(png)
                    }
                }
            }
        }
    }
}
