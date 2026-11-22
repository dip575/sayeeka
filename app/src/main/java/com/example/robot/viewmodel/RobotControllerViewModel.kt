package com.example.robot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.robot.data.RobotTransportManager
import com.example.robot.data.SettingsRepository
import com.example.robot.model.CommandMapping
import com.example.robot.model.ConnectionState
import com.example.robot.model.ConsoleLogEntry
import com.example.robot.model.ControlMode
import com.example.robot.model.ControllerSettings
import com.example.robot.model.LineEnding
import com.example.robot.model.LogDirection
import com.example.robot.model.PairedBluetoothDevice
import com.example.robot.model.ProtocolPreset
import com.example.robot.model.RobotDirection
import com.example.robot.model.WifiConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class RobotUiState(
    val activeDirection: RobotDirection = RobotDirection.IDLE,
    val controlMode: ControlMode = ControlMode.DPAD,
    val speedPercent: Int = 70,
    val analogVectorX: Float = 0f,
    val analogVectorY: Float = 0f,
    val analogMagnitude: Float = 0f,
    val lightsOn: Boolean = false,
    val hornActive: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val settings: ControllerSettings = ControllerSettings(),
    val pairedDevices: List<PairedBluetoothDevice> = emptyList(),
    val bluetoothAvailable: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val lastPacketSummary: String = "IDLE"
)

class RobotControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val transport = RobotTransportManager(application)
    private val logCounter = AtomicLong(0L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _uiState = MutableStateFlow(RobotUiState())
    val uiState: StateFlow<RobotUiState> = _uiState.asStateFlow()

    private var repeatJob: Job? = null
    private var hornJob: Job? = null
    private var spinJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            transport.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
                when (connState) {
                    is ConnectionState.Connected -> appendLog(
                        LogDirection.SYS,
                        "Connected via ${connState.transport.label} (${connState.address})"
                    )
                    is ConnectionState.Error -> appendLog(
                        LogDirection.ERR,
                        connState.message,
                        delivered = false
                    )
                    ConnectionState.Disconnected -> Unit
                    is ConnectionState.Connecting -> appendLog(
                        LogDirection.SYS,
                        "Connecting to ${connState.targetName}…"
                    )
                }
            }
        }
        viewModelScope.launch {
            transport.incomingMessages.collect { msg ->
                appendLog(LogDirection.RX, msg)
            }
        }
        refreshBluetoothState()
    }

    fun refreshBluetoothState() {
        val available = transport.isBluetoothAvailable()
        val enabled = transport.isBluetoothEnabled()
        val permitted = transport.hasBluetoothPermission()
        val paired = if (available && enabled && permitted) {
            transport.getPairedBluetoothDevices()
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                bluetoothAvailable = available,
                bluetoothEnabled = enabled,
                hasBluetoothPermission = permitted,
                pairedDevices = paired
            )
        }
    }

    fun setControlMode(mode: ControlMode) {
        if (_uiState.value.controlMode != mode) {
            stopActiveMotion()
            _uiState.update {
                it.copy(
                    controlMode = mode,
                    analogVectorX = 0f,
                    analogVectorY = 0f,
                    analogMagnitude = 0f
                )
            }
        }
    }

    fun onDirectionPressed(direction: RobotDirection) {
        spinJob?.cancel()
        if (_uiState.value.activeDirection == direction && direction != RobotDirection.IDLE) return

        _uiState.update {
            it.copy(
                activeDirection = direction,
                analogVectorX = directionVectorX(direction),
                analogVectorY = directionVectorY(direction),
                analogMagnitude = if (direction == RobotDirection.IDLE) 0f else it.speedPercent / 100f
            )
        }
        dispatchDirectionCommand(direction)
        startRepeatLoopIfNeeded(direction)
    }

    fun onDirectionReleased() {
        repeatJob?.cancel()
        if (_uiState.value.settings.autoStopOnRelease) {
            stopActiveMotion()
        }
    }

    fun onJoystickMoved(normX: Float, normY: Float) {
        spinJob?.cancel()
        val clampedMag = hypot(normX, normY).coerceIn(0f, 1f)
        if (clampedMag < 0.18f) {
            if (_uiState.value.activeDirection != RobotDirection.IDLE) {
                stopActiveMotion()
            } else {
                _uiState.update {
                    it.copy(analogVectorX = 0f, analogVectorY = 0f, analogMagnitude = 0f)
                }
            }
            return
        }

        // normY negative is UP (forward) in screen coordinates
        val angleDeg = Math.toDegrees(atan2(normX.toDouble(), (-normY).toDouble())).toFloat()
        val direction = mapAngleToDirection(angleDeg, _uiState.value.settings.enableDiagonals)
        val prevDirection = _uiState.value.activeDirection

        _uiState.update {
            it.copy(
                activeDirection = direction,
                analogVectorX = normX,
                analogVectorY = normY,
                analogMagnitude = clampedMag
            )
        }

        if (direction != prevDirection) {
            dispatchDirectionCommand(direction)
            startRepeatLoopIfNeeded(direction)
        }
    }

    fun onJoystickReleased() {
        repeatJob?.cancel()
        _uiState.update {
            it.copy(analogVectorX = 0f, analogVectorY = 0f, analogMagnitude = 0f)
        }
        if (_uiState.value.settings.autoStopOnRelease) {
            stopActiveMotion()
        }
    }

    fun emergencyStop() {
        repeatJob?.cancel()
        spinJob?.cancel()
        _uiState.update {
            it.copy(
                activeDirection = RobotDirection.IDLE,
                analogVectorX = 0f,
                analogVectorY = 0f,
                analogMagnitude = 0f
            )
        }
        dispatchDirectionCommand(RobotDirection.IDLE, forceLogLabel = "E-STOP")
    }

    fun setSpeedPercent(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        _uiState.update { it.copy(speedPercent = clamped) }
        // Standard single-digit throttle level (1..9 or q for max) compatible with Arduino RC sketches
        val speedCode = when {
            clamped >= 100 -> "q"
            else -> ((clamped / 10).coerceIn(1, 9)).toString()
        }
        transmitRaw(speedCode, "SPEED $clamped%")
    }

    fun triggerHorn() {
        hornJob?.cancel()
        val cmd = _uiState.value.settings.commandMapping.horn
        _uiState.update { it.copy(hornActive = true) }
        transmitRaw(cmd, "HORN")
        hornJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(hornActive = false) }
        }
    }

    fun toggleLights() {
        val nextOn = !_uiState.value.lightsOn
        val mapping = _uiState.value.settings.commandMapping
        val cmd = if (nextOn) mapping.lightsOn else mapping.lightsOff
        _uiState.update { it.copy(lightsOn = nextOn) }
        transmitRaw(cmd, if (nextOn) "LIGHTS ON" else "LIGHTS OFF")
    }

    fun triggerSpin360() {
        repeatJob?.cancel()
        spinJob?.cancel()
        _uiState.update {
            it.copy(
                activeDirection = RobotDirection.SPIN_360,
                analogMagnitude = it.speedPercent / 100f
            )
        }
        dispatchDirectionCommand(RobotDirection.SPIN_360)
        spinJob = viewModelScope.launch {
            delay(1200)
            stopActiveMotion()
        }
    }

    fun connectBluetoothDevice(device: PairedBluetoothDevice) {
        viewModelScope.launch {
            transport.connectBluetooth(device)
        }
    }

    fun connectWifi(config: WifiConfig) {
        viewModelScope.launch {
            settingsRepo.saveWifiConfig(config)
            transport.connectWifi(config)
        }
    }

    fun disconnect() {
        repeatJob?.cancel()
        spinJob?.cancel()
        viewModelScope.launch {
            if (_uiState.value.connectionState is ConnectionState.Connected) {
                val stopCmd = _uiState.value.settings.commandMapping.stop
                transport.sendCommand(
                    stopCmd,
                    _uiState.value.settings.lineEnding.suffix,
                    _uiState.value.speedPercent
                )
            }
            transport.disconnect()
            _uiState.update {
                it.copy(
                    activeDirection = RobotDirection.IDLE,
                    analogVectorX = 0f,
                    analogVectorY = 0f,
                    analogMagnitude = 0f
                )
            }
            appendLog(LogDirection.SYS, "Disconnected from robot")
        }
    }

    fun sendTestPing() {
        val stopCmd = _uiState.value.settings.commandMapping.stop
        transmitRaw(stopCmd, "PING / STOP")
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun applyProtocolPreset(preset: ProtocolPreset) {
        viewModelScope.launch {
            settingsRepo.saveCommandMapping(CommandMapping.fromPreset(preset))
            appendLog(LogDirection.SYS, "Applied protocol preset: ${preset.title}")
        }
    }

    fun updateCommandMapping(mapping: CommandMapping) {
        viewModelScope.launch {
            settingsRepo.saveCommandMapping(mapping)
        }
    }

    fun updateLineEnding(lineEnding: LineEnding) {
        viewModelScope.launch {
            settingsRepo.saveLineEnding(lineEnding)
        }
    }

    fun setEnableDiagonals(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setEnableDiagonals(enabled) }
    }

    fun setAutoStopOnRelease(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoStopOnRelease(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticEnabled(enabled) }
    }

    fun setContinuousRepeat(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setContinuousRepeat(enabled) }
    }

    private fun stopActiveMotion() {
        repeatJob?.cancel()
        _uiState.update {
            it.copy(
                activeDirection = RobotDirection.IDLE,
                analogVectorX = 0f,
                analogVectorY = 0f,
                analogMagnitude = 0f
            )
        }
        dispatchDirectionCommand(RobotDirection.IDLE)
    }

    private fun startRepeatLoopIfNeeded(direction: RobotDirection) {
        repeatJob?.cancel()
        if (direction == RobotDirection.IDLE || !_uiState.value.settings.continuousRepeat) return
        repeatJob = viewModelScope.launch {
            while (isActive) {
                delay(180)
                if (_uiState.value.activeDirection == direction) {
                    dispatchDirectionCommand(direction, logRepeat = false)
                } else {
                    break
                }
            }
        }
    }

    private fun dispatchDirectionCommand(
        direction: RobotDirection,
        forceLogLabel: String? = null,
        logRepeat: Boolean = true
    ) {
        val code = _uiState.value.settings.commandMapping.commandFor(direction)
        val label = forceLogLabel ?: direction.shortCode
        transmitRaw(code, label, appendToConsole = logRepeat)
    }

    private fun transmitRaw(
        rawCommand: String,
        label: String,
        appendToConsole: Boolean = true
    ) {
        val currentSettings = _uiState.value.settings
        val speed = _uiState.value.speedPercent
        val summary = "$label → \"$rawCommand\""
        _uiState.update { it.copy(lastPacketSummary = summary) }

        viewModelScope.launch {
            val result = transport.sendCommand(
                rawCommand = rawCommand,
                lineEnding = currentSettings.lineEnding.suffix,
                speedPercent = speed
            )
            if (appendToConsole) {
                result.fold(
                    onSuccess = { sentOverWire ->
                        val suffix = if (sentOverWire) "" else " [preview]"
                        appendLog(LogDirection.TX, "$label -> \"$rawCommand\"$suffix", sentOverWire)
                    },
                    onFailure = { error ->
                        appendLog(
                            LogDirection.ERR,
                            "TX failed ($rawCommand): ${error.message ?: "I/O error"}",
                            delivered = false
                        )
                    }
                )
            }
        }
    }

    private fun appendLog(
        direction: LogDirection,
        message: String,
        delivered: Boolean = true
    ) {
        val entry = ConsoleLogEntry(
            id = logCounter.incrementAndGet(),
            timestamp = timeFormat.format(Date()),
            direction = direction,
            message = message,
            delivered = delivered
        )
        _uiState.update { state ->
            val updated = (listOf(entry) + state.consoleLogs).take(60)
            state.copy(consoleLogs = updated)
        }
    }

    private fun mapAngleToDirection(angleDeg: Float, allowDiagonals: Boolean): RobotDirection {
        // 0 deg is Straight Up (Forward), 90 is Right, -90 is Left, +-180 is Back
        val normalized = ((angleDeg + 360f) % 360f)
        return if (allowDiagonals) {
            when {
                normalized < 22.5f || normalized >= 337.5f -> RobotDirection.FORWARD
                normalized < 67.5f -> RobotDirection.FORWARD_RIGHT
                normalized < 112.5f -> RobotDirection.RIGHT
                normalized < 157.5f -> RobotDirection.BACKWARD_RIGHT
                normalized < 202.5f -> RobotDirection.BACKWARD
                normalized < 247.5f -> RobotDirection.BACKWARD_LEFT
                normalized < 292.5f -> RobotDirection.LEFT
                else -> RobotDirection.FORWARD_LEFT
            }
        } else {
            when {
                normalized < 45f || normalized >= 315f -> RobotDirection.FORWARD
                normalized < 135f -> RobotDirection.RIGHT
                normalized < 225f -> RobotDirection.BACKWARD
                else -> RobotDirection.LEFT
            }
        }
    }

    private fun directionVectorX(direction: RobotDirection): Float = when (direction) {
        RobotDirection.LEFT -> -1f
        RobotDirection.RIGHT -> 1f
        RobotDirection.FORWARD_LEFT, RobotDirection.BACKWARD_LEFT -> -0.71f
        RobotDirection.FORWARD_RIGHT, RobotDirection.BACKWARD_RIGHT -> 0.71f
        else -> 0f
    }

    private fun directionVectorY(direction: RobotDirection): Float = when (direction) {
        RobotDirection.FORWARD -> -1f
        RobotDirection.BACKWARD -> 1f
        RobotDirection.FORWARD_LEFT, RobotDirection.FORWARD_RIGHT -> -0.71f
        RobotDirection.BACKWARD_LEFT, RobotDirection.BACKWARD_RIGHT -> 0.71f
        else -> 0f
    }

    override fun onCleared() {
        super.onCleared()
        repeatJob?.cancel()
        hornJob?.cancel()
        spinJob?.cancel()
        transport.release()
    }
}
