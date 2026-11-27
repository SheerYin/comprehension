package me.yin.comprehension

object VirtualKeyboard {
    val held = LinkedHashSet<Int>()

    private val modifierBits = mapOf(
        225 to 0x0001,
        229 to 0x0002,
        224 to 0x0040,
        228 to 0x0080,
        226 to 0x0100,
        230 to 0x0200,
        227 to 0x0400,
        231 to 0x0800
    )

    @JvmStatic
    fun isDown(scancode: Int): Boolean = scancode in held

    fun modifiers(): Int {
        var bits = 0
        for (scancode in held) {
            bits = bits or (modifierBits[scancode] ?: 0)
        }
        return bits
    }
}
