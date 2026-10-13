package com.soturine.replicascan.core.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.min

/**
 * Editable cut lines and pivots over the approved painted poses. Each part is drawn from the
 * original bitmap, so the rig never replaces the fox with a simplified vector character.
 * Coordinates are proportional to the pose artwork. The masks are render-time layers, not a
 * generated 3×3 pose atlas.
 */
internal fun DrawScope.drawFoxRig(
    pose: ImageBitmap,
    blinkPose: ImageBitmap,
    state: ReplicaScanMascotState,
    tailAngle: Float,
    headAngle: Float,
    pawAngle: Float,
    pageAngle: Float,
    blink: Float,
) {
    val fit = min(size.width / pose.width, size.height / pose.height)
    val left = (size.width - pose.width * fit) / 2f
    val top = (size.height - pose.height * fit) / 2f
    withTransform({ translate(left, top); scale(fit, fit, pivot = Offset.Zero) }) {
        val w = pose.width.toFloat()
        val h = pose.height.toFloat()
        val tail = tailMask(w, h)
        val head = headMask(w, h)
        val headCutout = headCutoutMask(w, h)
        val paw = pawMask(state, w, h)
        val page = pageMask(state, w, h)

        // The tail is behind the torso. A small overlap at its root conceals the cut while it
        // pivots; the body/head are then painted above it from the same approved PNG.
        withTransform({ rotate(tailAngle, Offset(w * .40f, h * .77f)) }) {
            clipPath(tail) { drawImage(pose, filterQuality = FilterQuality.Medium) }
        }
        clipPath(tail, clipOp = ClipOp.Difference) {
            clipPath(headCutout, clipOp = ClipOp.Difference) {
                drawImage(pose, filterQuality = FilterQuality.Medium)
            }
        }
        // The hind leg and front paws occlude the tail. Restore their original paint after the
        // tail cut, including the pixels at the shared root where a straight cut would show.
        clipPath(bodyForegroundMask(w, h)) {
            drawImage(pose, filterQuality = FilterQuality.Medium)
        }

        withTransform({ rotate(headAngle, Offset(w * .53f, h * .61f)) }) {
            clipPath(head) {
                drawImage(pose, filterQuality = FilterQuality.Medium)
            }
            if (blink > 0f && state != ReplicaScanMascotState.Success) {
                clipPath(eyeMask(state, w, h)) {
                    drawImage(
                        blinkPose,
                        dstSize = androidx.compose.ui.unit.IntSize(pose.width, pose.height),
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }
        }

        if (page != null) {
            withTransform({ rotate(pageAngle, pagePivot(state, w, h)) }) {
                clipPath(page) { drawImage(pose, filterQuality = FilterQuality.Medium) }
            }
        }

        if (paw != null) {
            withTransform({ rotate(pawAngle, pawPivot(state, w, h)) }) {
                clipPath(paw) { drawImage(pose, filterQuality = FilterQuality.Medium) }
            }
        }
    }
}

private fun tailMask(w: Float, h: Float) = Path().apply {
    moveTo(0f, h * .48f)
    lineTo(w * .25f, h * .48f)
    quadraticBezierTo(w * .38f, h * .53f, w * .39f, h * .71f)
    quadraticBezierTo(w * .43f, h * .89f, w * .48f, h)
    lineTo(0f, h)
    close()
}

private fun bodyForegroundMask(w: Float, h: Float) = Path().apply {
    moveTo(w * .43f, h * .65f)
    quadraticBezierTo(w * .37f, h * .78f, w * .34f, h)
    lineTo(w, h)
    lineTo(w, h * .65f)
    close()
}

private fun headMask(w: Float, h: Float) = Path().apply {
    moveTo(w * .19f, 0f)
    lineTo(w * .87f, 0f)
    lineTo(w, h * .44f)
    quadraticBezierTo(w * .90f, h * .62f, w * .73f, h * .635f)
    quadraticBezierTo(w * .50f, h * .67f, w * .29f, h * .62f)
    quadraticBezierTo(w * .18f, h * .55f, w * .19f, h * .44f)
    close()
}

/** The static neck overlaps the moving painted head, hiding the cut below the cheeks. */
private fun headCutoutMask(w: Float, h: Float) = Path().apply {
    moveTo(w * .19f, 0f)
    lineTo(w * .87f, 0f)
    lineTo(w, h * .44f)
    quadraticBezierTo(w * .90f, h * .57f, w * .73f, h * .585f)
    quadraticBezierTo(w * .50f, h * .605f, w * .29f, h * .57f)
    quadraticBezierTo(w * .18f, h * .53f, w * .19f, h * .44f)
    close()
}

private fun eyeMask(state: ReplicaScanMascotState, w: Float, h: Float): Path {
    val bounds = when (state) {
        ReplicaScanMascotState.Welcome -> listOf(.365f, .35f, .49f, .47f, .605f, .31f, .72f, .43f)
        ReplicaScanMascotState.Working, ReplicaScanMascotState.Processing,
        ReplicaScanMascotState.Scanning -> listOf(.395f, .345f, .52f, .47f, .634f, .372f, .746f, .50f)
        // Reading's open-eye outlines extend above the white of each eye. Include those
        // outlines in the blink overlay without touching the separate eyebrows.
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

private fun pageMask(state: ReplicaScanMascotState, w: Float, h: Float): Path? = when (state) {
    ReplicaScanMascotState.Working, ReplicaScanMascotState.Processing,
    ReplicaScanMascotState.Scanning, ReplicaScanMascotState.Ocr -> Path().apply {
        moveTo(w * .53f, h * .605f)
        lineTo(w * .785f, h * .598f)
        lineTo(w * .73f, h * .803f)
        lineTo(w * .50f, h * .807f)
        close()
    }
    ReplicaScanMascotState.Success -> Path().apply {
        moveTo(w * .50f, h * .585f)
        lineTo(w * .73f, h * .60f)
        lineTo(w * .70f, h * .855f)
        lineTo(w * .47f, h * .825f)
        close()
    }
    else -> null
}

private fun pagePivot(state: ReplicaScanMascotState, w: Float, h: Float) = when (state) {
    ReplicaScanMascotState.Success -> Offset(w * .58f, h * .80f)
    else -> Offset(w * .61f, h * .79f)
}

private fun pawMask(state: ReplicaScanMascotState, w: Float, h: Float): Path? = when (state) {
    ReplicaScanMascotState.Welcome, ReplicaScanMascotState.Success -> Path().apply {
        moveTo(w * .72f, h * .75f)
        quadraticBezierTo(w * .76f, h * .57f, w * .81f, h * .49f)
        lineTo(w, h * .45f)
        lineTo(w, h * .75f)
        close()
    }
    ReplicaScanMascotState.Working, ReplicaScanMascotState.Processing,
    ReplicaScanMascotState.Scanning, ReplicaScanMascotState.Ocr,
    ReplicaScanMascotState.Attention -> Path().apply {
        addOval(Rect(w * .39f, h * .66f, w * .58f, h * .82f))
        addOval(Rect(w * .69f, h * .66f, w * .84f, h * .81f))
    }
    else -> null
}

private fun pawPivot(state: ReplicaScanMascotState, w: Float, h: Float) = when (state) {
    ReplicaScanMascotState.Welcome, ReplicaScanMascotState.Success -> Offset(w * .75f, h * .70f)
    else -> Offset(w * .48f, h * .70f)
}
