package com.standbyme.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.ui.components.WavySlider
import kotlin.math.roundToInt

/** Panneau de réglages affiché par-dessus l'écran (appui sur l'engrenage). */
@Composable
fun SettingsPanel(
    waveIntensity: Float,
    onWaveIntensity: (Float) -> Unit,
    auraIntensity: Float,
    onAuraIntensity: (Float) -> Unit,
    auraEnabled: Boolean,
    onAuraEnabled: (Boolean) -> Unit,
    auraUseAlbumColor: Boolean,
    onAuraUseAlbumColor: (Boolean) -> Unit,
    appleDesign: Boolean,
    onAppleDesign: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(380.dp)
                .clip(shape)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outline, shape)
                // Absorbe les clics pour ne pas fermer le panneau en touchant son contenu.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.settings),
                color = colors.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )

            // Design Apple
            Column {
                SwitchRow(stringResource(R.string.settings_apple_design), appleDesign, onAppleDesign)
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.settings_apple_design_hint),
                    color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            // Intensité des vagues (aperçu en direct sur le curseur lui-même).
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelWithPercent(stringResource(R.string.settings_wave_intensity), waveIntensity)
                WavySlider(
                    value = waveIntensity,
                    onValueChange = onWaveIntensity,
                    wavy = true,
                    amplitude = (waveIntensity.coerceIn(0f, 1f) * 3f + 1f).dp,
                    trackColor = colors.primary.copy(alpha = 0.16f),
                    activeColor = colors.primary,
                    commitOnRelease = false,
                )
            }

            SwitchRow(stringResource(R.string.settings_aura_enabled), auraEnabled, onAuraEnabled)
            SwitchRow(stringResource(R.string.settings_aura_album_color), auraUseAlbumColor, onAuraUseAlbumColor)

            // Puissance du halo, avec une pastille d'aperçu.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelWithPercent(stringResource(R.string.settings_aura_intensity), auraIntensity)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraPreview(auraIntensity, colors.primary)
                    Spacer(Modifier.width(16.dp))
                    WavySlider(
                        value = auraIntensity,
                        onValueChange = onAuraIntensity,
                        modifier = Modifier.weight(1f),
                        trackColor = colors.primary.copy(alpha = 0.16f),
                        activeColor = colors.primary,
                        commitOnRelease = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LabelWithPercent(label: String, value: Float) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = color, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("${(value * 100).roundToInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AuraPreview(intensity: Float, color: Color) {
    val t = intensity.coerceIn(0f, 1f)
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(20.dp + 30.dp * t)
                .blur(6.dp + 12.dp * t)
                .background(Brush.radialGradient(listOf(color.copy(alpha = t), color.copy(alpha = 0f))), CircleShape),
        )
        Box(Modifier.size(20.dp).clip(CircleShape).background(color))
    }
}
