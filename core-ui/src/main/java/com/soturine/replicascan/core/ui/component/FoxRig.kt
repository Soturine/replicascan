package com.soturine.replicascan.core.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * Draw an approved pose intact. The former geometric tail/head/paw/page masks selected pixels
 * from a complete painting, so rotating them dragged scarf and body paint and exposed a second
 * static tail. Independent anatomical source layers do not exist yet. The only local overlay is
 * the matching closed-eye painting, clipped to eye regions that were inspected per pose.
 */
internal fun DrawScope.drawFoxPose(
    pose: ImageBitmap,
    blinkPose: ImageBitmap,
    state: ReplicaScanMascotState,
    blink: Boolean,
) {
    val fit = min(size.width / pose.width, size.height / pose.height)
    val left = (size.width - pose.width * fit) / 2f
    val top = (size.height - pose.height * fit) / 2f
    withTransform({ translate(left, top); scale(fit, fit, pivot = Offset.Zero) }) {
        drawImage(pose, filterQuality = FilterQuality.Medium)
        if (blink && state != ReplicaScanMascotState.Success) {
            clipPath(eyeMask(state, pose.width.toFloat(), pose.height.toFloat())) {
                drawImage(
                    blinkPose,
                    dstSize = IntSize(pose.width, pose.height),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
}

private fun eyeMask(state: ReplicaScanMascotState, w: Float, h: Float): Path {
    val bounds = when (state) {
        ReplicaScanMascotState.Welcome -> listOf(.365f, .35f, .49f, .47f, .605f, .31f, .72f, .43f)
        ReplicaScanMascotState.Working, ReplicaScanMascotState.Processing,
        ReplicaScanMascotState.Scanning -> listOf(.395f, .345f, .52f, .47f, .634f, .372f, .746f, .50f)
        ReplicaScanMascotState.Ocr -> listOf(.405f, .33f, .53f, .485f, .636f, .298f, .74f, .46f)
        ReplicaScanMascotState.Attention -> listOf(.375f, .347f, .50f, .46f, .608f, .307f, .729f, .43f)
        ReplicaScanMascotState.Empty -> listOf(.365f, .31f, .48f, .42f, .575f, .26f, .69f, .37f)
        else -> listOf(.34f, .325f, .475f, .455f, .585f, .315f, .722f, .455f)
    }
    return Path().apply {
        addOval(Rect(w * (bounds[0] - .03f), h * (bounds[1] - .005f),
            w * (bounds[2] + .03f), h * (bounds[3] + .02f)))
        addOval(Rect(w * (bounds[4] - .03f), h * (bounds[5] - .005f),
            w * (bounds[6] + .03f), h * (bounds[7] + .02f)))
    }
}
