package com.altinim.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val Parchment = Color(0xFFEFE3C0)      // ui/theme/Color.kt -> Parchment
    val LedgerCover = Color(0xFF1C2B39)    // ui/theme/Color.kt -> LedgerCover
    val InkCharcoal = Color(0xFF26211B)    // ui/theme/Color.kt -> InkCharcoal
    val InkFaded = Color(0xFF6B6152)       // ui/theme/Color.kt -> InkFaded
    val BottleInk = Color(0xFF2F4B3C)      // ui/theme/Color.kt -> BottleInk
    val WaxSeal = Color(0xFF7A2E2E)        // ui/theme/Color.kt -> WaxSeal
}

fun fixedColor(color: Color) = ColorProvider(day = color, night = color)