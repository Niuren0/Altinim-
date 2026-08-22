package com.altinim.app.widget

import android.content.Context
import android.content.Intent
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
import com.altinim.app.data.sortAndFilterGoldProducts
import kotlinx.coroutines.flow.first

class AltinimWidget : GlanceAppWidget() {

    companion object {
        private const val MAX_WIDGET_ROWS = 17
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as AltinimApplication).container

        val settings = try {
            container.settingsRepository.settings.first()
        } catch (e: Exception) {
            null
        }

        val products = container.priceStore.currentOrFetchOnce()

        val visibleProducts = if (settings != null) {
            sortAndFilterGoldProducts(products, settings.productOrder, settings.hiddenProducts)
        } else {
            products
        }.take(MAX_WIDGET_ROWS)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.Parchment)
                    .padding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            ) {
                Text(
                    text = "ALTINIM",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = fixedColor(WidgetColors.LedgerCover)
                    )
                )
                if (visibleProducts.isEmpty()) {
                    Text(
                        text = "Fiyatlar alınamadı",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = fixedColor(WidgetColors.InkFaded)
                        )
                    )
                } else {
                    visibleProducts.forEach { product ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = product.productName,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = fixedColor(WidgetColors.InkCharcoal)
                                )
                            )
                            Text(
                                text = "${product.roundSalesPrice} TL",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fixedColor(WidgetColors.WaxSeal)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}