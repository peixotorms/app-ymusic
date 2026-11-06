package io.ymusic.android.cast

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.widget.Toast
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastSession
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.cast_volume_error
import io.ymusic.ui.resources.cast_volume_feedback
import io.ymusic.ui.resources.cast_volume_muted_feedback
import kmpcommon.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.math.roundToInt

internal data class CastVolumeState(
    val deviceId: String,
    val name: String,
    val percent: Int,
    val muted: Boolean,
    val available: Boolean = true,
)

internal interface CastVolumeControl {
    val state: StateFlow<CastVolumeState?>
    fun setVolume(percent: Int)
    fun adjustVolume(direction: Int)
    fun setMuted(muted: Boolean)
}

internal class CastVolumeController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: CastDevicePreferences,
    private val logger: Logger,
) : CastVolumeControl {
    override val state = MutableStateFlow<CastVolumeState?>(null)
    private var session: CastSession? = null
    private var pending: CastVolumeState? = null
    private var reconcile: Job? = null
    private var toast: Toast? = null
    private val listener = object : Cast.Listener() {
        override fun onVolumeChanged() = refresh()
    }

    fun attach(session: CastSession?) {
        this.session?.removeCastListener(listener)
        reconcile?.cancel()
        pending = null
        this.session = session
        state.value = session?.castDevice?.let {
            CastVolumeState(it.deviceId, it.friendlyName, 0, false, available = false)
        }
        session?.addCastListener(listener)
        refresh(force = true)
    }

    fun suspendSession() {
        session?.removeCastListener(listener)
        session = null
        reconcile?.cancel()
        pending = null
        state.value = state.value?.copy(available = false)
    }

    private fun refresh(force: Boolean = false) {
        val cast = session ?: return
        val current = state.value ?: return
        try {
            val value = cast.volume
            if (!value.isFinite()) return
            val actual = current.copy(percent = (value * 100).roundToInt().coerceIn(0, 100), muted = cast.isMute, available = true)
            val target = pending
            if (!force && target != null && (actual.percent != target.percent || actual.muted != target.muted)) return
            pending = null
            state.value = actual
        } catch (error: Exception) {
            logger.log(Logger.Level.WARN, "Cast device volume unavailable", error)
            state.value = current.copy(available = false)
        }
    }

    override fun setVolume(percent: Int) {
        val current = state.value ?: return
        change(current.copy(percent = percent.coerceIn(0, 100), muted = false))
    }

    override fun adjustVolume(direction: Int) {
        val current = state.value ?: return
        if (direction == 0) return
        val step = preferences.volumeStep(current.deviceId)
        setVolume(current.percent + if (direction > 0) step else -step)
    }

    override fun setMuted(muted: Boolean) {
        val current = state.value ?: return
        change(current.copy(muted = muted))
    }

    private fun change(target: CastVolumeState) {
        val cast = session ?: return
        if (state.value?.available != true) return
        val before = state.value!!
        try {
            pending = target
            state.value = target
            cast.volume = target.percent / 100.0
            if (before.muted != target.muted) cast.isMute = target.muted
            reconcile?.cancel()
            reconcile = scope.launch {
                delay(1_000)
                refresh(force = true)
            }
            scope.launch {
                feedback(
                    if (target.muted) getString(Res.string.cast_volume_muted_feedback, target.name)
                    else getString(Res.string.cast_volume_feedback, target.name, target.percent),
                )
            }
        } catch (error: Exception) {
            pending = null
            state.value = before
            refresh(force = true)
            logger.log(Logger.Level.WARN, "Cast device volume change failed", error)
            scope.launch { feedback(getString(Res.string.cast_volume_error, "E60076")) }
        }
    }

    private fun feedback(text: String) {
        toast?.cancel()
        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT).also { it.show() }
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && event.keyCode != KeyEvent.KEYCODE_VOLUME_MUTE) return false
        val remote = state.value
        val localStep = preferences.volumeStep(CastDevicePreferences.LOCAL_DEVICE)
        if (remote == null && localStep == 0) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (remote != null) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> adjustVolume(1)
                KeyEvent.KEYCODE_VOLUME_DOWN -> adjustVolume(-1)
                KeyEvent.KEYCODE_VOLUME_MUTE -> if (event.repeatCount == 0) setMuted(!remote.muted)
            }
        } else {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                if (event.repeatCount == 0) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            } else {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val step = (max * localStep / 100.0).roundToInt().coerceAtLeast(1)
                val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                val target = current + if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) step else -step
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, target.coerceIn(0, max), AudioManager.FLAG_SHOW_UI)
            }
        }
        return true
    }
}
