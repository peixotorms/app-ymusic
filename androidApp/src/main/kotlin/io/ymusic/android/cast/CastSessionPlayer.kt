@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package io.ymusic.android.cast

import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class CastSessionPlayer(player: Player, private val volume: CastVolumeControl) : ForwardingSimpleBasePlayer(player) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch { volume.state.collect { invalidateState() } }
    }

    override fun getState(): State {
        val local = super.getState()
        val remote = volume.state.value ?: return local
        val commands = local.availableCommands.buildUpon()
            .add(Player.COMMAND_GET_DEVICE_VOLUME)
            .add(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
            .add(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
            .build()
        return local.buildUpon()
            .setAvailableCommands(commands)
            .setDeviceInfo(DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).setMinVolume(0).setMaxVolume(100).build())
            .setDeviceVolume(remote.percent)
            .setIsDeviceMuted(remote.muted)
            .build()
    }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
        if (volume.state.value == null) return super.handleSetDeviceVolume(deviceVolume, flags)
        volume.setVolume(deviceVolume)
        return Futures.immediateVoidFuture()
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        if (volume.state.value == null) return super.handleIncreaseDeviceVolume(flags)
        volume.adjustVolume(1)
        return Futures.immediateVoidFuture()
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        if (volume.state.value == null) return super.handleDecreaseDeviceVolume(flags)
        volume.adjustVolume(-1)
        return Futures.immediateVoidFuture()
    }

    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
        if (volume.state.value == null) return super.handleSetDeviceMuted(muted, flags)
        volume.setMuted(muted)
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        scope.cancel()
        return super.handleRelease()
    }
}
