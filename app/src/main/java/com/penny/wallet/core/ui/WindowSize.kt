package com.penny.wallet.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

val ReadableMaxWidth = 680.dp
val TwoPaneMaxWidth = 1200.dp

fun widthClassOf(widthDp: Int): WidthClass = when {
    widthDp < 600 -> WidthClass.COMPACT
    widthDp < 840 -> WidthClass.MEDIUM
    else -> WidthClass.EXPANDED
}

/** Configuration changes on rotation, unfolding and multi-window resizes, so this follows all of them. */
@Composable
fun rememberWidthClass(): WidthClass = widthClassOf(LocalConfiguration.current.screenWidthDp)

val LocalWidthClass = staticCompositionLocalOf { WidthClass.COMPACT }

/** Space a scrolling tab screen leaves at the bottom so its last item clears the navigation. */
val LocalBottomBarPadding = compositionLocalOf { 120.dp }

@Composable
fun Modifier.readableWidth(max: Dp = ReadableMaxWidth): Modifier =
    if (LocalWidthClass.current == WidthClass.COMPACT) this
    else fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)
