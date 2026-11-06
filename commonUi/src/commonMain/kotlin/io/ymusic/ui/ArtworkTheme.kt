package io.ymusic.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalArtworkCutout = staticCompositionLocalOf<((Rect?) -> Unit)?> { null }

@Composable
internal fun ArtworkTheme(url: String?, paintBackground: Boolean = true, nativeVideoCutout: Boolean = false, gradient: Boolean = true, content: @Composable () -> Unit) {
    var cutout by remember { mutableStateOf<Rect?>(null) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val updateCutout: (Rect?) -> Unit = remember { { cutout = it } }
    val loader = LocalImageLoader.current
    val color by produceState(Color(0xFF65513D), url, loader) {
        value = if (url != null && loader != null) {
            val bitmap = loader.loadOrNull(artImageRequest(url, 48, 48))
            bitmap?.let { withContext(Dispatchers.Default) { artworkColor(it) } } ?: Color(0xFF65513D)
        } else {
            Color(0xFF65513D)
        }
    }
    val tint by animateColorAsState(color, tween(600))
    val accent = lerp(tint, Color.White, 0.64f)
    val secondary = lerp(tint, Color.White, 0.72f)
    val backdrop = remember(tint, gradient) {
        if (!gradient) return@remember Color.Black
        var color = lerp(tint, Color.Black, 0.25f)
        while (color.luminance() > 0.16f) color = lerp(color, Color.Black, 0.1f)
        color
    }
    val palette = remember(tint) {
        DarkDesignColors.copy(
            Accent = accent,
            OnAccent = Color(0xFF151311),
            InkStrong = Color(0xFFFFFAF6),
            Ink = Color(0xFFF2ECE7),
            InkSecondary = secondary,
            InkMuted = secondary,
            InkSoft = secondary.copy(alpha = 0.68f),
            InkFaint = secondary.copy(alpha = 0.85f),
            IconDots = secondary,
            Surface = Color.Black,
            SurfaceDim = lerp(tint, Color.Black, 0.80f),
            SurfaceElevated = lerp(tint, Color.Black, 0.84f),
            SettingsCard = lerp(tint, Color.Black, 0.80f),
            FieldPill = lerp(tint, Color.Black, 0.70f),
            OutlineSoft = accent.copy(alpha = 0.24f),
            Divider = accent.copy(alpha = 0.12f),
        )
    }
    YMusicTheme(palette) {
        Box(
            Modifier.fillMaxSize().onGloballyPositioned { bounds = it.boundsInWindow() }.drawBehind {
                if (paintBackground) {
                    val brush = Brush.verticalGradient(0f to backdrop, 0.60f to Color.Black, 1f to Color.Black)
                    val hole = cutout?.translate(-bounds.topLeft)
                    if (nativeVideoCutout && hole != null) {
                        clipRect(hole.left, hole.top, hole.right, hole.bottom, ClipOp.Difference) { drawRect(brush) }
                    } else {
                        drawRect(brush)
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalArtworkCutout provides if (nativeVideoCutout) updateCutout else null) { content() }
        }
    }
}

private fun artworkColor(bitmap: ImageBitmap): Color {
    val pixels = bitmap.toPixelMap()
    val weights = FloatArray(64)
    val red = FloatArray(64)
    val green = FloatArray(64)
    val blue = FloatArray(64)
    for (y in 0 until pixels.height step (pixels.height / 24).coerceAtLeast(1)) {
        for (x in 0 until pixels.width step (pixels.width / 24).coerceAtLeast(1)) {
            val c = pixels[x, y]
            val high = maxOf(c.red, c.green, c.blue)
            val low = minOf(c.red, c.green, c.blue)
            if (c.alpha < 0.5f || high < 0.12f || low > 0.90f) continue
            val bucket = (c.red * 3).toInt() * 16 + (c.green * 3).toInt() * 4 + (c.blue * 3).toInt()
            val weight = 0.15f + high - low
            weights[bucket] += weight
            red[bucket] += c.red * weight
            green[bucket] += c.green * weight
            blue[bucket] += c.blue * weight
        }
    }
    val bucket = weights.indices.maxByOrNull { weights[it] } ?: return Color(0xFF65513D)
    val weight = weights[bucket]
    if (weight == 0f) return Color(0xFF65513D)
    return Color(red[bucket] / weight, green[bucket] / weight, blue[bucket] / weight)
}
