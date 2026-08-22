package com.altinim.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import com.altinim.app.ui.theme.BottleInk as ThemeBottleInk
import com.altinim.app.ui.theme.InkCharcoal as ThemeInkCharcoal
import com.altinim.app.ui.theme.InkFaded as ThemeInkFaded
import com.altinim.app.ui.theme.LedgerCover as ThemeLedgerCover
import com.altinim.app.ui.theme.Parchment as ThemeParchment
import com.altinim.app.ui.theme.WaxSeal as ThemeWaxSeal

object WidgetColors {
    val Parchment: Color = ThemeParchment
    val LedgerCover: Color = ThemeLedgerCover
    val InkCharcoal: Color = ThemeInkCharcoal
    val InkFaded: Color = ThemeInkFaded
    val BottleInk: Color = ThemeBottleInk
    val WaxSeal: Color = ThemeWaxSeal
}

fun fixedColor(color: Color) = ColorProvider(day = color, night = color)