package com.example.robot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.robot.ui.screens.ConnectScreen
import com.example.robot.ui.screens.ControlDeckScreen
import com.example.robot.ui.screens.SettingsScreen
import com.example.robot.viewmodel.RobotControllerViewModel

enum class RoboPilotDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONTROL("control", R.string.nav_control, Icons.Filled.Gamepad, Icons.Outlined.Gamepad),
    CONNECT("connect", R.string.nav_connect, Icons.Filled.Link, Icons.Outlined.Link),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Tune, Icons.Outlined.Tune)
}

@Composable
fun RoboPilotApp(
    viewModel: RobotControllerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(RoboPilotDestination.CONTROL) }

    BackHandler(enabled = currentDestination != RoboPilotDestination.CONTROL) {
        currentDestination = RoboPilotDestination.CONTROL
    }

    val colorScheme = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        val navContent: @Composable (Modifier) -> Unit = { contentModifier ->
            when (currentDestination) {
                RoboPilotDestination.CONTROL -> ControlDeckScreen(
                    uiState = uiState,
                    onSelectMode = viewModel::setControlMode,
                    onDirectionPressed = viewModel::onDirectionPressed,
                    onDirectionReleased = viewModel::onDirectionReleased,
                    onJoystickMoved = viewModel::onJoystickMoved,
                    onJoystickReleased = viewModel::onJoystickReleased,
                    onEmergencyStop = viewModel::emergencyStop,
                    onSpeedChanged = viewModel::setSpeedPercent,
                    onHornClick = viewModel::triggerHorn,
                    onToggleLights = viewModel::toggleLights,
                    onSpinClick = viewModel::triggerSpin360,
                    onOpenConnect = { currentDestination = RoboPilotDestination.CONNECT },
                    modifier = contentModifier
                )

                RoboPilotDestination.CONNECT -> ConnectScreen(
                    uiState = uiState,
                    onRefreshBluetooth = viewModel::refreshBluetoothState,
                    onConnectBluetooth = viewModel::connectBluetoothDevice,
                    onConnectWifi = viewModel::connectWifi,
                    onDisconnect = viewModel::disconnect,
                    onSendTestPing = viewModel::sendTestPing,
                    onClearConsole = viewModel::clearConsole,
                    modifier = contentModifier
                )

                RoboPilotDestination.SETTINGS -> SettingsScreen(
                    settings = uiState.settings,
                    onApplyPreset = viewModel::applyProtocolPreset,
                    onSaveMapping = viewModel::updateCommandMapping,
                    onSelectLineEnding = viewModel::updateLineEnding,
                    onToggleDiagonals = viewModel::setEnableDiagonals,
                    onToggleAutoStop = viewModel::setAutoStopOnRelease,
                    onToggleHaptic = viewModel::setHapticEnabled,
                    onToggleRepeat = viewModel::setContinuousRepeat,
                    modifier = contentModifier
                )
            }
        }

        if (useNavigationRail) {
            Scaffold(
                containerColor = colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = colorScheme.surface,
                        contentColor = colorScheme.onSurface
                    ) {
                        RoboPilotDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colorScheme.primary,
                                    selectedTextColor = colorScheme.primary,
                                    indicatorColor = colorScheme.primaryContainer,
                                    unselectedIconColor = colorScheme.onSurfaceVariant,
                                    unselectedTextColor = colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_rail_${dest.route}")
                            )
                        }
                    }
                    navContent(Modifier.weight(1f))
                }
            }
        } else {
            Scaffold(
                containerColor = colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        RoboPilotDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.primary,
                                    selectedTextColor = colorScheme.primary,
                                    indicatorColor = colorScheme.primaryContainer,
                                    unselectedIconColor = colorScheme.onSurfaceVariant,
                                    unselectedTextColor = colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_bar_${dest.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                navContent(Modifier.padding(innerPadding))
            }
        }
    }
}
