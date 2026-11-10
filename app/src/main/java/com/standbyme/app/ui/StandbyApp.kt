package com.standbyme.app.ui

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.standbyme.app.R
import com.standbyme.app.ui.clock.ClockScreen
import com.standbyme.app.ui.clock.ClockStyle
import com.standbyme.app.ui.player.PlayerPage
import com.standbyme.app.ui.settings.SettingsPanel
import com.standbyme.app.ui.theme.NightFilterRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale

private const val PAGE_WIDGETS = 0
private const val PAGE_CLOCK = 1

@Composable
fun StandbyApp(viewModel: StandbyViewModel = viewModel()) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val waveIntensity by viewModel.waveIntensity.collectAsStateWithLifecycle()
    val auraIntensity by viewModel.auraIntensity.collectAsStateWithLifecycle()
    val auraEnabled by viewModel.auraEnabled.collectAsStateWithLifecycle()
    val auraUseAlbumColor by viewModel.auraUseAlbumColor.collectAsStateWithLifecycle()
    val appleDesign by viewModel.appleDesign.collectAsStateWithLifecycle()

    // Horloge : un tic par seconde, plus l'horloge monotone pour extrapoler la position de lecture.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var nowRealtime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = LocalDateTime.now()
            nowRealtime = SystemClock.elapsedRealtime()
        }
    }

    var clockStyle by rememberSaveable { mutableStateOf(ClockStyle.NUMERIC) }
    var showStylePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var nightMode by rememberSaveable { mutableStateOf(false) }
    var permissionDismissed by rememberSaveable { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(viewModel.hasNotificationAccess()) }

    // Revérifie l'autorisation au retour des réglages système.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasAccess = viewModel.hasNotificationAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .drawWithContent {
                drawContent()
                if (nightMode) {
                    // Filtre rouge façon « vision nocturne » : multiplication puis voile léger.
                    drawRect(NightFilterRed, blendMode = BlendMode.Multiply)
                    drawRect(NightFilterRed.copy(alpha = 0.35f))
                }
            },
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val pad = if (appleDesign && page == PAGE_WIDGETS) 0.dp else 24.dp
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
            ) {
                if (page == PAGE_WIDGETS) {
                    PlayerPage(
                        playback = playback,
                        positionMs = playback.positionAt(nowRealtime),
                        waveIntensity = waveIntensity,
                        auraIntensity = auraIntensity,
                        auraEnabled = auraEnabled,
                        auraUseAlbumColor = auraUseAlbumColor,
                        appleDesign = appleDesign,
                        onTogglePlay = viewModel::togglePlayPause,
                        onNext = viewModel::skipNext,
                        onPrevious = viewModel::skipPrevious,
                        onSeek = viewModel::seekTo,
                        onSetVolume = viewModel::setVolume,
                        onOpenOutputPicker = viewModel::openOutputPicker,
                    )
                } else {
                    ClockScreen(
                        style = clockStyle,
                        now = now,
                        playback = playback,
                        showStylePicker = showStylePicker,
                        onLongPressClock = { showStylePicker = true },
                        onDismissStyles = { showStylePicker = false },
                        onPickStyle = {
                            clockStyle = it
                            showStylePicker = false
                        },
                    )
                }
            }
        }

        // Flèches de navigation entre les deux pages.
        EdgeArrow(
            visible = pager.currentPage != PAGE_WIDGETS,
            icon = Icons.Filled.KeyboardArrowLeft,
            label = stringResource(R.string.tab_widgets),
            alignment = Alignment.CenterStart,
            onClick = { scope.launch { pager.animateScrollToPage(PAGE_WIDGETS) } },
        )
        EdgeArrow(
            visible = pager.currentPage != PAGE_CLOCK,
            icon = Icons.Filled.KeyboardArrowRight,
            label = stringResource(R.string.tab_clock),
            alignment = Alignment.CenterEnd,
            onClick = { scope.launch { pager.animateScrollToPage(PAGE_CLOCK) } },
        )

        // Petite heure en haut à droite quand l'horloge n'est pas affichée en grand.
        if (pager.currentPage != PAGE_CLOCK) {
            Text(
                String.format(Locale.ROOT, "%d:%02d", now.hour, now.minute),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 30.dp),
                color = colors.onBackground.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        PageDots(
            current = pager.currentPage,
            color = colors.onBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )

        // Réglages (haut gauche) et mode nuit (bas droite).
        CornerButton(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.settings),
            active = showSettings,
            alignment = Alignment.TopStart,
            onClick = { showSettings = !showSettings },
        )
        CornerButton(
            icon = if (nightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            label = stringResource(R.string.night_mode),
            active = nightMode,
            alignment = Alignment.BottomEnd,
            onClick = { nightMode = !nightMode },
        )

        if (!hasAccess && !permissionDismissed) {
            PermissionCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp),
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                onLater = { permissionDismissed = true },
            )
        }

        if (showSettings) {
            SettingsPanel(
                waveIntensity = waveIntensity,
                onWaveIntensity = viewModel::setWaveIntensity,
                auraIntensity = auraIntensity,
                onAuraIntensity = viewModel::setAuraIntensity,
                auraEnabled = auraEnabled,
                onAuraEnabled = viewModel::setAuraEnabled,
                auraUseAlbumColor = auraUseAlbumColor,
                onAuraUseAlbumColor = viewModel::setAuraUseAlbumColor,
                appleDesign = appleDesign,
                onAppleDesign = viewModel::setAppleDesign,
                onDismiss = { showSettings = false },
            )
        }
    }
}

@Composable
private fun BoxScope.EdgeArrow(
    visible: Boolean,
    icon: ImageVector,
    label: String,
    alignment: Alignment,
    onClick: () -> Unit,
) {
    val start = alignment == Alignment.CenterStart
    Box(
        Modifier
            .align(alignment)
            .padding(start = if (start) 2.dp else 0.dp, end = if (start) 0.dp else 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = visible, onClick = onClick)
            .size(width = 28.dp, height = 54.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun BoxScope.CornerButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    alignment: Alignment,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .align(alignment)
            .padding(18.dp)
            .clip(CircleShape)
            .background(if (active) colors.primary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = colors.onBackground.copy(alpha = 0.5f))
    }
}

@Composable
private fun PageDots(current: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { i ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (i == current) 0.9f else 0.25f)),
            )
        }
    }
}

@Composable
private fun PermissionCard(modifier: Modifier, onOpenSettings: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth(0.62f)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.perm_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.perm_body), color = colors.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLater) { Text(stringResource(R.string.perm_later)) }
            Button(onClick = onOpenSettings) { Text(stringResource(R.string.perm_action)) }
        }
    }
}
