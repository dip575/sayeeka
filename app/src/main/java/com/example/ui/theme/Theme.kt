package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RoboPilotColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = SignalAmber,
    onSecondary = OnSignalAmber,
    secondaryContainer = SignalAmberContainer,
    onSecondaryContainer = SignalAmber,
    tertiary = EmeraldMint,
    onTertiary = OnElectricCyan,
    tertiaryContainer = EmeraldMintContainer,
    onTertiaryContainer = EmeraldMint,
    error = EmergencyCoral,
    onError = OnEmergencyCoral,
    errorContainer = EmergencyCoralContainer,
    onErrorContainer = EmergencyCoral,
    background = CockpitBackground,
    onBackground = TextPrimary,
    surface = CockpitSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = CockpitBackground,
    surfaceContainerLow = CockpitSurface,
    surfaceContainer = CockpitSurfaceElevated,
    surfaceContainerHigh = CockpitSurfaceHighest,
    surfaceContainerHighest = CockpitSurfaceHighest,
    outline = CockpitOutline,
    outlineVariant = CockpitOutlineSubtle
)

private val RoboPilotShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RoboPilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoboPilotColorScheme,
        typography = Typography,
        shapes = RoboPilotShapes,
        content = content
    )
}
