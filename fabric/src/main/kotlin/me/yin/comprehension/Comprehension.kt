package me.yin.comprehension

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.slf4j.LoggerFactory

object Comprehension : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("comprehension")
    private var server: ControlServer? = null

    override fun onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register {
            val port = Integer.getInteger("comprehension.port", 25592)
            server = ControlServer(port)
            logger.info("Comprehension listening on http://127.0.0.1:{}", port)
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            Frames.onClientTick()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            server?.stop()
            server = null
        }
    }
}
