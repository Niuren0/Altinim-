package com.altinim.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Bilerek tek, sabit bir tema — sistem karanlık moduna göre otomatik
// mor/mavi Material paletine dönmüyor. "Kuyumcu defteri" kimliği her
// zaman aynı kalıyor.
@Composable
fun AltinimTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AltinimColors,
        typography = AltinimTypography,
        content = content
    )
}

private val AltinimColors = lightColorScheme(
    primary = AntiqueBrass,
    onPrimary = ParchmentLight,
    secondary = BottleInk,
    background = Parchment,
    onBackground = InkCharcoal,
    surface = ParchmentLight,
    onSurface = InkCharcoal,
    error = WaxSeal,
    outline = HairlineRule
)
