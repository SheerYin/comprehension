package me.yin.comprehension

import me.yin.comprehension.mixin.AdvancementToastAccessor
import me.yin.comprehension.mixin.SystemToastAccessor
import me.yin.comprehension.mixin.TutorialToastAccessor
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

class TextEntry(
    val sequence: Long,
    val time: Long,
    val label: String,
    val component: Component
)

class TextLog {
    val entries = ArrayDeque<TextEntry>()
    var latestSequence = 0L
        private set

    fun record(label: String, component: Component) {
        latestSequence++
        entries.addLast(TextEntry(latestSequence, System.currentTimeMillis(), label, component))
        if (entries.size > 500) {
            entries.removeFirst()
        }
    }
}

class ToastEntry(
    val sequence: Long,
    val time: Long,
    val type: String,
    val title: String?,
    val description: String?
)

object DisplayLog {
    val chat = TextLog()
    val title = TextLog()
    val actionbar = TextLog()

    val toasts = ArrayDeque<ToastEntry>()
    var latestToastSequence = 0L
        private set

    @JvmStatic
    fun recordChat(source: String, component: Component) {
        chat.record(source, component)
    }

    @JvmStatic
    fun recordTitle(type: String, component: Component) {
        title.record(type, component)
    }

    @JvmStatic
    fun recordActionbar(component: Component) {
        actionbar.record("actionbar", component)
    }

    @JvmStatic
    fun recordToast(toast: Toast) {
        var type = toast.javaClass.simpleName.removeSuffix("Toast").replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        var title: String? = null
        var description: String? = null
        if (toast is SystemToastAccessor) {
            type = "system"
            title = linesToText(toast.`comprehension$getTitleLines`())
            description = linesToText(toast.`comprehension$getMessageLines`())
        } else if (toast is AdvancementToastAccessor) {
            type = "advancement"
            val display = toast.`comprehension$getAdvancement`().value().display().orElse(null)
            title = display?.title()?.string
            description = display?.description()?.string
        } else if (toast is TutorialToastAccessor) {
            type = "tutorial"
            description = linesToText(toast.`comprehension$getLines`())
        }

        latestToastSequence++
        toasts.addLast(ToastEntry(latestToastSequence, System.currentTimeMillis(), type, title, description))
        if (toasts.size > 500) {
            toasts.removeFirst()
        }
    }

    private fun linesToText(lines: List<FormattedCharSequence>?): String? {
        if (lines.isNullOrEmpty()) {
            return null
        }
        val text = StringBuilder()
        for ((index, line) in lines.withIndex()) {
            if (index > 0) {
                text.append('\n')
            }
            line.accept { _, _, codepoint ->
                text.appendCodePoint(codepoint)
                true
            }
        }
        return text.toString()
    }
}
