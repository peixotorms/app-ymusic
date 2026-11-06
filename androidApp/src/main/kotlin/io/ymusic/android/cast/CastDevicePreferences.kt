package io.ymusic.android.cast

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

internal class CastDevicePreferences(private val preferences: SharedPreferences) {
    val revision = MutableStateFlow(0L)

    var sortDescending: Boolean
        get() = preferences.getBoolean("sortDescending", false)
        set(value) {
            preferences.edit().putBoolean("sortDescending", value).apply()
            revision.value++
        }

    val hasHiddenDevices: Boolean
        get() = preferences.all.any { (key, value) -> key.startsWith("hidden.") && value == true }

    fun isHidden(id: String): Boolean = preferences.getBoolean("hidden.$id", false)

    fun setHidden(id: String, hidden: Boolean) {
        if (id == LOCAL_DEVICE) return
        preferences.edit().putBoolean("hidden.$id", hidden).apply()
        revision.value++
    }

    fun volumeStep(id: String): Int = preferences.getInt("step.$id", if (id == LOCAL_DEVICE) 0 else 5)
        .coerceIn(if (id == LOCAL_DEVICE) 0 else 1, 20)

    fun setVolumeStep(id: String, step: Int) {
        preferences.edit().putInt("step.$id", step.coerceIn(if (id == LOCAL_DEVICE) 0 else 1, 20)).apply()
        revision.value++
    }

    fun restore(id: String) {
        preferences.edit().remove("step.$id").remove("hidden.$id").apply()
        revision.value++
    }

    companion object {
        const val LOCAL_DEVICE = "local"
    }
}
