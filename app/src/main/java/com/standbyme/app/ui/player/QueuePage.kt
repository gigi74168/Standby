package com.standbyme.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.media.PlaybackUiState
import com.standbyme.app.media.QueueItem

/** Deuxième page verticale du lecteur : la file d'attente + la sortie audio. */
@Composable
fun QueuePage(
    playback: PlaybackUiState,
    onOpenOutputPicker: () -> Unit,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(background, RoundedCornerShape(30.dp))
            .padding(horizontal = 34.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.queue_title).uppercase(),
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
            if (playback.queue.isNotEmpty()) {
                Text(
                    stringResource(R.string.queue_count, playback.queue.size),
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
            }
        }

        if (playback.queue.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(260.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(playback.queue, key = { it.id }) { QueueRow(it) }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.queue_unavailable), color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        val label = remember(playback.isRemoteVolume, playback.packageName) {
            shortOutputLabel(context, playback.isRemoteVolume)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceVariant)
                .clickable(enabled = playback.hasSession, onClick = onOpenOutputPicker)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (playback.isRemoteVolume) Icons.Filled.Cast else Icons.Filled.SpeakerGroup,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = colors.tertiary,
            )
            Text(label, color = colors.tertiary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QueueRow(item: QueueItem) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (item.isCurrent) colors.primary.copy(alpha = 0.14f) else colors.surfaceVariant)
            .border(1.dp, if (item.isCurrent) colors.primary.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.3f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            item.title,
            color = if (item.isCurrent) colors.primary else colors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(item.subtitle, color = colors.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
