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
import com.mojang.serialization.JsonOps
import kotlinx.serialization.json.JsonElement
import me.yin.comprehension.mixin.BossHealthOverlayAccessor
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
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
    val loading: Boolean
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
    val loading: Boolean
)

@Serializable
data class ClientState(
    val username: String,
    val server: String?,
    val inWorld: Boolean,
    val screen: String?,
    val loading: Boolean,
    val biome: String?,
    val timeOfDay: Long?,
    val gameTime: Long?,
    val raining: Boolean?,
    val thundering: Boolean?,
    val uuid: String?,
    val entityId: Int?,
    val x: Double?,
    val y: Double?,
    val z: Double?,
    val yaw: Float?,
    val pitch: Float?,
    val dimension: String?,
    val gameMode: String?,
    val health: Float?,
    val maxHealth: Float?,
    val food: Int?,
    val saturation: Float?,
    val xpLevel: Int?,
    val selectedSlot: Int?
)

@Serializable
data class EntityInfo(
    val id: Int,
    val uuid: String,
    val type: String,
    val name: String,
    val customName: JsonElement?,
    val x: Double,
    val y: Double,
    val z: Double,
    val distance: Double,
    val invisible: Boolean,
    val glowing: Boolean,
    val onFire: Boolean,
    val sneaking: Boolean,
    val pose: String,
    val vehicle: String?,
    val passengers: List<String>,
    val equipment: Map<String, JsonElement>,
    val health: Float?,
    val maxHealth: Float?,
    val text: JsonElement?,
    val item: JsonElement?,
    val block: String?
)

@Serializable
data class EntitiesState(val radius: Double, val entities: List<EntityInfo>)

@Serializable
data class SlotItem(val slot: Int, val item: JsonElement)

@Serializable
data class InventoryState(
    val selectedSlot: Int,
    val cursor: JsonElement?,
    val slots: List<SlotItem>
)

@Serializable
data class ChatMessage(
    val sequence: Long,
    val time: Long,
    val source: String,
    val text: String,
    val component: JsonElement?
)

@Serializable
data class ChatState(val latest: Long, val messages: List<ChatMessage>)

@Serializable
data class TitleMessage(
    val sequence: Long,
    val time: Long,
    val type: String,
    val text: String,
    val component: JsonElement?
)

@Serializable
data class TitleState(val latest: Long, val messages: List<TitleMessage>)

@Serializable
data class ActionbarMessage(
    val sequence: Long,
    val time: Long,
    val text: String,
    val component: JsonElement?
)

@Serializable
data class ActionbarState(val latest: Long, val messages: List<ActionbarMessage>)

@Serializable
data class ToastMessage(
    val sequence: Long,
    val time: Long,
    val type: String,
    val title: String?,
    val description: String?
)

@Serializable
data class ToastState(val latest: Long, val toasts: List<ToastMessage>)

@Serializable
data class BossBar(
    val id: String,
    val text: String,
    val component: JsonElement?,
    val progress: Float,
    val color: String,
    val style: String
)

