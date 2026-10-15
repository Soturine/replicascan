package com.soturine.replicascan.core.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

/** Original transparent paintings. No member is sampled from a flattened pose. */
internal data class FoxLayers(
    val tail: ImageBitmap,
    val body: ImageBitmap,
    val head: ImageBitmap,
    val scarf: ImageBitmap,
    val arm: ImageBitmap,
    val wave: ImageBitmap,
    val paper: ImageBitmap,
)

internal data class FoxMotion(
    val tail: Float = 0f,
    val head: Float = 0f,
    val paws: Float = 0f,
    val gesture: Float = 0f,
    val breath: Float = 0f,
)

private const val STAGE = 1254f

/** All coordinates and anatomical pivots are recorded in rig/manifest.json. */
internal fun DrawScope.drawFoxRig(layers: FoxLayers, state: ReplicaScanMascotState, motion: FoxMotion) {
    val fit = min(size.width / STAGE, size.height / STAGE)
    val left = (size.width - STAGE * fit) / 2f
    val top = (size.height - STAGE * fit) / 2f
    withTransform({ translate(left, top); scale(fit, fit, pivot = Offset.Zero) }) {
        val active = state.isBusy
        val waving = state == ReplicaScanMascotState.Welcome ||
            state == ReplicaScanMascotState.Empty ||
            state == ReplicaScanMascotState.Attention ||
            state == ReplicaScanMascotState.Success
        val paperVisible = active || state == ReplicaScanMascotState.Success

        // Rear tail root stays under a fully painted body, so its rotation never opens a hole.
        sprite(layers.tail, 80f, 610f, 580f, angle = -9f + motion.tail * 11f,
            pivotX = .80f, pivotY = .75f)
        if (waving) {
            sprite(layers.wave, 700f, 440f, 400f,
                angle = 8f + motion.gesture * 24f, pivotX = .12f, pivotY = .78f)
        }
        sprite(layers.body, 280f, 500f + motion.breath * 3f, 690f)
        if (paperVisible) {
            sprite(layers.paper, 420f, 670f + motion.paws * 8f, 420f,
                angle = -2f + motion.paws * 5f, pivotX = .50f, pivotY = .85f)
        }
        sprite(layers.arm, 320f, 660f, 439f,
            angle = 8f + motion.paws * (if (active) 8f else 3f), pivotX = .50f, pivotY = .18f)
        if (!waving) {
            sprite(layers.arm, 520f, 660f, 439f,
                angle = -8f - motion.paws * (if (active) 8f else 3f),
                pivotX = .50f, pivotY = .18f)
        }
        // The scarf is a stationary neck layer. Neither the tail nor the head contains it.
        sprite(layers.scarf, 400f, 525f + motion.breath * 2f, 455f)
        sprite(layers.head, 130f, 50f, 998f, angle = 3f + motion.head * 5f,
            pivotX = .50f, pivotY = .85f)
    }
}

private fun DrawScope.sprite(
    image: ImageBitmap,
    x: Float,
    y: Float,
    width: Float,
    angle: Float = 0f,
    pivotX: Float = .5f,
    pivotY: Float = .5f,
) {
    val height = width * image.height / image.width
    withTransform({ rotate(angle, Offset(x + width * pivotX, y + height * pivotY)) }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
            dstSize = IntSize(width.roundToInt(), height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
