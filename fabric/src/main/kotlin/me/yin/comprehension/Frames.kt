package me.yin.comprehension

import kotlinx.coroutines.CompletableDeferred

object Frames {
    private val waiting = ArrayList<CompletableDeferred<Unit>>()

    fun next(): CompletableDeferred<Unit> {
        val frame = CompletableDeferred<Unit>()
        waiting.add(frame)
        return frame
    }

    @JvmStatic
    fun onFrameRendered() {
        if (waiting.isEmpty()) {
            return
        }
        val rendered = waiting.toList()
        waiting.clear()
        for (frame in rendered) {
            frame.complete(Unit)
        }
    }
}
