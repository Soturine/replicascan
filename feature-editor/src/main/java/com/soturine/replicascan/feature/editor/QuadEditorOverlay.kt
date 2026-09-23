package com.soturine.replicascan.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import kotlin.math.roundToInt

@Composable
fun QuadEditorOverlay(
    quad: DocumentQuad,
    imageBounds: Rect,
    onQuadChange: (DocumentQuad) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokeColor = MaterialTheme.colorScheme.tertiary
    val handleFill = MaterialTheme.colorScheme.surface
    val handleStroke = MaterialTheme.colorScheme.primary
    val moveUp = stringResource(R.string.editor_corner_move_up)
    val moveDown = stringResource(R.string.editor_corner_move_down)
    val moveLeft = stringResource(R.string.editor_corner_move_left)
    val moveRight = stringResource(R.string.editor_corner_move_right)
    var activeHandle by remember { mutableStateOf<HandleAnchor?>(null) }

    val topLeft = imageBounds.toOffset(quad.topLeft)
    val topRight = imageBounds.toOffset(quad.topRight)
    val bottomRight = imageBounds.toOffset(quad.bottomRight)
    val bottomLeft = imageBounds.toOffset(quad.bottomLeft)
    val activeOffset = when (activeHandle) {
        HandleAnchor.TOP_LEFT -> topLeft
        HandleAnchor.TOP_RIGHT -> topRight
        HandleAnchor.BOTTOM_RIGHT -> bottomRight
        HandleAnchor.BOTTOM_LEFT -> bottomLeft
        null -> null
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .magnifier(
                sourceCenter = { activeOffset ?: Offset.Unspecified },
                magnifierCenter = {
                    activeOffset?.let { Offset(it.x, it.y - 96.dp.toPx()) } ?: Offset.Unspecified
                },
                zoom = 1.8f,
            ),
    ) {
        val dimmedPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(imageBounds)
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }
        drawPath(
            path = dimmedPath,
            color = Color.Black.copy(alpha = 0.18f),
        )
        drawLine(strokeColor, topLeft, topRight, strokeWidth = 6f)
        drawLine(strokeColor, topRight, bottomRight, strokeWidth = 6f)
        drawLine(strokeColor, bottomRight, bottomLeft, strokeWidth = 6f)
        drawLine(strokeColor, bottomLeft, topLeft, strokeWidth = 6f)
        activeOffset?.let { offset ->
            drawLine(
                color = strokeColor.copy(alpha = 0.4f),
                start = Offset(imageBounds.left, offset.y),
                end = Offset(imageBounds.right, offset.y),
                strokeWidth = 2.5f,
            )
            drawLine(
                color = strokeColor.copy(alpha = 0.4f),
                start = Offset(offset.x, imageBounds.top),
                end = Offset(offset.x, imageBounds.bottom),
                strokeWidth = 2.5f,
            )
        }
        drawCircle(
            color = strokeColor.copy(alpha = 0.2f),
            radius = 18f,
            center = topLeft,
            style = Fill,
        )
        drawCircle(
            color = strokeColor.copy(alpha = 0.2f),
            radius = 18f,
            center = topRight,
            style = Fill,
        )
        drawCircle(
            color = strokeColor.copy(alpha = 0.2f),
            radius = 18f,
            center = bottomRight,
            style = Fill,
        )
        drawCircle(
            color = strokeColor.copy(alpha = 0.2f),
            radius = 18f,
            center = bottomLeft,
            style = Fill,
        )
        drawCircle(strokeColor, radius = 14f, center = topLeft, style = Stroke(4f))
        drawCircle(strokeColor, radius = 14f, center = topRight, style = Stroke(4f))
        drawCircle(strokeColor, radius = 14f, center = bottomRight, style = Stroke(4f))
        drawCircle(strokeColor, radius = 14f, center = bottomLeft, style = Stroke(4f))
    }

    Handle(
        label = stringResource(R.string.editor_corner_top_left),
        actionLabels = listOf(moveUp, moveDown, moveLeft, moveRight),
        point = quad.topLeft,
        imageBounds = imageBounds,
        fillColor = handleFill,
        strokeColor = handleStroke,
        active = activeHandle == HandleAnchor.TOP_LEFT,
        onDragStateChanged = { isDragging ->
            activeHandle = if (isDragging) HandleAnchor.TOP_LEFT else null
        },
        onMoved = { point -> onQuadChange(quad.copy(topLeft = point)) },
    )
    Handle(
        label = stringResource(R.string.editor_corner_top_right),
        actionLabels = listOf(moveUp, moveDown, moveLeft, moveRight),
        point = quad.topRight,
        imageBounds = imageBounds,
        fillColor = handleFill,
        strokeColor = handleStroke,
        active = activeHandle == HandleAnchor.TOP_RIGHT,
        onDragStateChanged = { isDragging ->
            activeHandle = if (isDragging) HandleAnchor.TOP_RIGHT else null
        },
        onMoved = { point -> onQuadChange(quad.copy(topRight = point)) },
    )
    Handle(
        label = stringResource(R.string.editor_corner_bottom_right),
        actionLabels = listOf(moveUp, moveDown, moveLeft, moveRight),
        point = quad.bottomRight,
        imageBounds = imageBounds,
        fillColor = handleFill,
        strokeColor = handleStroke,
        active = activeHandle == HandleAnchor.BOTTOM_RIGHT,
        onDragStateChanged = { isDragging ->
            activeHandle = if (isDragging) HandleAnchor.BOTTOM_RIGHT else null
        },
        onMoved = { point -> onQuadChange(quad.copy(bottomRight = point)) },
    )
    Handle(
        label = stringResource(R.string.editor_corner_bottom_left),
        actionLabels = listOf(moveUp, moveDown, moveLeft, moveRight),
        point = quad.bottomLeft,
        imageBounds = imageBounds,
        fillColor = handleFill,
        strokeColor = handleStroke,
        active = activeHandle == HandleAnchor.BOTTOM_LEFT,
        onDragStateChanged = { isDragging ->
            activeHandle = if (isDragging) HandleAnchor.BOTTOM_LEFT else null
        },
        onMoved = { point -> onQuadChange(quad.copy(bottomLeft = point)) },
    )
}

