package com.example.robot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.robot.model.RobotDirection

@Composable
fun DirectionalPad(
    activeDirection: RobotDirection,
    enableDiagonals: Boolean,
    hapticEnabled: Boolean,
    onDirectionPressed: (RobotDirection) -> Unit,
    onDirectionReleased: () -> Unit,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 10.dp

    Column(
        modifier = modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth()
            .testTag("directional_pad"),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: FL, FORWARD, FR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            if (enableDiagonals) {
                DirectionKeyButton(
                    direction = RobotDirection.FORWARD_LEFT,
                    icon = Icons.Filled.NorthWest,
                    label = "FL",
                    contentDesc = stringResource(R.string.cd_forward_left),
                    isActive = activeDirection == RobotDirection.FORWARD_LEFT,
                    isDiagonal = true,
                    hapticEnabled = hapticEnabled,
                    onPress = { onDirectionPressed(RobotDirection.FORWARD_LEFT) },
                    onRelease = onDirectionReleased,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("dpad_forward_left")
                )
            } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
            }

            DirectionKeyButton(
                direction = RobotDirection.FORWARD,
                icon = Icons.Filled.KeyboardArrowUp,
                label = "FWD",
                contentDesc = stringResource(R.string.cd_forward),
                isActive = activeDirection == RobotDirection.FORWARD,
                isDiagonal = false,
                hapticEnabled = hapticEnabled,
                onPress = { onDirectionPressed(RobotDirection.FORWARD) },
                onRelease = onDirectionReleased,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("dpad_forward")
            )

            if (enableDiagonals) {
                DirectionKeyButton(
                    direction = RobotDirection.FORWARD_RIGHT,
                    icon = Icons.Filled.NorthEast,
                    label = "FR",
                    contentDesc = stringResource(R.string.cd_forward_right),
                    isActive = activeDirection == RobotDirection.FORWARD_RIGHT,
                    isDiagonal = true,
                    hapticEnabled = hapticEnabled,
                    onPress = { onDirectionPressed(RobotDirection.FORWARD_RIGHT) },
                    onRelease = onDirectionReleased,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("dpad_forward_right")
                )
            } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
            }
        }

        // Middle Row: LEFT, CENTER STOP, RIGHT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            DirectionKeyButton(
                direction = RobotDirection.LEFT,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = "LEFT",
                contentDesc = stringResource(R.string.cd_left),
                isActive = activeDirection == RobotDirection.LEFT,
                isDiagonal = false,
                hapticEnabled = hapticEnabled,
                onPress = { onDirectionPressed(RobotDirection.LEFT) },
                onRelease = onDirectionReleased,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("dpad_left")
            )

            CenterStopHub(
                isIdle = activeDirection == RobotDirection.IDLE,
                hapticEnabled = hapticEnabled,
                onStop = onStopClicked,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("dpad_stop")
            )

            DirectionKeyButton(
                direction = RobotDirection.RIGHT,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = "RIGHT",
                contentDesc = stringResource(R.string.cd_right),
                isActive = activeDirection == RobotDirection.RIGHT,
                isDiagonal = false,
                hapticEnabled = hapticEnabled,
                onPress = { onDirectionPressed(RobotDirection.RIGHT) },
                onRelease = onDirectionReleased,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("dpad_right")
            )
        }

        // Bottom Row: BL, BACKWARD, BR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            if (enableDiagonals) {
                DirectionKeyButton(
                    direction = RobotDirection.BACKWARD_LEFT,
                    icon = Icons.Filled.SouthWest,
                    label = "BL",
                    contentDesc = stringResource(R.string.cd_backward_left),
                    isActive = activeDirection == RobotDirection.BACKWARD_LEFT,
                    isDiagonal = true,
                    hapticEnabled = hapticEnabled,
                    onPress = { onDirectionPressed(RobotDirection.BACKWARD_LEFT) },
                    onRelease = onDirectionReleased,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("dpad_backward_left")
                )
            } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
            }

            DirectionKeyButton(
                direction = RobotDirection.BACKWARD,
                icon = Icons.Filled.KeyboardArrowDown,
                label = "REV",
                contentDesc = stringResource(R.string.cd_backward),
                isActive = activeDirection == RobotDirection.BACKWARD,
                isDiagonal = false,
                hapticEnabled = hapticEnabled,
                onPress = { onDirectionPressed(RobotDirection.BACKWARD) },
                onRelease = onDirectionReleased,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("dpad_backward")
            )

            if (enableDiagonals) {
                DirectionKeyButton(
                    direction = RobotDirection.BACKWARD_RIGHT,
                    icon = Icons.Filled.SouthEast,
                    label = "BR",
                    contentDesc = stringResource(R.string.cd_backward_right),
                    isActive = activeDirection == RobotDirection.BACKWARD_RIGHT,
                    isDiagonal = true,
                    hapticEnabled = hapticEnabled,
                    onPress = { onDirectionPressed(RobotDirection.BACKWARD_RIGHT) },
                    onRelease = onDirectionReleased,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("dpad_backward_right")
                )
            } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun DirectionKeyButton(
    direction: RobotDirection,
    icon: ImageVector,
    label: String,
    contentDesc: String,
    isActive: Boolean,
    isDiagonal: Boolean,
    hapticEnabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var isPressedLocally by remember { mutableStateOf(false) }
    val engaged = isActive || isPressedLocally

    val scale by animateFloatAsState(
        targetValue = if (engaged) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "keyScale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            engaged -> colorScheme.primary
            isDiagonal -> colorScheme.outlineVariant
            else -> colorScheme.outline
        },
        label = "keyBorder"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            engaged -> colorScheme.onPrimary
            isDiagonal -> colorScheme.onSurfaceVariant
            else -> colorScheme.onSurface
        },
        label = "keyIcon"
    )

    val shape = RoundedCornerShape(if (isDiagonal) 18.dp else 22.dp)
    val bgBrush = if (engaged) {
        Brush.verticalGradient(
            colors = listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.82f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (isDiagonal) colorScheme.surface else colorScheme.surfaceContainerHigh,
                colorScheme.surface
            )
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bgBrush)
            .border(width = if (engaged) 2.dp else 1.dp, color = borderColor, shape = shape)
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
                onClick(label = direction.label) {
                    onPress()
                    true
                }
            }
            .pointerInput(direction, hapticEnabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressedLocally = true
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onPress()
                    waitForUpOrCancellation()
                    isPressedLocally = false
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(if (isDiagonal) 24.dp else 36.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (engaged) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                fontWeight = if (engaged) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CenterStopHub(
    isIdle: Boolean,
    hapticEnabled: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "stopScale"
    )
    val hubColor by animateColorAsState(
        targetValue = when {
            pressed -> colorScheme.error
            !isIdle -> colorScheme.errorContainer
            else -> colorScheme.surfaceVariant
        },
        label = "stopBg"
    )
    val contentColor: Color = when {
        pressed -> colorScheme.onError
        !isIdle -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(hubColor)
            .border(
                width = if (!isIdle || pressed) 2.dp else 1.dp,
                color = if (!isIdle || pressed) colorScheme.error else colorScheme.outline,
                shape = CircleShape
            )
            .semantics {
                contentDescription = "Stop robot"
                role = Role.Button
                onClick {
                    onStop()
                    true
                }
            }
            .pointerInput(hapticEnabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onStop()
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "STOP",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
