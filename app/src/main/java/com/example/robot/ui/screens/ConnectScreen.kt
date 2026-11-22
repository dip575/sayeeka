package com.example.robot.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.robot.model.ConnectionState
import com.example.robot.model.ConsoleLogEntry
import com.example.robot.model.LogDirection
import com.example.robot.model.PairedBluetoothDevice
import com.example.robot.model.TransportType
import com.example.robot.model.WifiConfig
import com.example.robot.viewmodel.RobotUiState

@Composable
fun ConnectScreen(
    uiState: RobotUiState,
    onRefreshBluetooth: () -> Unit,
    onConnectBluetooth: (PairedBluetoothDevice) -> Unit,
    onConnectWifi: (WifiConfig) -> Unit,
    onDisconnect: () -> Unit,
    onSendTestPing: () -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Bluetooth, 1 = Wi-Fi

    var wifiHost by remember(uiState.settings.wifiConfig.host) {
        mutableStateOf(uiState.settings.wifiConfig.host)
    }
    var wifiPortText by remember(uiState.settings.wifiConfig.port) {
        mutableStateOf(uiState.settings.wifiConfig.port.toString())
    }
    var wifiProtocol by remember(uiState.settings.wifiConfig.protocol) {
        mutableStateOf(uiState.settings.wifiConfig.protocol)
    }
    var wifiHttpPath by remember(uiState.settings.wifiConfig.httpPathTemplate) {
        mutableStateOf(uiState.settings.wifiConfig.httpPathTemplate)
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshBluetooth()
    }

    LaunchedEffect(Unit) {
        onRefreshBluetooth()
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
                text = "Robot Connection",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )

            // Rover Hero Banner + Connection Summary Card
            RobotHeroConnectionCard(
                connectionState = uiState.connectionState,
                onDisconnect = onDisconnect,
                onSendTestPing = onSendTestPing
            )

            // Transport Selector (Bluetooth SPP vs Wi-Fi LAN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransportTabButton(
                    label = "Bluetooth SPP",
                    icon = Icons.Filled.Bluetooth,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transport_tab_bluetooth")
                )
                TransportTabButton(
                    label = "Wi-Fi LAN",
                    icon = Icons.Filled.Wifi,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transport_tab_wifi")
                )
            }

            if (selectedTab == 0) {
                BluetoothSection(
                    uiState = uiState,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            btPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        } else {
                            onRefreshBluetooth()
                        }
                    },
                    onOpenSystemBluetooth = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    onRefresh = onRefreshBluetooth,
                    onConnectDevice = onConnectBluetooth
                )
            } else {
                WifiSection(
                    host = wifiHost,
                    onHostChange = { wifiHost = it },
                    portText = wifiPortText,
                    onPortChange = { wifiPortText = it.filter { ch -> ch.isDigit() }.take(5) },
                    protocol = wifiProtocol,
                    onProtocolChange = { wifiProtocol = it },
                    httpPath = wifiHttpPath,
                    onHttpPathChange = { wifiHttpPath = it },
                    isConnecting = uiState.connectionState is ConnectionState.Connecting,
                    onConnect = {
                        val parsedPort = wifiPortText.toIntOrNull() ?: 8080
                        onConnectWifi(
                            WifiConfig(
                                host = wifiHost.trim(),
                                port = parsedPort,
                                protocol = wifiProtocol,
                                httpPathTemplate = wifiHttpPath.trim()
                            )
                        )
                    }
                )
            }

            // Live Serial / Packet Monitor Console
            SerialConsoleCard(
                logs = uiState.consoleLogs,
                onClear = onClearConsole
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RobotHeroConnectionCard(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onSendTestPing: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isConnected = connectionState is ConnectionState.Connected

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                color = if (isConnected) colorScheme.tertiary else colorScheme.outlineVariant,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_robot_banner_1795358224841),
            contentDescription = stringResource(R.string.cd_robot_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background.copy(alpha = 0.25f),
                            colorScheme.background.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val badgeColor = when (connectionState) {
                    is ConnectionState.Connected -> colorScheme.tertiary
                    is ConnectionState.Connecting -> colorScheme.secondary
                    is ConnectionState.Error -> colorScheme.error
                    ConnectionState.Disconnected -> colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "LINK ACTIVE · ${connectionState.transport.label.uppercase()}"
                        is ConnectionState.Connecting -> "ESTABLISHING LINK…"
                        is ConnectionState.Error -> "LINK ERROR"
                        ConnectionState.Disconnected -> "NO HARDWARE PAIRED"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "${connectionState.targetName} (${connectionState.address})"
                        is ConnectionState.Connecting -> "Connecting to ${connectionState.targetName}…"
                        is ConnectionState.Error -> connectionState.message
                        ConnectionState.Disconnected -> "Pair an HC-05 / ESP32 Bluetooth robot or connect over Wi-Fi."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSendTestPing,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("send_ping_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ping")
                        }
                        Button(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BluetoothSection(
    uiState: RobotUiState,
    onRequestPermission: () -> Unit,
    onOpenSystemBluetooth: () -> Unit,
    onRefresh: () -> Unit,
    onConnectDevice: (PairedBluetoothDevice) -> Unit
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paired Bluetooth Robots",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Classic SPP (HC-05, HC-06, ESP32 Serial)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_bluetooth_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh paired Bluetooth devices",
                    tint = colorScheme.primary
                )
            }
        }

        when {
            !uiState.bluetoothAvailable -> {
                BluetoothNoticeBox(
                    icon = Icons.Filled.BluetoothDisabled,
                    title = "Bluetooth Hardware Unavailable",
                    subtitle = "This device does not expose a Bluetooth radio. Switch to the Wi-Fi LAN tab to control Wi-Fi/ESP robots over UDP, TCP, or HTTP."
                )
            }

            !uiState.hasBluetoothPermission -> {
                BluetoothNoticeBox(
                    icon = Icons.Filled.SettingsBluetooth,
                    title = "Bluetooth Access Needed",
                    subtitle = "Grant nearby Bluetooth permission to list paired HC-05 / ESP32 modules and open serial links."
                )
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_bluetooth_permission_button")
                ) {
                    Text("Grant Bluetooth Permission")
                }
            }

            !uiState.bluetoothEnabled -> {
                BluetoothNoticeBox(
                    icon = Icons.Filled.BluetoothDisabled,
                    title = "Bluetooth is Off",
                    subtitle = "Turn on Bluetooth in system settings and pair your robot module first."
                )
                OutlinedButton(
                    onClick = onOpenSystemBluetooth,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_bluetooth_settings_button")
                ) {
                    Text("Open Bluetooth Settings")
                }
            }

            uiState.pairedDevices.isEmpty() -> {
                BluetoothNoticeBox(
                    icon = Icons.Filled.Bluetooth,
                    title = "No Paired Robots Found",
                    subtitle = "Pair your HC-05, HC-06, or ESP32 toy robot in Android Bluetooth settings, then tap refresh."
                )
                OutlinedButton(
                    onClick = onOpenSystemBluetooth,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pair_new_robot_button")
                ) {
                    Text("Pair Robot in System Settings")
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.pairedDevices.forEach { device ->
                        val connectedToThis =
                            (uiState.connectionState as? ConnectionState.Connected)?.address == device.address
                        Surface(
                            onClick = { onConnectDevice(device) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (connectedToThis) {
                                colorScheme.tertiaryContainer
                            } else {
                                colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (connectedToThis) colorScheme.tertiary else colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bt_device_${device.address}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (connectedToThis) {
                                            Icons.Filled.BluetoothConnected
                                        } else {
                                            Icons.Filled.Bluetooth
                                        },
                                        contentDescription = null,
                                        tint = if (connectedToThis) colorScheme.tertiary else colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = if (connectedToThis) "ACTIVE" else "CONNECT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (connectedToThis) colorScheme.tertiary else colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothNoticeBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    }
}

@Composable
private fun WifiSection(
    host: String,
    onHostChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
    protocol: TransportType,
    onProtocolChange: (TransportType) -> Unit,
    httpPath: String,
    onHttpPathChange: (String) -> Unit,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val protocols = listOf(TransportType.WIFI_UDP, TransportType.WIFI_TCP, TransportType.WIFI_HTTP)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Wi-Fi Robot Endpoint",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface
        )
        Text(
            text = "Connect to an ESP32, ESP8266, or Raspberry Pi toy robot on the same Wi-Fi network or AP hotspot.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            protocols.forEach { item ->
                val selected = protocol == item
                Surface(
                    onClick = { onProtocolChange(item) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (selected) colorScheme.primary else colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("wifi_protocol_${item.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.label.removePrefix("Wi-Fi "),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Robot IP / Host") },
                singleLine = true,
                modifier = Modifier
                    .weight(1.7f)
                    .testTag("wifi_host_input")
            )
            OutlinedTextField(
                value = portText,
                onValueChange = onPortChange,
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_port_input")
            )
        }

        if (protocol == TransportType.WIFI_HTTP) {
            OutlinedTextField(
                value = httpPath,
                onValueChange = onHttpPathChange,
                label = { Text("HTTP Path ({cmd}, {speed})") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_http_path_input")
            )
        }

        Button(
            onClick = onConnect,
            enabled = !isConnecting,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("wifi_connect_button")
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Connecting…")
            } else {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect via ${protocol.label}")
            }
        }
    }
}

@Composable
private fun SerialConsoleCard(
    logs: List<ConsoleLogEntry>,
    onClear: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("serial_console_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LIVE PACKET CONSOLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag("clear_console_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear console logs",
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "No packets transmitted yet. Drive from Control Deck or connect a robot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            } else {
                logs.take(15).forEach { entry ->
                    val dirColor = when (entry.direction) {
                        LogDirection.TX -> colorScheme.primary
                        LogDirection.RX -> colorScheme.tertiary
                        LogDirection.SYS -> colorScheme.secondary
                        LogDirection.ERR -> colorScheme.error
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entry.direction.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = dirColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = entry.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
