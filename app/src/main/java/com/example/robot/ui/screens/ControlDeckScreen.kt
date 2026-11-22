package com.example.robot.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.robot.model.ConnectionState
import com.example.robot.model.ControlMode
import com.example.robot.model.RobotDirection
import com.example.robot.model.TransportType
import com.example.robot.ui.components.AnalogJoystick
import com.example.robot.ui.components.DirectionalPad
import com.example.robot.ui.components.DirectionalVectorHud
import com.example.robot.viewmodel.RobotUiState
import kotlin.math.roundToInt

@Composable
fun ControlDeckScreen(
    uiState: RobotUiState,
    onSelectMode: (ControlMode) -> Unit,
    onDirectionPressed: (RobotDirection) -> Unit,
    onDirectionReleased: () -> Unit,
    onJoystickMoved: (Float, Float) -> Unit,
    onJoystickReleased: () -> Unit,
    onEmergencyStop: () -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onHornClick: () -> Unit,
    onToggleLights: () -> Unit,
    onSpinClick: () -> Unit,
    onOpenConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = uiState.connectionState is ConnectionState.Connected

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Telemetry & Throttle Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TopCockpitHeader(
                        connectionState = uiState.connectionState,
                        onOpenConnect = onOpenConnect
                    )
                    DirectionalVectorHud(
                        direction = uiState.activeDirection,
                        speedPercent = uiState.speedPercent,
                        lightsOn = uiState.lightsOn,
                        lastPacketSummary = uiState.lastPacketSummary,
                        isConnected = isConnected
                    )
                    SpeedThrottleCard(
                        speedPercent = uiState.speedPercent,
                        onSpeedChanged = onSpeedChanged
                    )
                    AuxActionsRow(
                        hornActive = uiState.hornActive,
                        lightsOn = uiState.lightsOn,
                        spinActive = uiState.activeDirection == RobotDirection.SPIN_360,
                        onHornClick = onHornClick,
                        onToggleLights = onToggleLights,
                        onSpinClick = onSpinClick
                    )
                    EmergencyStopButton(onEmergencyStop = onEmergencyStop)
                }

                // Right Active Control Surface Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ControlModeSwitcher(
                        selectedMode = uiState.controlMode,
                        onSelectMode = onSelectMode
                    )
                    ControlPadContainer(
                        uiState = uiState,
                        onDirectionPressed = onDirectionPressed,
                        onDirectionReleased = onDirectionReleased,
                        onJoystickMoved = onJoystickMoved,
                        onJoystickReleased = onJoystickReleased,
                        onEmergencyStop = onEmergencyStop
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopCockpitHeader(
                    connectionState = uiState.connectionState,
                    onOpenConnect = onOpenConnect
                )

                DirectionalVectorHud(
                    direction = uiState.activeDirection,
                    speedPercent = uiState.speedPercent,
                    lightsOn = uiState.lightsOn,
                    lastPacketSummary = uiState.lastPacketSummary,
                    isConnected = isConnected
                )

                ControlModeSwitcher(
                    selectedMode = uiState.controlMode,
                    onSelectMode = onSelectMode
                )

                ControlPadContainer(
                    uiState = uiState,
                    onDirectionPressed = onDirectionPressed,
                    onDirectionReleased = onDirectionReleased,
                    onJoystickMoved = onJoystickMoved,
                    onJoystickReleased = onJoystickReleased,
                    onEmergencyStop = onEmergencyStop
                )

                SpeedThrottleCard(
                    speedPercent = uiState.speedPercent,
                    onSpeedChanged = onSpeedChanged
                )

                AuxActionsRow(
                    hornActive = uiState.hornActive,
                    lightsOn = uiState.lightsOn,
                    spinActive = uiState.activeDirection == RobotDirection.SPIN_360,
                    onHornClick = onHornClick,
                    onToggleLights = onToggleLights,
                    onSpinClick = onSpinClick
                )

                EmergencyStopButton(onEmergencyStop = onEmergencyStop)

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TopCockpitHeader(
    connectionState: ConnectionState,
    onOpenConnect: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val (statusText, statusColor, statusIcon) = when (connectionState) {
        is ConnectionState.Connected -> Triple(
            connectionState.targetName,
            colorScheme.tertiary,
            if (connectionState.transport == TransportType.BLUETOOTH_SPP) {
                Icons.Filled.BluetoothConnected
            } else {
                Icons.Filled.Wifi
            }
        )
        is ConnectionState.Connecting -> Triple(
            stringResource(R.string.status_connecting),
            colorScheme.secondary,
            Icons.Filled.Radar
        )
        is ConnectionState.Error -> Triple(
            "Link Error",
            colorScheme.error,
            Icons.Filled.LinkOff
        )
        ConnectionState.Disconnected -> Triple(
            stringResource(R.string.status_disconnected),
            colorScheme.onSurfaceVariant,
            Icons.Filled.LinkOff
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )
            Text(
                text = "ROBOT TELEMETRY DECK",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary
            )
        }

        Surface(
            onClick = onOpenConnect,
            shape = RoundedCornerShape(999.dp),
            color = colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                if (connectionState is ConnectionState.Connected) statusColor else colorScheme.outline
            ),
            modifier = Modifier.testTag("connection_status_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ControlModeSwitcher(
    selectedMode: ControlMode,
    onSelectMode: (ControlMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .testTag("control_mode_switcher"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ModeSegmentTab(
            title = stringResource(R.string.mode_dpad),
            icon = Icons.Filled.Gamepad,
            selected = selectedMode == ControlMode.DPAD,
            onClick = { onSelectMode(ControlMode.DPAD) },
            modifier = Modifier
                .weight(1f)
                .testTag("mode_tab_dpad")
        )
        ModeSegmentTab(
            title = stringResource(R.string.mode_joystick),
            icon = Icons.Filled.Radar,
            selected = selectedMode == ControlMode.JOYSTICK,
            onClick = { onSelectMode(ControlMode.JOYSTICK) },
            modifier = Modifier
                .weight(1f)
                .testTag("mode_tab_joystick")
        )
    }
}

@Composable
private fun ModeSegmentTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else Color.Transparent,
        label = "modeTabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        label = "modeTabContent"
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) colorScheme.primary.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Composable
private fun ControlPadContainer(
    uiState: RobotUiState,
    onDirectionPressed: (RobotDirection) -> Unit,
    onDirectionReleased: () -> Unit,
    onJoystickMoved: (Float, Float) -> Unit,
    onJoystickReleased: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = uiState.controlMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "controlSurface"
        ) { mode ->
            when (mode) {
                ControlMode.DPAD -> {
                    DirectionalPad(
                        activeDirection = uiState.activeDirection,
                        enableDiagonals = uiState.settings.enableDiagonals,
                        hapticEnabled = uiState.settings.hapticEnabled,
                        onDirectionPressed = onDirectionPressed,
                        onDirectionReleased = onDirectionReleased,
                        onStopClicked = onEmergencyStop
                    )
                }

                ControlMode.JOYSTICK -> {
                    AnalogJoystick(
                        hapticEnabled = uiState.settings.hapticEnabled,
                        onMove = onJoystickMoved,
                        onRelease = onJoystickReleased
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedThrottleCard(
    speedPercent: Int,
    onSpeedChanged: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val gears = listOf("LOW" to 40, "MED" to 70, "TURBO" to 100)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("speed_throttle_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "DRIVE THROTTLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface
                )
            }
            Text(
                text = "$speedPercent%",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = speedPercent.toFloat(),
            onValueChange = { onSpeedChanged(it.roundToInt()) },
            valueRange = 20f..100f,
            steps = 7,
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.secondary,
                activeTrackColor = colorScheme.secondary,
                inactiveTrackColor = colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("speed_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gears.forEach { (label, pct) ->
                val selected = speedPercent == pct
                Surface(
                    onClick = { onSpeedChanged(pct) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (selected) colorScheme.secondary else colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("speed_gear_${label.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$label · $pct%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colorScheme.secondary else colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuxActionsRow(
    hornActive: Boolean,
    lightsOn: Boolean,
    spinActive: Boolean,
    onHornClick: () -> Unit,
    onToggleLights: () -> Unit,
    onSpinClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuxActionTile(
            label = stringResource(R.string.action_horn),
            icon = Icons.Filled.Campaign,
            active = hornActive,
            activeColor = MaterialTheme.colorScheme.secondary,
            onClick = onHornClick,
            modifier = Modifier
                .weight(1f)
                .testTag("aux_horn_button")
        )
        AuxActionTile(
            label = stringResource(R.string.action_lights),
            icon = if (lightsOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
            active = lightsOn,
            activeColor = MaterialTheme.colorScheme.primary,
            onClick = onToggleLights,
            modifier = Modifier
                .weight(1f)
                .testTag("aux_lights_button")
        )
        AuxActionTile(
            label = stringResource(R.string.action_spin),
            icon = Icons.Filled.Refresh,
            active = spinActive,
            activeColor = MaterialTheme.colorScheme.tertiary,
            onClick = onSpinClick,
            modifier = Modifier
                .weight(1f)
                .testTag("aux_spin_button")
        )
    }
}

@Composable
private fun AuxActionTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) activeColor.copy(alpha = 0.18f) else colorScheme.surface,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) activeColor else colorScheme.outlineVariant
        ),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeColor else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) activeColor else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmergencyStopButton(
    onEmergencyStop: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Button(
        onClick = onEmergencyStop,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.error
        ),
        border = BorderStroke(1.5.dp, colorScheme.error),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("emergency_stop_button")
    ) {
        Icon(
            imageVector = Icons.Filled.Report,
            contentDescription = stringResource(R.string.cd_stop),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.emergency_stop),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
