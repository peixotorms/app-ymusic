package io.ymusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val PlayerPreviousIcon: ImageVector by lazy { skipIcon(previous = true) }
internal val PlayerNextIcon: ImageVector by lazy { skipIcon(previous = false) }

private fun skipIcon(previous: Boolean): ImageVector =
    ImageVector.Builder(if (previous) "Previous" else "Next", 32.dp, 32.dp, 32f, 32f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            val bar = if (previous) 6f else 26f
            moveTo(bar, 5f)
            lineTo(bar, 27f)
            val edge = if (previous) 25f else 7f
            val tip = if (previous) 11f else 21f
            moveTo(edge, 7f)
            lineTo(tip, 16f)
            lineTo(edge, 25f)
            close()
        }
    }.build()
