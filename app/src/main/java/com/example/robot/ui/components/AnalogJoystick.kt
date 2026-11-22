package com.example.robot.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun AnalogJoystick(
    hapticEnabled: Boolean,
    onMove: (normX: Float, normY: Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var rawOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedKnobOffset by animateOffsetAsState(
        targetValue = if (isDragging) rawOffset else Offset.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "joystickKnob"
    )

    Box(
        modifier = modifier
            .widthIn(max = 284.dp)
            .aspectRatio(1f)
            .semantics {
                contentDescription = "360 degree analog robot joystick. Drag to steer robot."
            }
            .testTag("analog_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hapticEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = (minOf(size.width, size.height) / 2f) * 0.72f

                        fun dispatchPosition(pos: Offset) {
                            val delta = pos - center
                            val dist = hypot(delta.x, delta.y)
                            val clamped = if (dist > maxRadius && dist > 0f) {
                                Offset(delta.x / dist * maxRadius, delta.y / dist * maxRadius)
                            } else {
                                delta
                            }
                            rawOffset = clamped
                            val normX = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                            val normY = (clamped.y / maxRadius).coerceIn(-1f, 1f)
                            onMove(normX, normY)
                        }

                        isDragging = true
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        dispatchPosition(down.position)

                        var activePointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                break
                            }
                            activePointerId = change.id
                            change.consume()
                            dispatchPosition(change.position)
                        }

                        isDragging = false
                        rawOffset = Offset.Zero
                        onRelease()
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.48f
            val travelRadius = size.minDimension * 0.36f
            val knobRadius = size.minDimension * 0.145f

            // Outer recessed joystick well
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.surfaceVariant,
                        colorScheme.background
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer precision bezel
            drawCircle(
                color = if (isDragging) colorScheme.primary.copy(alpha = 0.75f) else colorScheme.outline,
                radius = outerRadius,
                center = center,
                style = Stroke(width = if (isDragging) 3.5f else 2f)
            )

            // Guide rings
            drawCircle(
                color = colorScheme.outlineVariant,
                radius = travelRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = colorScheme.outlineVariant.copy(alpha = 0.7f),
                radius = travelRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // 8-way axis spokes
            for (deg in 0 until 360 step 45) {
                val rad = Math.toRadians(deg.toDouble())
                val startR = travelRadius * 0.25f
                val endR = outerRadius * 0.88f
                drawLine(
                    color = colorScheme.outlineVariant.copy(alpha = if (deg % 90 == 0) 0.9f else 0.45f),
                    start = Offset(
                        center.x + (startR * cos(rad)).toFloat(),
                        center.y + (startR * sin(rad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (endR * cos(rad)).toFloat(),
                        center.y + (endR * sin(rad)).toFloat()
                    ),
                    strokeWidth = if (deg % 90 == 0) 1.8f else 1f
                )
            }

            val knobCenter = center + animatedKnobOffset

            // Glowing thrust vector tether
            if (animatedKnobOffset.getDistance() > 4f) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.2f),
                            colorScheme.primary
                        ),
                        start = center,
                        end = knobCenter
                    ),
                    start = center,
                    end = knobCenter,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }

            // Soft cyan halo beneath thumb puck
            if (isDragging) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.35f),
                            colorScheme.primary.copy(alpha = 0f)
                        ),
                        center = knobCenter,
                        radius = knobRadius * 1.65f
                    ),
                    radius = knobRadius * 1.65f,
                    center = knobCenter
                )
            }

            // Thumb puck outer metallic ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorScheme.surfaceContainerHighest,
                        colorScheme.surface
                    ),
                    start = knobCenter - Offset(knobRadius, knobRadius),
                    end = knobCenter + Offset(knobRadius, knobRadius)
                ),
                radius = knobRadius,
                center = knobCenter
            )
            drawCircle(
                color = if (isDragging) colorScheme.primary else colorScheme.outline,
                radius = knobRadius,
                center = knobCenter,
                style = Stroke(width = 3.5f)
            )

            // Center LED core
            drawCircle(
                color = if (isDragging) colorScheme.primary else colorScheme.primary.copy(alpha = 0.65f),
                radius = knobRadius * 0.36f,
                center = knobCenter
            )
        }

        // Cardinal hints around perimeter
        Text(
            text = "FWD",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "REV",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "L",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "R",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
