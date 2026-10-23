package com.amjad.fahimni.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amjad.fahimni.R
import com.amjad.fahimni.data.BuiltInSamples
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

/**
 * دفتر العبارات الصوتية السريعة للسفر والمواقف اليومية:
 * ضغطة واحدة تترجم العبارة إلى لهجة أو لغة الشخص المقابل وتنطقها بالصوت فوراً!
 */
@Composable
fun PhrasebookScreen(
    targetShortLabel: String,
    onSpeakPhraseToPartner: (String) -> Unit,
) {
    val categories = BuiltInSamples.phraseCategories
    var selectedCatId by remember { mutableStateOf(categories.first().id) }
    val activeCategory = categories.firstOrNull { it.id == selectedCatId } ?: categories.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📚 عبارات سريعة جاهزة للنطق الفوري",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold,
                )
                Text(
                    text = "اضغط على أي عبارة ليحولها التطبيق فوراً إلى ($targetShortLabel) وينطقها بالصوت للشخص الذي أمامك!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { cat ->
                val isSelected = (cat.id == selectedCatId)
                Surface(
                    onClick = { selectedCatId = cat.id },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) EmeraldBright.copy(alpha = 0.2f) else CardElevated,
                    border = BorderStroke(1.dp, if (isSelected) EmeraldBright else BorderSubtle),
                ) {
                    Text(
                        text = "${cat.emoji} ${cat.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) EmeraldBright else TextWhite,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(activeCategory.phrases) { phrase ->
                Surface(
                    onClick = { onSpeakPhraseToPartner(phrase) },
                    shape = RoundedCornerShape(18.dp),
                    color = CardElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0A2A22),
                            border = BorderStroke(1.dp, EmeraldBright),
                            modifier = Modifier.padding(start = 10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_volume),
                                    contentDescription = null,
                                    tint = CyanRadar,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "ترجم وانطق",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldBright,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
