package me.yin.comprehension

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

object Comprehension : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("comprehension")
    private var server: ControlServer? = null

    override fun onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register {
            val port = Integer.getInteger("comprehension.port", 25592)
            server = ControlServer(port)
            logger.info("Comprehension listening on http://127.0.0.1:{}", port)
            exitWithParent(it)
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            Frames.onClientTick()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            server?.stop()
            server = null
        }
    }

    private fun exitWithParent(minecraft: Minecraft) {
        val pid = java.lang.Long.getLong("comprehension.parentPid") ?: return
        val parent = ProcessHandle.of(pid).orElse(null)
        if (parent == null) {
            logger.warn("Parent process {} is not running; the client will not exit with it", pid)
            return
        }
        logger.info("Client will exit when process {} exits", pid)
        Thread {
            while (parent.isAlive) {
                Thread.sleep(1000)
            }
            logger.info("Parent process {} exited, stopping the client", pid)
            minecraft.execute { minecraft.stop() }
        }.apply {
            name = "Comprehension parent watcher"
            isDaemon = true
            start()
        }
    }
}
