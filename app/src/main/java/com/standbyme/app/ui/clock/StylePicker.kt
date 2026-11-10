package com.standbyme.app.ui.clock

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.ui.theme.Anton

/** Superposition affichée après un appui long sur l'horloge. */
@Composable
fun StylePicker(
    tileColor: Color,
    borderColor: Color,
    previewColor: Color,
    labelColor: Color,
    onPick: (ClockStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StyleTile(stringResource(R.string.style_numeric), tileColor, borderColor, labelColor, { onPick(ClockStyle.NUMERIC) }) {
                Text("9:41", color = previewColor, fontSize = 34.sp, fontFamily = Anton)
            }
            StyleTile(stringResource(R.string.style_analog), tileColor, borderColor, labelColor, { onPick(ClockStyle.ANALOG) }) {
                PreviewIcon(Icons.Filled.Schedule, previewColor)
            }
            StyleTile(stringResource(R.string.style_solar), tileColor, borderColor, labelColor, { onPick(ClockStyle.SOLAR) }) {
                PreviewIcon(Icons.Filled.WbSunny, previewColor)
            }
            StyleTile(stringResource(R.string.style_wave), tileColor, borderColor, labelColor, { onPick(ClockStyle.WAVE) }) {
                PreviewIcon(Icons.Filled.GraphicEq, previewColor)
            }
        }
    }
}

@Composable
private fun PreviewIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
}

@Composable
private fun StyleTile(
    label: String,
    tileColor: Color,
    borderColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 150.dp, height = 100.dp)
                .clip(shape)
                .background(tileColor)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { preview() }
        Spacer(Modifier.height(8.dp))
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
