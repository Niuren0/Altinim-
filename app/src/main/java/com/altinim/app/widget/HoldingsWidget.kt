package com.altinim.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.altinim.app.AltinimApplication
import com.altinim.app.MainActivity
import com.altinim.app.data.computePortfolioSummary
import kotlinx.coroutines.flow.first

class HoldingsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as AltinimApplication).container

        val entries = try {
            container.goldEntryRepository.getAllEntries().first()
        } catch (e: Exception) {
            emptyList()
        }

        val products = container.priceStore.currentOrFetchOnce()

        val summary = computePortfolioSummary(entries, products)
        val profitColor = if (summary.profit >= 0) WidgetColors.BottleInk else WidgetColors.WaxSeal
        val sign = if (summary.profit >= 0) "+" else ""

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.Parchment)
                    .padding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            ) {
                Text(
                    text = "BİRİKİMİM",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = fixedColor(WidgetColors.LedgerCover)
                    )
                )
                if (entries.isEmpty()) {
                    Text(
                        text = "Henüz kayıt yok",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = fixedColor(WidgetColors.InkFaded)
                        )
                    )
                } else if (!summary.hasLivePrices) {
                    Text(
                        text = "Fiyat verisi alınamadı",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = fixedColor(WidgetColors.InkFaded))
                    )
                } else {
                    SummaryLine("Yatırılan", "${summary.totalInvested.toInt()} TL")
                    SummaryLine("Güncel", "${summary.currentValue.toInt()} TL")
                    SummaryLine("Kâr / Zarar", "$sign${summary.profit.toInt()} TL", profitColor)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = WidgetColors.InkCharcoal
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(fontSize = 12.sp, color = fixedColor(WidgetColors.InkFaded))
        )
        Text(
            text = value,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fixedColor(valueColor)
            )
        )
    }
}