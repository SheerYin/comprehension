package me.yin.comprehension

import com.mojang.blaze3d.platform.InputConstants
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import org.lwjgl.sdl.SDLKeyboard
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ErrorReply(val error: String)

@Serializable
data class MouseState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val grabbed: Boolean,
    val screen: String?,
    val overlay: String?
)

@Serializable
data class MoveRequest(val x: Double, val y: Double)

@Serializable
data class DragRequest(val button: String = "left", val path: List<List<Double>>)

@Serializable
data class ScrollRequest(val dx: Double = 0.0, val dy: Double = 0.0)

@Serializable
data class ButtonRequest(val button: String = "left", val count: Int = 1)

@Serializable
data class KeyState(
    val held: List<String>,
    val screen: String?,
    val overlay: String?
)

@Serializable
data class EffectsState(
    val clipboard: String,
    val effects: List<SideEffect>
)

@Serializable
data class KeyRequest(val key: String)

@Serializable
data class TypeRequest(val text: String)

class ControlServer(port: Int) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    private val timeout = 5.seconds
    private val minecraft = Minecraft.getInstance()
    private val clientThread = minecraft.asCoroutineDispatcher()

    private val server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        install(StatusPages) {
            exception<TimeoutCancellationException> { call, _ ->
                call.respondText(json.encodeToString(ErrorReply("Client did not respond within $timeout")), ContentType.Application.Json, HttpStatusCode.GatewayTimeout)
            }
            exception<IllegalArgumentException> { call, cause ->
                call.respondText(json.encodeToString(ErrorReply(cause.message ?: "Bad request")), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
            exception<Throwable> { call, cause ->
                call.respondText(json.encodeToString(ErrorReply(cause.message ?: cause.javaClass.name)), ContentType.Application.Json, HttpStatusCode.InternalServerError)
            }
        }

        routing {
            get("/mouse") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/move") {
                val request = json.decodeFromString<MoveRequest>(call.receive<ByteArray>().decodeToString())
                stepThenSettle {
                    requireInFrame(request.x, request.y)
                    moveCursor(request.x, request.y)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/drag") {
                val request = json.decodeFromString<DragRequest>(call.receive<ByteArray>().decodeToString())
                val button = parseButton(request.button)
                require(request.path.size >= 2 && request.path.all { it.size == 2 }) {
                    "path needs at least two [x, y] points"
                }
                val (startX, startY) = request.path.first()
                stepThenSettle {
                    for ((x, y) in request.path) {
                        requireInFrame(x, y)
                    }
                    moveCursor(startX, startY)
                }
                stepThenSettle {
                    minecraft.mouseHandler.onButton(minecraft.window.handle(), MouseButtonInfo(button, VirtualKeyboard.modifiers()), 1)
                }
                stepThenSettle {
                    val nudgeX = if (startX + 1 < minecraft.window.width) startX + 1 else startX - 1
                    moveCursor(nudgeX, startY)
                }
                for ((x, y) in request.path.drop(1)) {
                    stepThenSettle { moveCursor(x, y) }
                }
                stepThenSettle {
                    minecraft.mouseHandler.onButton(minecraft.window.handle(), MouseButtonInfo(button, VirtualKeyboard.modifiers()), 0)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/press") {
                val request = json.decodeFromString<ButtonRequest>(call.receive<ByteArray>().decodeToString())
                val button = parseButton(request.button)
                stepThenSettle {
                    minecraft.mouseHandler.onButton(minecraft.window.handle(), MouseButtonInfo(button, VirtualKeyboard.modifiers()), 1)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/release") {
                val request = json.decodeFromString<ButtonRequest>(call.receive<ByteArray>().decodeToString())
                val button = parseButton(request.button)
                stepThenSettle {
                    minecraft.mouseHandler.onButton(minecraft.window.handle(), MouseButtonInfo(button, VirtualKeyboard.modifiers()), 0)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/scroll") {
                val request = json.decodeFromString<ScrollRequest>(call.receive<ByteArray>().decodeToString())
                require(request.dx != 0.0 || request.dy != 0.0) { "Give a non-zero dx or dy" }
                require(kotlin.math.abs(request.dx) <= 50 && kotlin.math.abs(request.dy) <= 50) { "dx and dy must be between -50 and 50" }
                stepThenSettle {
                    minecraft.mouseHandler.onScroll(minecraft.window.handle(), request.dx, request.dy)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/mouse/click") {
                val request = json.decodeFromString<ButtonRequest>(call.receive<ByteArray>().decodeToString())
                val button = parseButton(request.button)
                require(request.count in 1..3) { "count must be 1, 2 or 3" }
                stepThenSettle {
                    val handle = minecraft.window.handle()
                    repeat(request.count) {
                        minecraft.mouseHandler.onButton(handle, MouseButtonInfo(button, VirtualKeyboard.modifiers()), 1)
                        minecraft.mouseHandler.onButton(handle, MouseButtonInfo(button, VirtualKeyboard.modifiers()), 0)
                    }
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { mouseState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/key") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/key/press") {
                val request = json.decodeFromString<KeyRequest>(call.receive<ByteArray>().decodeToString())
                val scancode = parseKey(request.key)
                stepThenSettle {
                    VirtualKeyboard.held.add(scancode)
                    val modifiers = VirtualKeyboard.modifiers()
                    val keycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, modifiers.toShort(), true)
                    minecraft.keyboardHandler.keyPress(minecraft.window.handle(), 1, KeyEvent(scancode, keycode, modifiers))
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/key/release") {
                val request = json.decodeFromString<KeyRequest>(call.receive<ByteArray>().decodeToString())
                val scancode = parseKey(request.key)
                stepThenSettle {
                    VirtualKeyboard.held.remove(scancode)
                    val modifiers = VirtualKeyboard.modifiers()
                    val keycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, modifiers.toShort(), true)
                    minecraft.keyboardHandler.keyPress(minecraft.window.handle(), 0, KeyEvent(scancode, keycode, modifiers))
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/key/tap") {
                val request = json.decodeFromString<KeyRequest>(call.receive<ByteArray>().decodeToString())
                val scancode = parseKey(request.key)
                stepThenSettle {
                    val handle = minecraft.window.handle()
                    VirtualKeyboard.held.add(scancode)
                    val pressModifiers = VirtualKeyboard.modifiers()
                    val pressKeycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, pressModifiers.toShort(), true)
                    minecraft.keyboardHandler.keyPress(handle, 1, KeyEvent(scancode, pressKeycode, pressModifiers))
                    VirtualKeyboard.held.remove(scancode)
                    val releaseModifiers = VirtualKeyboard.modifiers()
                    val releaseKeycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, releaseModifiers.toShort(), true)
                    minecraft.keyboardHandler.keyPress(handle, 0, KeyEvent(scancode, releaseKeycode, releaseModifiers))
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/key/release-all") {
                stepThenSettle {
                    val handle = minecraft.window.handle()
                    for (scancode in VirtualKeyboard.held.toList()) {
                        VirtualKeyboard.held.remove(scancode)
                        val modifiers = VirtualKeyboard.modifiers()
                        val keycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, modifiers.toShort(), true)
                        minecraft.keyboardHandler.keyPress(handle, 0, KeyEvent(scancode, keycode, modifiers))
                    }
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/key/type") {
                val request = json.decodeFromString<TypeRequest>(call.receive<ByteArray>().decodeToString())
                stepThenSettle {
                    minecraft.keyboardHandler.textInput(minecraft.window.handle(), request.text)
                }
                val state = withTimeout(timeout) {
                    withContext(clientThread) { keyState() }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/effects") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        EffectsState(SideEffects.clipboard, SideEffects.records.toList())
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            post("/effects/clear") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        SideEffects.records.clear()
                        EffectsState(SideEffects.clipboard, SideEffects.records.toList())
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/screenshot") {
                val parameters = call.request.queryParameters
                val regionValues = listOf("x", "y", "w", "h").map { parameters[it]?.toIntOrNull() }
                val scale = parameters["scale"]?.toIntOrNull() ?: 1
                require(regionValues.all { it == null } || regionValues.all { it != null }) {
                    "Give all of x, y, w, h to crop, or none of them"
                }
                require(scale in 1..8) { "scale must be between 1 and 8" }
                val pending = withTimeout(timeout) {
                    withContext(clientThread) {
                        val window = minecraft.window
                        var region: ScreenshotRegion? = null
                        if (regionValues[0] != null) {
                            val (x, y, w, h) = regionValues.map { it!! }
                            require(x >= 0 && y >= 0 && w > 0 && h > 0 && x + w <= window.width && y + h <= window.height) {
                                "Crop ($x, $y, $w, $h) is outside the ${window.width}x${window.height} frame"
                            }
                            region = ScreenshotRegion(x, y, w, h, scale)
                        } else if (scale != 1) {
                            region = ScreenshotRegion(0, 0, window.width, window.height, scale)
                        }
                        Screenshots.request(region)
                    }
                }
                val png = try {
                    withTimeout(timeout) { pending.await() }
                } catch (exception: TimeoutCancellationException) {
                    pending.cancel()
                    throw exception
                }
                call.respondBytes(png, ContentType.Image.PNG)
            }
        }
    }.start(wait = false)

    private fun mouseState(): MouseState {
        val window = minecraft.window
        val mouse = minecraft.mouseHandler
        return MouseState(
            x = (mouse.xpos() * window.width / window.screenWidth).toInt(),
            y = (mouse.ypos() * window.height / window.screenHeight).toInt(),
            width = window.width,
            height = window.height,
            grabbed = mouse.isMouseGrabbed,
            screen = minecraft.gui.screen()?.javaClass?.name,
            overlay = minecraft.gui.overlay()?.javaClass?.name
        )
    }

    private suspend fun stepThenSettle(step: () -> Unit) {
        val frame = withTimeout(timeout) {
            withContext(clientThread) {
                step()
                Frames.afterTickAndFrame()
            }
        }
        withTimeout(timeout) { frame.await() }
    }

    private fun requireInFrame(x: Double, y: Double) {
        val window = minecraft.window
        require(x >= 0 && x < window.width && y >= 0 && y < window.height) {
            "Point ($x, $y) is outside the ${window.width}x${window.height} frame"
        }
    }

    private fun moveCursor(x: Double, y: Double) {
        val window = minecraft.window
        val mouse = minecraft.mouseHandler
        val xpos = x * window.screenWidth / window.width
        val ypos = y * window.screenHeight / window.height
        mouse.onMove(window.handle(), xpos, ypos, xpos - mouse.xpos(), ypos - mouse.ypos())
    }

    private fun keyState(): KeyState {
        val prefix = "key.keyboard."
        val held = VirtualKeyboard.held.map { InputConstants.Type.KEYBOARD.getOrCreate(it).name.removePrefix(prefix) }
        return KeyState(held = held, screen = minecraft.gui.screen()?.javaClass?.name, overlay = minecraft.gui.overlay()?.javaClass?.name)
    }

    private fun parseKey(name: String): Int {
        val prefix = "key.keyboard."
        val fullName = if (name.startsWith(prefix)) name else prefix + name
        val key = try {
            InputConstants.getKey(fullName)
        } catch (exception: NumberFormatException) {
            throw IllegalArgumentException("Unknown key '$name', expected a Minecraft key name such as e, escape, left.shift, enter, f3")
        }
        require(key.type == InputConstants.Type.KEYBOARD && key != InputConstants.UNKNOWN) {
            "Unknown key '$name', expected a Minecraft key name such as e, escape, left.shift, enter, f3"
        }
        return key.value
    }

    private fun parseButton(name: String): Int =
        when (name) {
            "left" -> InputConstants.MOUSE_BUTTON_LEFT
            "right" -> InputConstants.MOUSE_BUTTON_RIGHT
            "middle" -> InputConstants.MOUSE_BUTTON_MIDDLE
            else -> throw IllegalArgumentException("Unknown button '$name', expected left, right or middle")
        }

    private suspend fun respondJson(call: ApplicationCall, body: String) {
        call.respondText(body, ContentType.Application.Json)
    }

    fun stop() {
        server.stop(1000, 2000)
    }
}
