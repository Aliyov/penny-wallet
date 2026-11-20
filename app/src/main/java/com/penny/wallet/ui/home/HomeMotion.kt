package com.penny.wallet.ui.home

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.penny.wallet.ui.theme.Penny

/** Size-change spring for `animateContentSize`, following the Animations setting. */
@Composable
internal fun sizeSpec(): FiniteAnimationSpec<IntSize> = Penny.motion.spec()
