package com.standbyme.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.media.PlaybackUiState
import com.standbyme.app.ui.components.Equalizer
import com.standbyme.app.ui.components.formatHours
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

// Heures de lever/coucher fixes, comme dans la 1.6.1.
private const val SUNRISE_HOURS = 7.2
private const val SUNSET_HOURS = 20.07

@Composable
fun ClockScreen(
    style: ClockStyle,
    now: LocalDateTime,
    playback: PlaybackUiState,
    showStylePicker: Boolean,
    onLongPressClock: () -> Unit,
    onDismissStyles: () -> Unit,
    onPickStyle: (ClockStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val time = String.format(Locale.ROOT, "%d:%02d", now.hour, now.minute)
    val secondsOfDay = now.hour * 3600 + now.minute * 60 + now.second
    val dayProgress = ((secondsOfDay / 86_400.0 * 24 - SUNRISE_HOURS) / (SUNSET_HOURS - SUNRISE_HOURS))
        .coerceIn(0.0, 1.0).toFloat()

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPressClock() }) },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (style) {
                ClockStyle.NUMERIC -> NumericClock(time, now.format(DateFormat), colors.primary, colors.onSurfaceVariant)
                ClockStyle.ANALOG -> AnalogClock(
                    hour = now.hour,
                    minute = now.minute,
                    second = now.second,
                    dialColor = colors.primary,
                    hourHandColor = colors.onBackground,
                    secondHandColor = colors.tertiary,
                )
                ClockStyle.SOLAR -> SolarClock(
                    time = time,
                    dayProgress = dayProgress,
                    sunrise = formatHours(SUNRISE_HOURS),
                    sunset = formatHours(SUNSET_HOURS),
                    lineColor = colors.primary,
                    sunColor = colors.tertiary,
                    timeColor = colors.onBackground,
                    labelColor = colors.onSurfaceVariant,
                )
                ClockStyle.WAVE -> WaveClock(time, colors.primary, colors.surfaceVariant)
            }
        }

        // Pastille « en cours de lecture » en bas à gauche.
        if (playback.isPlaying) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 30.dp, bottom = 22.dp)
                    .background(colors.surface.copy(alpha = 0.92f), RoundedCornerShape(22.dp))
                    .padding(start = 13.dp, top = 9.dp, end = 16.dp, bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Equalizer(bars = 8, height = 16.dp, color = colors.primary)
                Text(
                    playback.title,
                    color = colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    playback.artist,
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showStylePicker) {
            StylePicker(
                tileColor = colors.surfaceVariant,
                borderColor = colors.outline,
                previewColor = colors.primary,
                labelColor = colors.onBackground,
                onPick = onPickStyle,
                onDismiss = onDismissStyles,
            )
        }
    }
}
