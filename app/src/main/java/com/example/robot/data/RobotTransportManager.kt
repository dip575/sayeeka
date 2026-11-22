package com.example.robot.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.robot.model.ConnectionState
import com.example.robot.model.PairedBluetoothDevice
import com.example.robot.model.TransportType
import com.example.robot.model.WifiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class RobotTransportManager(private val context: Context) {

    companion object {
        // Standard Serial Port Profile (SPP) UUID used by HC-05, HC-06, ESP32 Classic BT robots
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private var btSocket: BluetoothSocket? = null
    private var tcpSocket: Socket? = null
    private var udpSocket: DatagramSocket? = null
    private var activeOutputStream: OutputStream? = null
    private var activeWifiConfig: WifiConfig? = null
    private var readerJob: Job? = null

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<PairedBluetoothDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!hasBluetoothPermission()) return emptyList()
        return try {
            adapter.bondedDevices
                ?.map { device ->
                    PairedBluetoothDevice(
                        name = device.name?.takeIf { it.isNotBlank() } ?: "Unnamed Robot",
                        address = device.address
                    )
                }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectBluetooth(device: PairedBluetoothDevice): Result<Unit> =
        withContext(Dispatchers.IO) {
            disconnectInternal()
            val adapter = bluetoothAdapter
                ?: return@withContext fail("Bluetooth hardware is not available on this device.")
            if (!hasBluetoothPermission()) {
                return@withContext fail("Bluetooth Connect permission is required.")
            }
            if (!adapter.isEnabled) {
                return@withContext fail("Bluetooth is turned off. Enable Bluetooth to pair.")
            }

            _connectionState.value =
                ConnectionState.Connecting(device.name, TransportType.BLUETOOTH_SPP)

            try {
                try {
                    adapter.cancelDiscovery()
                } catch (_: SecurityException) {
                    // Safe to ignore if SCAN permission isn't granted
                }
                val remoteDevice = adapter.getRemoteDevice(device.address)
                val socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                btSocket = socket
                activeOutputStream = socket.outputStream
                _connectionState.value = ConnectionState.Connected(
                    targetName = device.name,
                    address = device.address,
                    transport = TransportType.BLUETOOTH_SPP
                )
                startStreamReader(socket.inputStream)
                Result.success(Unit)
            } catch (e: Exception) {
                disconnectInternal()
                val reason = e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not open SPP serial channel to ${device.name}."
                _connectionState.value = ConnectionState.Error(reason)
                Result.failure(e)
            }
        }

    suspend fun connectWifi(config: WifiConfig): Result<Unit> = withContext(Dispatchers.IO) {
        disconnectInternal()
        val host = config.host.trim()
        if (host.isEmpty()) {
            return@withContext fail("Enter a valid robot IP address or hostname.")
        }
        if (config.port !in 1..65535) {
            return@withContext fail("Port must be between 1 and 65535.")
        }

        val targetLabel = "$host:${config.port}"
        _connectionState.value = ConnectionState.Connecting(targetLabel, config.protocol)

        try {
            when (config.protocol) {
                TransportType.WIFI_UDP -> {
                    // Verify host resolves and allocate local UDP socket
                    InetAddress.getByName(host)
                    udpSocket = DatagramSocket().apply { soTimeout = 1500 }
                    activeWifiConfig = config
                    _connectionState.value = ConnectionState.Connected(
                        targetName = "Wi-Fi UDP Robot",
                        address = targetLabel,
                        transport = TransportType.WIFI_UDP
                    )
                }

                TransportType.WIFI_TCP -> {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, config.port), 3000)
                    tcpSocket = socket
                    activeOutputStream = socket.getOutputStream()
                    activeWifiConfig = config
                    _connectionState.value = ConnectionState.Connected(
                        targetName = "Wi-Fi TCP Robot",
                        address = targetLabel,
                        transport = TransportType.WIFI_TCP
                    )
                    startStreamReader(socket.getInputStream())
                }

                TransportType.WIFI_HTTP -> {
                    // Validate host resolution before marking ready
                    InetAddress.getByName(host)
                    activeWifiConfig = config
                    _connectionState.value = ConnectionState.Connected(
                        targetName = "Wi-Fi HTTP Robot",
                        address = targetLabel,
                        transport = TransportType.WIFI_HTTP
                    )
                }

                TransportType.BLUETOOTH_SPP -> {
                    return@withContext fail("Select a Wi-Fi protocol for network connections.")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            disconnectInternal()
            val reason = e.message?.takeIf { it.isNotBlank() }
                ?: "Unable to reach $targetLabel."
            _connectionState.value = ConnectionState.Error(reason)
            Result.failure(e)
        }
    }

    suspend fun sendCommand(
        rawCommand: String,
        lineEnding: String,
        speedPercent: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) {
            // Not connected to hardware; caller logs as local preview without faking hardware TX
            return@withContext Result.success(false)
        }

        val payload = rawCommand + lineEnding
        val bytes = payload.toByteArray(Charsets.UTF_8)

        try {
            when (state.transport) {
                TransportType.BLUETOOTH_SPP, TransportType.WIFI_TCP -> {
                    val out = activeOutputStream
                        ?: return@withContext Result.failure(IllegalStateException("Stream closed"))
                    out.write(bytes)
                    out.flush()
                    Result.success(true)
                }

                TransportType.WIFI_UDP -> {
                    val cfg = activeWifiConfig
                        ?: return@withContext Result.failure(IllegalStateException("UDP not configured"))
                    val socket = udpSocket
                        ?: return@withContext Result.failure(IllegalStateException("UDP socket closed"))
                    val addr = InetAddress.getByName(cfg.host.trim())
                    val packet = DatagramPacket(bytes, bytes.size, addr, cfg.port)
                    socket.send(packet)
                    Result.success(true)
                }

                TransportType.WIFI_HTTP -> {
                    val cfg = activeWifiConfig
                        ?: return@withContext Result.failure(IllegalStateException("HTTP not configured"))
                    val encodedCmd = URLEncoder.encode(rawCommand, "UTF-8")
                    val path = cfg.httpPathTemplate
                        .replace("{cmd}", encodedCmd)
                        .replace("{speed}", speedPercent.toString())
                        .let { if (it.startsWith("/")) it else "/$it" }
                    val url = "http://${cfg.host.trim()}:${cfg.port}$path"
                    val request = Request.Builder().url(url).get().build()
                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string()?.trim().orEmpty()
                        if (body.isNotEmpty()) {
                            _incomingMessages.tryEmit("HTTP ${response.code}: ${body.take(80)}")
                        }
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(
                                IllegalStateException("HTTP ${response.code}")
                            )
                        }
                    }
                    Result.success(true)
                }
            }
        } catch (e: Exception) {
            if (state.transport == TransportType.BLUETOOTH_SPP || state.transport == TransportType.WIFI_TCP) {
                disconnectInternal()
                _connectionState.value = ConnectionState.Error(
                    e.message ?: "Link dropped while transmitting."
                )
            }
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun startStreamReader(inputStream: java.io.InputStream) {
        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        _incomingMessages.tryEmit(line.trim())
                    }
                }
            } catch (_: Exception) {
                // Socket closed or disconnected
            }
        }
    }

    private fun disconnectInternal() {
        readerJob?.cancel()
        readerJob = null
        try {
            activeOutputStream?.close()
        } catch (_: Exception) {
        }
        activeOutputStream = null
        try {
            btSocket?.close()
        } catch (_: Exception) {
        }
        btSocket = null
        try {
            tcpSocket?.close()
        } catch (_: Exception) {
        }
        tcpSocket = null
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
        activeWifiConfig = null
    }

    private fun fail(message: String): Result<Unit> {
        _connectionState.value = ConnectionState.Error(message)
        return Result.failure(IllegalStateException(message))
    }

    fun release() {
        disconnectInternal()
    }
}
