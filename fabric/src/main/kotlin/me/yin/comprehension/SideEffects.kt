package me.yin.comprehension

import kotlinx.serialization.Serializable

@Serializable
data class SideEffect(val kind: String, val value: String)

object SideEffects {
    val records = ArrayList<SideEffect>()
    var clipboard = ""

    @JvmStatic
    fun openUri(uri: String) {
        records.add(SideEffect("open_uri", uri))
    }

    @JvmStatic
    fun openPath(path: String) {
        records.add(SideEffect("open_path", path))
    }

    @JvmStatic
    fun writeClipboard(text: String) {
        clipboard = text
        records.add(SideEffect("set_clipboard", text))
    }

    @JvmStatic
    fun readClipboard(): String = clipboard
}
