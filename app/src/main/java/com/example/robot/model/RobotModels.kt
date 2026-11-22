package com.example.robot.model

enum class RobotDirection(
    val label: String,
    val shortCode: String,
    val headingDegrees: Float?,
    val leftMotor: Float,
    val rightMotor: Float
) {
    IDLE("STOPPED", "STOP", null, 0f, 0f),
    FORWARD("FORWARD", "FWD", 0f, 1f, 1f),
    BACKWARD("REVERSE", "REV", 180f, -1f, -1f),
    LEFT("TURN LEFT", "LEFT", -90f, -0.85f, 0.85f),
    RIGHT("TURN RIGHT", "RIGHT", 90f, 0.85f, -0.85f),
    FORWARD_LEFT("FWD LEFT", "FWD-L", -45f, 0.45f, 1f),
    FORWARD_RIGHT("FWD RIGHT", "FWD-R", 45f, 1f, 0.45f),
    BACKWARD_LEFT("REV LEFT", "REV-L", -135f, -0.45f, -1f),
    BACKWARD_RIGHT("REV RIGHT", "REV-R", 135f, -1f, -0.45f),
    SPIN_360("SPIN 360°", "SPIN", 90f, 1f, -1f)
}

enum class ControlMode {
    DPAD,
    JOYSTICK
}

enum class TransportType(val label: String, val subtitle: String) {
    BLUETOOTH_SPP("Bluetooth SPP", "HC-05 / HC-06 / ESP32 Serial"),
    WIFI_UDP("Wi-Fi UDP", "Low-latency datagram packets"),
    WIFI_TCP("Wi-Fi TCP", "Persistent socket stream"),
    WIFI_HTTP("Wi-Fi HTTP", "REST GET endpoint (/cmd)")
}

enum class LineEnding(val label: String, val suffix: String) {
    NONE("None", ""),
    LF("LF (\\n)", "\n"),
    CRLF("CRLF (\\r\\n)", "\r\n")
}

enum class ProtocolPreset(val title: String, val subtitle: String) {
    ARDUINO_RC("Arduino RC", "F / B / L / R / S"),
    WORD_COMMANDS("Word Protocol", "FWD / REV / LEFT / RIGHT / STOP"),
    NUMERIC_PAD("Numeric Pad", "8 / 2 / 4 / 6 / 5")
}

data class CommandMapping(
    val forward: String = "F",
    val backward: String = "B",
    val left: String = "L",
    val right: String = "R",
    val stop: String = "S",
    val forwardLeft: String = "G",
    val forwardRight: String = "I",
    val backwardLeft: String = "H",
    val backwardRight: String = "J",
    val horn: String = "V",
    val lightsOn: String = "W",
    val lightsOff: String = "w",
    val spin360: String = "X"
) {
    fun commandFor(direction: RobotDirection): String = when (direction) {
        RobotDirection.IDLE -> stop
        RobotDirection.FORWARD -> forward
        RobotDirection.BACKWARD -> backward
        RobotDirection.LEFT -> left
        RobotDirection.RIGHT -> right
        RobotDirection.FORWARD_LEFT -> forwardLeft
        RobotDirection.FORWARD_RIGHT -> forwardRight
        RobotDirection.BACKWARD_LEFT -> backwardLeft
        RobotDirection.BACKWARD_RIGHT -> backwardRight
        RobotDirection.SPIN_360 -> spin360
    }

    companion object {
        fun fromPreset(preset: ProtocolPreset): CommandMapping = when (preset) {
            ProtocolPreset.ARDUINO_RC -> CommandMapping()
            ProtocolPreset.WORD_COMMANDS -> CommandMapping(
                forward = "FWD",
                backward = "REV",
                left = "LEFT",
                right = "RIGHT",
                stop = "STOP",
                forwardLeft = "FWD_L",
                forwardRight = "FWD_R",
                backwardLeft = "REV_L",
                backwardRight = "REV_R",
                horn = "HORN",
                lightsOn = "LED_ON",
                lightsOff = "LED_OFF",
                spin360 = "SPIN"
            )
            ProtocolPreset.NUMERIC_PAD -> CommandMapping(
                forward = "8",
                backward = "2",
                left = "4",
                right = "6",
                stop = "5",
                forwardLeft = "7",
                forwardRight = "9",
                backwardLeft = "1",
                backwardRight = "3",
                horn = "0",
                lightsOn = "*",
                lightsOff = "#",
                spin360 = "R"
            )
        }
    }
}

data class WifiConfig(
    val host: String = "192.168.4.1",
    val port: Int = 8080,
    val protocol: TransportType = TransportType.WIFI_UDP,
    val httpPathTemplate: String = "/cmd?dir={cmd}&speed={speed}"
)

data class ControllerSettings(
    val commandMapping: CommandMapping = CommandMapping(),
    val lineEnding: LineEnding = LineEnding.LF,
    val enableDiagonals: Boolean = true,
    val autoStopOnRelease: Boolean = true,
    val hapticEnabled: Boolean = true,
    val continuousRepeat: Boolean = false,
    val wifiConfig: WifiConfig = WifiConfig()
)

data class PairedBluetoothDevice(
    val name: String,
    val address: String
)

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val targetName: String, val transport: TransportType) : ConnectionState()
    data class Connected(val targetName: String, val address: String, val transport: TransportType) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

enum class LogDirection {
    TX,
    RX,
    SYS,
    ERR
}

data class ConsoleLogEntry(
    val id: Long,
    val timestamp: String,
    val direction: LogDirection,
    val message: String,
    val delivered: Boolean = true
)
