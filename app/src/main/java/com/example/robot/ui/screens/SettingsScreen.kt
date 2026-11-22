package com.example.robot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.robot.model.CommandMapping
import com.example.robot.model.ControllerSettings
import com.example.robot.model.LineEnding
import com.example.robot.model.ProtocolPreset
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    settings: ControllerSettings,
    onApplyPreset: (ProtocolPreset) -> Unit,
    onSaveMapping: (CommandMapping) -> Unit,
    onSelectLineEnding: (LineEnding) -> Unit,
    onToggleDiagonals: (Boolean) -> Unit,
    onToggleAutoStop: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleRepeat: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var draftMapping by remember(settings.commandMapping) {
        mutableStateOf(settings.commandMapping)
    }
    var savedToast by remember { mutableStateOf(false) }

    LaunchedEffect(savedToast) {
        if (savedToast) {
            delay(1800)
            savedToast = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Controller Config",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )

            // Controller Behavior Toggles
            SectionCard(
                title = "Drive Behavior",
                icon = Icons.Filled.Tune
            ) {
                SettingToggleRow(
                    title = "8-Way Diagonal Steering",
                    subtitle = "Enable FL, FR, BL, BR keys and 8-sector joystick mapping",
                    checked = settings.enableDiagonals,
                    onCheckedChange = onToggleDiagonals,
                    testTag = "toggle_diagonals"
                )
                SettingToggleRow(
                    title = "Auto-Stop on Release",
                    subtitle = "Send STOP command automatically when lifting your finger",
                    checked = settings.autoStopOnRelease,
                    onCheckedChange = onToggleAutoStop,
                    testTag = "toggle_auto_stop"
                )
                SettingToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile vibration when pressing directional controls",
                    checked = settings.hapticEnabled,
                    onCheckedChange = onToggleHaptic,
                    testTag = "toggle_haptic"
                )
                SettingToggleRow(
                    title = "Continuous Keep-Alive Repeat",
                    subtitle = "Re-transmit active direction every 180ms while held",
                    checked = settings.continuousRepeat,
                    onCheckedChange = onToggleRepeat,
                    testTag = "toggle_repeat"
                )
            }

            // Protocol Presets & Line Endings
            SectionCard(
                title = "Serial Command Protocol",
                icon = Icons.Filled.Code
            ) {
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtocolPreset.entries.forEach { preset ->
                        Surface(
                            onClick = { onApplyPreset(preset) },
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preset_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "APPLY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Packet Line Ending",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LineEnding.entries.forEach { ending ->
                        val selected = settings.lineEnding == ending
                        Surface(
                            onClick = { onSelectLineEnding(ending) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colorScheme.primary else colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("line_ending_${ending.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ending.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Custom Direction Payloads",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )

                CommandRowPair(
                    leftLabel = "Forward",
                    leftValue = draftMapping.forward,
                    onLeftChange = { draftMapping = draftMapping.copy(forward = it) },
                    rightLabel = "Reverse",
                    rightValue = draftMapping.backward,
                    onRightChange = { draftMapping = draftMapping.copy(backward = it) }
                )
                CommandRowPair(
                    leftLabel = "Turn Left",
                    leftValue = draftMapping.left,
                    onLeftChange = { draftMapping = draftMapping.copy(left = it) },
                    rightLabel = "Turn Right",
                    rightValue = draftMapping.right,
                    onRightChange = { draftMapping = draftMapping.copy(right = it) }
                )
                CommandRowPair(
                    leftLabel = "Stop",
                    leftValue = draftMapping.stop,
                    onLeftChange = { draftMapping = draftMapping.copy(stop = it) },
                    rightLabel = "Spin 360°",
                    rightValue = draftMapping.spin360,
                    onRightChange = { draftMapping = draftMapping.copy(spin360 = it) }
                )
                CommandRowPair(
                    leftLabel = "Fwd Left",
                    leftValue = draftMapping.forwardLeft,
                    onLeftChange = { draftMapping = draftMapping.copy(forwardLeft = it) },
                    rightLabel = "Fwd Right",
                    rightValue = draftMapping.forwardRight,
                    onRightChange = { draftMapping = draftMapping.copy(forwardRight = it) }
                )
                CommandRowPair(
                    leftLabel = "Rev Left",
                    leftValue = draftMapping.backwardLeft,
                    onLeftChange = { draftMapping = draftMapping.copy(backwardLeft = it) },
                    rightLabel = "Rev Right",
                    rightValue = draftMapping.backwardRight,
                    onRightChange = { draftMapping = draftMapping.copy(backwardRight = it) }
                )
                CommandRowPair(
                    leftLabel = "Horn",
                    leftValue = draftMapping.horn,
                    onLeftChange = { draftMapping = draftMapping.copy(horn = it) },
                    rightLabel = "Lights On",
                    rightValue = draftMapping.lightsOn,
                    onRightChange = { draftMapping = draftMapping.copy(lightsOn = it) }
                )

                Button(
                    onClick = {
                        onSaveMapping(draftMapping)
                        savedToast = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_command_mapping_button")
                ) {
                    if (savedToast) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Protocol Saved")
                    } else {
                        Text("Save Custom Protocol")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )
        }
        content()
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun CommandRowPair(
    leftLabel: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRightChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = leftValue,
            onValueChange = { onLeftChange(it.take(12)) },
            label = { Text(leftLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = rightValue,
            onValueChange = { onRightChange(it.take(12)) },
            label = { Text(rightLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
