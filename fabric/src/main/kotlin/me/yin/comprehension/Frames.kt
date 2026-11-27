package me.yin.comprehension

import kotlinx.coroutines.CompletableDeferred

class FrameWaiter {
    var ticked = false
    val done = CompletableDeferred<Unit>()
}

object Frames {
    private val waiting = ArrayList<FrameWaiter>()

    fun afterTickAndFrame(): CompletableDeferred<Unit> {
        val waiter = FrameWaiter()
        waiting.add(waiter)
        return waiter.done
    }

    @JvmStatic
    fun onClientTick() {
        for (waiter in waiting) {
            waiter.ticked = true
        }
    }

    @JvmStatic
    fun onFrameRendered() {
        if (waiting.isEmpty()) {
            return
        }
        val settled = waiting.filter { it.ticked }
        waiting.removeAll(settled)
        for (waiter in settled) {
            waiter.done.complete(Unit)
        }
    }
}
