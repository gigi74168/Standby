package com.standbyme.app.ui.clock

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.ui.theme.Anton
import com.standbyme.app.ui.theme.InstrumentSans
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Grande heure en Anton + date en capitales. */
@Composable
fun NumericClock(time: String, date: String, timeColor: Color, dateColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = timeColor, fontSize = 108.sp, lineHeight = 108.sp, fontFamily = Anton)
        Text(
            date.uppercase(),
            color = dateColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InstrumentSans,
            letterSpacing = 2.sp,
        )
    }
}

/** Cadran à aiguilles, dessiné entièrement au Canvas. */
@Composable
fun AnalogClock(
    hour: Int,
    minute: Int,
    second: Int,
    dialColor: Color,
    hourHandColor: Color,
    secondHandColor: Color,
) {
    Canvas(Modifier.size(220.dp)) {
        val radius = size.minDimension / 2f
        val c = center
        drawCircle(dialColor.copy(alpha = 0.16f), radius, c, style = Stroke(2f))

        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val major = i % 3 == 0
            val len = if (major) 14f else 8f
            val outer = Offset((cos(angle) * radius).toFloat() + c.x, (sin(angle) * radius).toFloat() + c.y)
            val inner = Offset((cos(angle) * (radius - len)).toFloat() + c.x, (sin(angle) * (radius - len)).toFloat() + c.y)
            drawLine(
                color = if (major) dialColor else dialColor.copy(alpha = 0.3f),
                start = inner,
                end = outer,
                strokeWidth = if (major) 4f else 2.5f,
                cap = StrokeCap.Round,
            )
        }

        drawHand(c, (hour % 12 + minute / 60.0) * 30.0, radius * 0.5f, hourHandColor, 8f)
        drawHand(c, (minute + second / 60.0) * 6.0, radius * 0.72f, dialColor, 5f)
        drawHand(c, second * 6.0, radius * 0.78f, secondHandColor, 2.5f)
        drawCircle(secondHandColor, 7f, c)
    }
}

private fun DrawScope.drawHand(center: Offset, degrees: Double, length: Float, color: Color, width: Float) {
    val angle = Math.toRadians(degrees - 90.0)
    val end = Offset((cos(angle) * length).toFloat() + center.x, (sin(angle) * length).toFloat() + center.y)
    drawLine(color, center, end, width, StrokeCap.Round)
}

/**
 * Horloge solaire : le soleil suit un arc entre le lever et le coucher.
 * [dayProgress] vaut 0 au lever, 1 au coucher.
 */
@Composable
fun SolarClock(
    time: String,
    dayProgress: Float,
    sunrise: String,
    sunset: String,
    lineColor: Color,
    sunColor: Color,
    timeColor: Color,
    labelColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(90.dp),
        ) {
            val w = size.width
            val h = size.height
            val horizon = h * 0.92f
            val top = h * 0.15f

            drawLine(
                lineColor.copy(alpha = 0.3f),
                Offset(0f, horizon),
                Offset(w, horizon),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
            )

            val arc = Path().apply {
                moveTo(w * 0.02f, horizon)
                cubicTo(w * 0.02f, top, w * 0.98f, top, w * 0.98f, horizon)
            }
            drawPath(arc, lineColor.copy(alpha = 0.18f), style = Stroke(2f))

            val theta = (1f - dayProgress.coerceIn(0f, 1f)) * PI
            val sun = Offset(
                x = w / 2f + (cos(theta) * (w / 2f - 20f)).toFloat(),
                y = horizon - (sin(theta) * (horizon - top)).toFloat(),
            )
            drawCircle(sunColor.copy(alpha = 0.45f), 20f, sun, style = Stroke(1.5f))
            drawCircle(sunColor, 11f, sun)
        }
        Text(time, color = timeColor, fontSize = 64.sp, lineHeight = 64.sp, fontFamily = Anton)
        Text(
            stringResource(R.string.sunrise_sunset, sunrise, sunset).uppercase(),
            color = labelColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InstrumentSans,
            letterSpacing = 1.5.sp,
        )
    }
}

/** Heure posée sur 26 barres qui ondulent comme un visualiseur audio. */
@Composable
fun WaveClock(time: String, timeColor: Color, barColor: Color) {
    val barsHeight = 150.dp
    val transition = rememberInfiniteTransition(label = "waveClock")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(barsHeight),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(26) { i ->
                val peak = (abs(sin(i * 1.07)).toFloat() * 0.68f + 0.3f).coerceAtMost(1f)
                val scale = transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = peak,
                    animationSpec = infiniteRepeatable(
                        tween(1600),
                        RepeatMode.Reverse,
                        StartOffset((i % 9) * 110),
                    ),
                    label = "wcScale$i",
                )
                Box(
                    Modifier
                        .width(9.dp)
                        .height(barsHeight * scale.value)
                        .background(barColor, RoundedCornerShape(8.dp)),
                )
            }
        }
        Text(time, color = timeColor, fontSize = 96.sp, lineHeight = 96.sp, fontFamily = Anton)
    }
}