@Serializable
data class BossBarState(val bars: List<BossBar>)

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
            exception<IllegalStateException> { call, cause ->
                call.respondText(json.encodeToString(ErrorReply(cause.message ?: "Not possible right now")), ContentType.Application.Json, HttpStatusCode.Conflict)
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

            get("/state") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val player = minecraft.player
                        val level = minecraft.level
                        ClientState(
                            username = minecraft.user.name,
                            server = minecraft.currentServer?.ip,
                            inWorld = player != null,
                            screen = minecraft.gui.screen()?.javaClass?.name,
                            loading = minecraft.gui.overlay() != null,
                            biome = if (player != null && level != null) level.getBiome(player.blockPosition()).unwrapKey().map { it.identifier().toString() }.orElse(null) else null,
                            timeOfDay = if (level != null && level.dimensionType().defaultClock().isPresent) level.defaultClockTime % 24000 else null,
                            gameTime = level?.gameTime,
                            raining = level?.isRaining,
                            thundering = level?.isThundering,
                            uuid = player?.uuid?.toString(),
                            entityId = player?.id,
                            x = player?.x,
                            y = player?.y,
                            z = player?.z,
                            yaw = player?.yRot,
                            pitch = player?.xRot,
                            dimension = player?.level()?.dimension()?.identifier()?.toString(),
                            gameMode = minecraft.gameMode?.playerMode?.serializedName,
                            health = player?.health,
                            maxHealth = player?.maxHealth,
                            food = player?.foodData?.foodLevel,
                            saturation = player?.foodData?.saturationLevel,
                            xpLevel = player?.experienceLevel,
                            selectedSlot = player?.inventory?.selectedSlot
                        )
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/inventory") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val player = minecraft.player ?: throw IllegalStateException("Not in a world")
                        val ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE)
                        val inventory = player.inventory
                        val slots = ArrayList<SlotItem>()
                        for (slot in 0 until inventory.containerSize) {
                            val stack = inventory.getItem(slot)
                            if (!stack.isEmpty) {
                                val encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()
                                slots.add(SlotItem(slot, Json.parseToJsonElement(encoded.toString())))
                            }
                        }
                        val carried = player.containerMenu.carried
                        var cursor: JsonElement? = null
                        if (!carried.isEmpty) {
                            cursor = Json.parseToJsonElement(ItemStack.CODEC.encodeStart(ops, carried).getOrThrow().toString())
                        }
                        InventoryState(inventory.selectedSlot, cursor, slots)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/entities") {
                val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 16.0
                require(radius > 0 && radius <= 128) { "radius must be greater than 0 and at most 128" }
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val player = minecraft.player ?: throw IllegalStateException("Not in a world")
                        val level = minecraft.level ?: throw IllegalStateException("Not in a world")
                        val ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE)
                        val found = level.getEntities(player, player.boundingBox.inflate(radius)) { it.distanceTo(player) <= radius }
                        val entities = found.sortedBy { it.distanceTo(player) }.map { entity ->
                            var text: JsonElement? = null
                            var item: ItemStack? = null
                            var block: String? = null
                            if (entity is Display.TextDisplay) {
                                text = componentJson(entity.text)
                            } else if (entity is Display.ItemDisplay) {
                                item = entity.itemStack
                            } else if (entity is Display.BlockDisplay) {
                                block = entity.blockState.toString()
                            } else if (entity is ItemEntity) {
                                item = entity.item
                            }
                            val living = entity as? LivingEntity
                            val equipment = LinkedHashMap<String, JsonElement>()
                            if (living != null) {
                                for (slot in EquipmentSlot.entries) {
                                    val stack = living.getItemBySlot(slot)
                                    if (!stack.isEmpty) {
                                        equipment[slot.serializedName] = Json.parseToJsonElement(ItemStack.CODEC.encodeStart(ops, stack).getOrThrow().toString())
                                    }
                                }
                            }
                            EntityInfo(
                                id = entity.id,
                                uuid = entity.uuid.toString(),
                                type = EntityType.getKey(entity.type).toString(),
                                name = entity.displayName.string,
                                customName = entity.customName?.let { componentJson(it) },
                                x = entity.x,
                                y = entity.y,
                                z = entity.z,
                                distance = entity.distanceTo(player).toDouble(),
                                invisible = entity.isInvisible,
                                glowing = entity.isCurrentlyGlowing,
                                onFire = entity.isOnFire,
                                sneaking = entity.isShiftKeyDown,
                                pose = entity.pose.name.lowercase(),
                                vehicle = entity.vehicle?.uuid?.toString(),
                                passengers = entity.passengers.map { it.uuid.toString() },
                                equipment = equipment,
                                health = living?.health,
                                maxHealth = living?.maxHealth,
                                text = text,
                                item = if (item != null && !item.isEmpty) Json.parseToJsonElement(ItemStack.CODEC.encodeStart(ops, item).getOrThrow().toString()) else null,
                                block = block
                            )
                        }
                        EntitiesState(radius, entities)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/chat") {
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val messages = DisplayLog.chat.entries.filter { it.sequence > since }.map {
                            ChatMessage(it.sequence, it.time, it.label, it.component.string, componentJson(it.component))
                        }
                        ChatState(DisplayLog.chat.latestSequence, messages)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/title") {
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val messages = DisplayLog.title.entries.filter { it.sequence > since }.map {
                            TitleMessage(it.sequence, it.time, it.label, it.component.string, componentJson(it.component))
                        }
                        TitleState(DisplayLog.title.latestSequence, messages)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/actionbar") {
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val messages = DisplayLog.actionbar.entries.filter { it.sequence > since }.map {
                            ActionbarMessage(it.sequence, it.time, it.component.string, componentJson(it.component))
                        }
                        ActionbarState(DisplayLog.actionbar.latestSequence, messages)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/toast") {
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val toasts = DisplayLog.toasts.filter { it.sequence > since }.map {
                            ToastMessage(it.sequence, it.time, it.type, it.title, it.description)
                        }
                        ToastState(DisplayLog.latestToastSequence, toasts)
                    }
                }
                respondJson(call, json.encodeToString(state))
            }

            get("/bossbar") {
                val state = withTimeout(timeout) {
                    withContext(clientThread) {
                        val overlay = minecraft.gui.hud.bossOverlay as BossHealthOverlayAccessor
                        val bars = overlay.`comprehension$getEvents`().values.map {
                            BossBar(
                                id = it.id.toString(),
                                text = it.name.string,
                                component = componentJson(it.name),
                                progress = it.progress,
                                color = it.color.name.lowercase(),
                                style = it.overlay.name.lowercase()
                            )
                        }
                        BossBarState(bars)
                    }
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
            loading = minecraft.gui.overlay() != null
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

    private fun componentJson(component: Component): JsonElement? {
        val registries = minecraft.connection?.registryAccess() ?: return null
        val encoded = ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
        return encoded.result().map { Json.parseToJsonElement(it.toString()) }.orElse(null)
    }

    private fun keyState(): KeyState {
        val prefix = "key.keyboard."
        val held = VirtualKeyboard.held.map { InputConstants.Type.KEYBOARD.getOrCreate(it).name.removePrefix(prefix) }
        return KeyState(held = held, screen = minecraft.gui.screen()?.javaClass?.name, loading = minecraft.gui.overlay() != null)
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
