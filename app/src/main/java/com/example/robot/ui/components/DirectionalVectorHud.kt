package com.example.robot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.robot.model.RobotDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DirectionalVectorHud(
    direction: RobotDirection,
    speedPercent: Int,
    lightsOn: Boolean,
    lastPacketSummary: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isActive = direction != RobotDirection.IDLE
    val speedFactor = if (isActive) speedPercent / 100f else 0f

    val leftPower by animateFloatAsState(
        targetValue = direction.leftMotor * speedFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "leftPower"
    )
    val rightPower by animateFloatAsState(
        targetValue = direction.rightMotor * speedFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rightPower"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hudPulse")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val targetHeading = direction.headingDegrees ?: 0f
    val animatedHeading by animateFloatAsState(
        targetValue = targetHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heading"
    )

    val accentColor by animateColorAsState(
        targetValue = when (direction) {
            RobotDirection.IDLE -> colorScheme.outline
            RobotDirection.SPIN_360 -> colorScheme.secondary
            else -> colorScheme.primary
        },
        label = "hudAccent"
    )

    val displayHeading = if (direction == RobotDirection.SPIN_360) spinAngle else animatedHeading

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.surface,
                        colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (isActive) accentColor.copy(alpha = 0.65f) else colorScheme.outlineVariant,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp)
            .testTag("directional_vector_hud")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Top-Down Rover & Heading Radar
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.background.copy(alpha = 0.85f))
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.44f

                    // Radar rings
                    drawCircle(
                        color = colorScheme.outlineVariant,
                        radius = maxR,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = colorScheme.outlineVariant.copy(alpha = 0.6f),
                        radius = maxR * 0.62f,
                        style = Stroke(width = 1f)
                    )

                    // Cardinal compass ticks
                    for (deg in 0 until 360 step 45) {
                        val rad = Math.toRadians(deg.toDouble())
                        val inner = if (deg % 90 == 0) maxR * 0.82f else maxR * 0.9f
                        drawLine(
                            color = colorScheme.outline,
                            start = Offset(
                                cx + (inner * cos(rad)).toFloat(),
                                cy + (inner * sin(rad)).toFloat()
                            ),
                            end = Offset(
                                cx + (maxR * cos(rad)).toFloat(),
                                cy + (maxR * sin(rad)).toFloat()
                            ),
                            strokeWidth = if (deg % 90 == 0) 2.2f else 1.2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Active heading glow cone / vector arrow
                    if (isActive) {
                        rotate(degrees = displayHeading, pivot = Offset(cx, cy)) {
                            val arrowTip = Offset(cx, cy - maxR * 0.92f)
                            drawLine(
                                color = accentColor,
                                start = Offset(cx, cy),
                                end = arrowTip,
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                            val headPath = Path().apply {
                                moveTo(arrowTip.x, arrowTip.y - 3f)
                                lineTo(arrowTip.x - 7f, arrowTip.y + 10f)
                                lineTo(arrowTip.x + 7f, arrowTip.y + 10f)
                                close()
                            }
                            drawPath(headPath, color = accentColor)
                        }
                    }

                    // Miniature top-down toy robot rover chassis
                    rotate(degrees = if (isActive) displayHeading * 0.25f else 0f, pivot = Offset(cx, cy)) {
                        val bodyW = 24f
                        val bodyH = 34f
                        val wheelW = 6f
                        val wheelH = 11f

                        // Headlight beams when Lights ON
                        if (lightsOn) {
                            val beamPath = Path().apply {
                                moveTo(cx - 8f, cy - bodyH / 2f)
                                lineTo(cx - 22f, cy - maxR)
                                lineTo(cx + 22f, cy - maxR)
                                lineTo(cx + 8f, cy - bodyH / 2f)
                                close()
                            }
                            drawPath(
                                path = beamPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colorScheme.secondary.copy(alpha = 0.0f),
                                        colorScheme.secondary.copy(alpha = 0.42f)
                                    ),
                                    startY = cy - maxR,
                                    endY = cy - bodyH / 2f
                                )
                            )
                        }

                        // 4 wheels
                        val wheelColor = colorScheme.onSurfaceVariant
                        val wheelOffsets = listOf(
                            Offset(cx - bodyW / 2f - wheelW + 1f, cy - bodyH / 2f + 2f),
                            Offset(cx + bodyW / 2f - 1f, cy - bodyH / 2f + 2f),
                            Offset(cx - bodyW / 2f - wheelW + 1f, cy + bodyH / 2f - wheelH - 2f),
                            Offset(cx + bodyW / 2f - 1f, cy + bodyH / 2f - wheelH - 2f)
                        )
                        wheelOffsets.forEach { topLeft ->
                            drawRoundRect(
                                color = wheelColor,
                                topLeft = topLeft,
                                size = Size(wheelW, wheelH),
                                cornerRadius = CornerRadius(2.5f, 2.5f)
                            )
                        }

                        // Chassis body
                        drawRoundRect(
                            color = colorScheme.surfaceContainerHighest,
                            topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
                            size = Size(bodyW, bodyH),
                            cornerRadius = CornerRadius(7f, 7f)
                        )
                        drawRoundRect(
                            color = if (isActive) accentColor else colorScheme.outline,
                            topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
                            size = Size(bodyW, bodyH),
                            cornerRadius = CornerRadius(7f, 7f),
                            style = Stroke(width = 2f)
                        )

                        // Visor LED bar
                        drawRoundRect(
                            color = if (lightsOn) colorScheme.secondary else colorScheme.primary,
                            topLeft = Offset(cx - 7f, cy - bodyH / 2f + 5f),
                            size = Size(14f, 4f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }

            // Telemetry Readout & Differential Motor Bars
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = direction.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive) accentColor else colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${(speedFactor * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) colorScheme.secondary else colorScheme.onSurfaceVariant
                    )
                }

                MotorDriveBar(
                    label = "L-MTR",
                    power = leftPower,
                    activeColor = colorScheme.primary,
                    reverseColor = colorScheme.secondary
                )

                MotorDriveBar(
                    label = "R-MTR",
                    power = rightPower,
                    activeColor = colorScheme.primary,
                    reverseColor = colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TX $lastPacketSummary",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MotorDriveBar(
    label: String,
    power: Float,
    activeColor: Color,
    reverseColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val pct = (power * 100).roundToInt()
    val barColor = when {
        power > 0.02f -> activeColor
        power < -0.02f -> reverseColor
        else -> colorScheme.outline
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.width(38.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorScheme.background)
        ) {
            val fillFraction = abs(power).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Text(
            text = if (pct > 0) "+$pct%" else "$pct%",
            style = MaterialTheme.typography.labelSmall,
            color = if (pct == 0) colorScheme.onSurfaceVariant else barColor,
            modifier = Modifier.width(38.dp)
        )
    }
}
