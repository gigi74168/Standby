package com.standbyme.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Calcule des marges horizontales sûres vis-à-vis du poinçon caméra / de l'encoche
 * (`WindowInsets.displayCutout`), en tenant compte de `LocalLayoutDirection`.
 *
 * - Sur le bord de départ (`start`) : si une encoche est présente (`cutoutStart > 0.dp`),
 *   retourne `maxOf(baseStart + maxOf(10.dp, extraStartWhenNoCutout), cutoutStart + extraCutoutClearance)`.
 *   Sans encoche détectée (`cutoutStart == 0.dp`), retourne `baseStart + extraStartWhenNoCutout`.
 * - Sur le bord de fin (`end`) : si une encoche est présente (`cutoutEnd > 0.dp`, ex. paysage inversé),
 *   retourne `maxOf(baseEnd, cutoutEnd + extraCutoutClearance)`, sinon conserve `baseEnd`.
 */
@Composable
fun rememberCutoutSafeHorizontalPadding(
    baseStart: Dp,
    baseEnd: Dp = baseStart,
    extraCutoutClearance: Dp = 14.dp,
    extraStartWhenNoCutout: Dp = 0.dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutStart = cutoutPadding.calculateStartPadding(layoutDirection)
    val cutoutEnd = cutoutPadding.calculateEndPadding(layoutDirection)

    val safeStart = if (cutoutStart > 0.dp) {
        maxOf(baseStart + maxOf(10.dp, extraStartWhenNoCutout), cutoutStart + extraCutoutClearance)
    } else {
        baseStart + extraStartWhenNoCutout
    }

    val safeEnd = if (cutoutEnd > 0.dp) {
        maxOf(baseEnd, cutoutEnd + extraCutoutClearance)
    } else {
        baseEnd
    }

    return remember(safeStart, top, safeEnd, bottom) {
        PaddingValues(start = safeStart, top = top, end = safeEnd, bottom = bottom)
    }
}
