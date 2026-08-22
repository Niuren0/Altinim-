package com.altinim.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.altinim.app.AltinimApplication
import com.altinim.app.data.sortAndFilterGoldProducts
import kotlinx.coroutines.flow.first

class AltinimWidget : GlanceAppWidget() {

    companion object {
        // altinim_widget_info.xml'deki varsayılan widget boyutunda (4x3) sığan
        // maksimum satır sayısı. Widget boyutu XML'de değişirse burası da
        // gözden geçirilmeli.
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
            WidgetScaffold(context = context, title = "ALTINIM") {
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