@Composable
private fun Handle(
    label: String,
    actionLabels: List<String>,
    point: PointValue,
    imageBounds: Rect,
    fillColor: Color,
    strokeColor: Color,
    active: Boolean,
    onDragStateChanged: (Boolean) -> Unit,
    onMoved: (PointValue) -> Unit,
) {
    val touchTarget = 64.dp
    val visualSize = if (active) 34.dp else 28.dp
    val latestPoint by rememberUpdatedState(point)
    fun move(point: PointValue) = onMoved(point.snapToEdge())

    Box(
        modifier = Modifier
            // The page image is never mirrored, so handles use absolute (not RTL-aware) positions.
            .absoluteOffset {
                IntOffset(
                    x = (imageBounds.left + point.x * imageBounds.width).roundToInt() - touchTarget.roundToPx() / 2,
                    y = (imageBounds.top + point.y * imageBounds.height).roundToInt() - touchTarget.roundToPx() / 2,
                )
            }
            .size(touchTarget)
            .semantics {
                contentDescription = label
                customActions = listOf(
                    CustomAccessibilityAction(actionLabels[0]) {
                        move(latestPoint.copy(y = (latestPoint.y - ACCESSIBILITY_STEP).coerceAtLeast(0f)))
                        true
                    },
                    CustomAccessibilityAction(actionLabels[1]) {
                        move(latestPoint.copy(y = (latestPoint.y + ACCESSIBILITY_STEP).coerceAtMost(1f)))
                        true
                    },
                    CustomAccessibilityAction(actionLabels[2]) {
                        move(latestPoint.copy(x = (latestPoint.x - ACCESSIBILITY_STEP).coerceAtLeast(0f)))
                        true
                    },
                    CustomAccessibilityAction(actionLabels[3]) {
                        move(latestPoint.copy(x = (latestPoint.x + ACCESSIBILITY_STEP).coerceAtMost(1f)))
                        true
                    },
                )
            }
            .pointerInput(imageBounds) {
                var currentPoint = latestPoint
                detectDragGestures(
                    onDragStart = {
                        currentPoint = latestPoint
                        onDragStateChanged(true)
                    },
                    onDragEnd = {
                        onDragStateChanged(false)
                    },
                    onDragCancel = {
                        onDragStateChanged(false)
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val moved = PointValue(
                        x = (currentPoint.x + dragAmount.x / imageBounds.width).coerceIn(0f, 1f),
                        y = (currentPoint.y + dragAmount.y / imageBounds.height).coerceIn(0f, 1f),
                    )
                    currentPoint = moved
                    move(moved)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .background(fillColor, CircleShape)
                .border(
                    width = 3.dp,
                    color = if (active) strokeColor else strokeColor.copy(alpha = 0.82f),
                    shape = CircleShape,
                ),
        )
    }
}

private enum class HandleAnchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
}

private const val ACCESSIBILITY_STEP = 0.025f
private const val EDGE_SNAP_THRESHOLD = 0.018f

private fun PointValue.snapToEdge(): PointValue = PointValue(
    x = when {
        x <= EDGE_SNAP_THRESHOLD -> 0f
        x >= 1f - EDGE_SNAP_THRESHOLD -> 1f
        else -> x
    },
    y = when {
        y <= EDGE_SNAP_THRESHOLD -> 0f
        y >= 1f - EDGE_SNAP_THRESHOLD -> 1f
        else -> y
    },
)

private fun Rect.toOffset(point: PointValue): Offset =
    Offset(
        x = left + point.x * width,
        y = top + point.y * height,
    )
