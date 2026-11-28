package me.yin.comprehension

import com.mojang.blaze3d.platform.InputConstants

enum class MouseButton(val code: Int, val apiName: String) {
    LEFT(InputConstants.MOUSE_BUTTON_LEFT, "left"),
    RIGHT(InputConstants.MOUSE_BUTTON_RIGHT, "right"),
    MIDDLE(InputConstants.MOUSE_BUTTON_MIDDLE, "middle");

    companion object {
        val held = LinkedHashSet<MouseButton>()

        fun parse(name: String): MouseButton =
            entries.firstOrNull { it.apiName == name }
                ?: throw IllegalArgumentException("Unknown button '$name', expected left, right or middle")
    }
}
