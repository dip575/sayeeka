package com.example.robot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.robot.model.CommandMapping
import com.example.robot.model.ControllerSettings
import com.example.robot.model.LineEnding
import com.example.robot.model.TransportType
import com.example.robot.model.WifiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.robotSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "robopilot_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CMD_FORWARD = stringPreferencesKey("cmd_forward")
        val CMD_BACKWARD = stringPreferencesKey("cmd_backward")
        val CMD_LEFT = stringPreferencesKey("cmd_left")
        val CMD_RIGHT = stringPreferencesKey("cmd_right")
        val CMD_STOP = stringPreferencesKey("cmd_stop")
        val CMD_FWD_LEFT = stringPreferencesKey("cmd_fwd_left")
        val CMD_FWD_RIGHT = stringPreferencesKey("cmd_fwd_right")
        val CMD_REV_LEFT = stringPreferencesKey("cmd_rev_left")
        val CMD_REV_RIGHT = stringPreferencesKey("cmd_rev_right")
        val CMD_HORN = stringPreferencesKey("cmd_horn")
        val CMD_LIGHTS_ON = stringPreferencesKey("cmd_lights_on")
        val CMD_LIGHTS_OFF = stringPreferencesKey("cmd_lights_off")
        val CMD_SPIN = stringPreferencesKey("cmd_spin")

        val LINE_ENDING = stringPreferencesKey("line_ending")
        val ENABLE_DIAGONALS = booleanPreferencesKey("enable_diagonals")
        val AUTO_STOP_RELEASE = booleanPreferencesKey("auto_stop_release")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val CONTINUOUS_REPEAT = booleanPreferencesKey("continuous_repeat")

        val WIFI_HOST = stringPreferencesKey("wifi_host")
        val WIFI_PORT = intPreferencesKey("wifi_port")
        val WIFI_PROTOCOL = stringPreferencesKey("wifi_protocol")
        val WIFI_HTTP_PATH = stringPreferencesKey("wifi_http_path")
    }

    val settingsFlow: Flow<ControllerSettings> = context.robotSettingsDataStore.data.map { prefs ->
        val defaults = CommandMapping()
        val mapping = CommandMapping(
            forward = prefs[Keys.CMD_FORWARD] ?: defaults.forward,
            backward = prefs[Keys.CMD_BACKWARD] ?: defaults.backward,
            left = prefs[Keys.CMD_LEFT] ?: defaults.left,
            right = prefs[Keys.CMD_RIGHT] ?: defaults.right,
            stop = prefs[Keys.CMD_STOP] ?: defaults.stop,
            forwardLeft = prefs[Keys.CMD_FWD_LEFT] ?: defaults.forwardLeft,
            forwardRight = prefs[Keys.CMD_FWD_RIGHT] ?: defaults.forwardRight,
            backwardLeft = prefs[Keys.CMD_REV_LEFT] ?: defaults.backwardLeft,
            backwardRight = prefs[Keys.CMD_REV_RIGHT] ?: defaults.backwardRight,
            horn = prefs[Keys.CMD_HORN] ?: defaults.horn,
            lightsOn = prefs[Keys.CMD_LIGHTS_ON] ?: defaults.lightsOn,
            lightsOff = prefs[Keys.CMD_LIGHTS_OFF] ?: defaults.lightsOff,
            spin360 = prefs[Keys.CMD_SPIN] ?: defaults.spin360
        )

        val lineEnding = prefs[Keys.LINE_ENDING]?.let { name ->
            LineEnding.entries.firstOrNull { it.name == name }
        } ?: LineEnding.LF

        val wifiProtocol = prefs[Keys.WIFI_PROTOCOL]?.let { name ->
            TransportType.entries.firstOrNull { it.name == name && it != TransportType.BLUETOOTH_SPP }
        } ?: TransportType.WIFI_UDP

        val defaultWifi = WifiConfig()
        ControllerSettings(
            commandMapping = mapping,
            lineEnding = lineEnding,
            enableDiagonals = prefs[Keys.ENABLE_DIAGONALS] ?: true,
            autoStopOnRelease = prefs[Keys.AUTO_STOP_RELEASE] ?: true,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            continuousRepeat = prefs[Keys.CONTINUOUS_REPEAT] ?: false,
            wifiConfig = WifiConfig(
                host = prefs[Keys.WIFI_HOST] ?: defaultWifi.host,
                port = prefs[Keys.WIFI_PORT] ?: defaultWifi.port,
                protocol = wifiProtocol,
                httpPathTemplate = prefs[Keys.WIFI_HTTP_PATH] ?: defaultWifi.httpPathTemplate
            )
        )
    }

    suspend fun saveCommandMapping(mapping: CommandMapping) {
        context.robotSettingsDataStore.edit { prefs ->
            prefs[Keys.CMD_FORWARD] = mapping.forward
            prefs[Keys.CMD_BACKWARD] = mapping.backward
            prefs[Keys.CMD_LEFT] = mapping.left
            prefs[Keys.CMD_RIGHT] = mapping.right
            prefs[Keys.CMD_STOP] = mapping.stop
            prefs[Keys.CMD_FWD_LEFT] = mapping.forwardLeft
            prefs[Keys.CMD_FWD_RIGHT] = mapping.forwardRight
            prefs[Keys.CMD_REV_LEFT] = mapping.backwardLeft
            prefs[Keys.CMD_REV_RIGHT] = mapping.backwardRight
            prefs[Keys.CMD_HORN] = mapping.horn
            prefs[Keys.CMD_LIGHTS_ON] = mapping.lightsOn
            prefs[Keys.CMD_LIGHTS_OFF] = mapping.lightsOff
            prefs[Keys.CMD_SPIN] = mapping.spin360
        }
    }

    suspend fun saveLineEnding(lineEnding: LineEnding) {
        context.robotSettingsDataStore.edit { it[Keys.LINE_ENDING] = lineEnding.name }
    }

    suspend fun setEnableDiagonals(enabled: Boolean) {
        context.robotSettingsDataStore.edit { it[Keys.ENABLE_DIAGONALS] = enabled }
    }

    suspend fun setAutoStopOnRelease(enabled: Boolean) {
        context.robotSettingsDataStore.edit { it[Keys.AUTO_STOP_RELEASE] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.robotSettingsDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setContinuousRepeat(enabled: Boolean) {
        context.robotSettingsDataStore.edit { it[Keys.CONTINUOUS_REPEAT] = enabled }
    }

    suspend fun saveWifiConfig(wifiConfig: WifiConfig) {
        context.robotSettingsDataStore.edit { prefs ->
            prefs[Keys.WIFI_HOST] = wifiConfig.host
            prefs[Keys.WIFI_PORT] = wifiConfig.port
            prefs[Keys.WIFI_PROTOCOL] = wifiConfig.protocol.name
            prefs[Keys.WIFI_HTTP_PATH] = wifiConfig.httpPathTemplate
        }
    }
}
