package com.worddrop.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Stroke icons on a 24dp grid, 1.75 stroke, round caps — the same set drawn in the mockups
 * (UI/UX spec §6.3). Tinted through `Icon(tint = …)`.
 */
object WdIcons {

    private fun stroke(name: String, fill: Boolean = false, build: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(
                    fill = if (fill) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero,
                    pathBuilder = build,
                )
            }
            .build()

    val Refresh: ImageVector by lazy {
        stroke("Refresh") {
            moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 17.66f, 6.34f)
            moveTo(20f, 4f); verticalLineTo(9f); horizontalLineTo(15f)
        }
    }

    val Bookmark: ImageVector by lazy {
        stroke("Bookmark") { moveTo(6f, 4f); horizontalLineTo(18f); verticalLineTo(21f); lineTo(12f, 17f); lineTo(6f, 21f); close() }
    }

    val BookmarkFilled: ImageVector by lazy {
        stroke("BookmarkFilled", fill = true) { moveTo(6f, 4f); horizontalLineTo(18f); verticalLineTo(21f); lineTo(12f, 17f); lineTo(6f, 21f); close() }
    }

    val Back: ImageVector by lazy {
        stroke("Back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    }

    val ChevronRight: ImageVector by lazy {
        stroke("ChevronRight") { moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f) }
    }

    val Sliders: ImageVector by lazy {
        stroke("Sliders") {
            moveTo(4f, 7f); horizontalLineTo(10f); moveTo(18f, 7f); horizontalLineTo(20f)
            moveTo(4f, 12f); horizontalLineTo(6f); moveTo(10f, 12f); horizontalLineTo(20f)
            moveTo(4f, 17f); horizontalLineTo(12f); moveTo(16f, 17f); horizontalLineTo(20f)
            circle(16f, 7f, 2f); circle(8f, 12f, 2f); circle(14f, 17f, 2f)
        }
    }

    val Speaker: ImageVector by lazy {
        stroke("Speaker") {
            moveTo(5f, 9f); verticalLineTo(15f); horizontalLineTo(9f); lineTo(14f, 19f); verticalLineTo(5f); lineTo(9f, 9f); close()
            moveTo(17f, 9f); arcTo(4f, 4f, 0f, false, true, 17f, 15f)
        }
    }

    val Check: ImageVector by lazy {
        stroke("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }
    }

    val Close: ImageVector by lazy {
        stroke("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }

    val Trash: ImageVector by lazy {
        stroke("Trash") {
            moveTo(4f, 7f); horizontalLineTo(20f)
            moveTo(10f, 11f); verticalLineTo(17f); moveTo(14f, 11f); verticalLineTo(17f)
            moveTo(6f, 7f); lineTo(7f, 20f); horizontalLineTo(17f); lineTo(18f, 7f)
            moveTo(9f, 7f); verticalLineTo(4f); horizontalLineTo(15f); verticalLineTo(7f)
        }
    }

    val Calendar: ImageVector by lazy {
        stroke("Calendar") {
            moveTo(6f, 5f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 7f); verticalLineTo(18f)
            arcTo(2f, 2f, 0f, false, true, 18f, 20f); horizontalLineTo(6f); arcTo(2f, 2f, 0f, false, true, 4f, 18f)
            verticalLineTo(7f); arcTo(2f, 2f, 0f, false, true, 6f, 5f); close()
            moveTo(4f, 10f); horizontalLineTo(20f); moveTo(8f, 3f); verticalLineTo(7f); moveTo(16f, 3f); verticalLineTo(7f)
        }
    }

    val ListCheck: ImageVector by lazy {
        stroke("ListCheck") {
            moveTo(9f, 6f); horizontalLineTo(20f); moveTo(9f, 12f); horizontalLineTo(20f); moveTo(9f, 18f); horizontalLineTo(20f)
            moveTo(4f, 6f); lineTo(5f, 7f); lineTo(7f, 5f)
            moveTo(4f, 12f); lineTo(5f, 13f); lineTo(7f, 11f)
            moveTo(4f, 18f); lineTo(5f, 19f); lineTo(7f, 17f)
        }
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, false, cx + r, cy)
        arcTo(r, r, 0f, true, false, cx - r, cy)
    }
}
