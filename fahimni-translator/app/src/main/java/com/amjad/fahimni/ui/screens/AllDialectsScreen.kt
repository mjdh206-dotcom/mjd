package com.amjad.fahimni.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.amjad.fahimni.data.DialectComparisonItem
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

/**
 * استوديو «جملتك بكل اللهجات العربية»:
 * اكتب أو أرسل أي جملة وشاهدها واستمع إليها بـ 10 لهجات عربية مختلفة بضغطة واحدة!
 */
@Composable
fun AllDialectsScreen(
    initialInput: String,
    items: List<DialectComparisonItem>,
    speakingId: String?,
    onCompare: (String) -> Unit,
    onSpeakItem: (String, DialectProfile, String) -> Unit,
    onCopy: (String) -> Unit,
) {
    var inputText by remember(initialInput) { mutableStateOf(initialInput) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🌍 جملتك بجميع اللهجات العربية فوراً",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold,
                )
                Text(
                    text = "أدخل أي كلام بأي لهجة لترى وتسمع كيف يُقال في 10 دول عربية!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("مثال: شو بدك نعمل اليوم يا أخي؟") },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCompare(inputText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldBright,
                        contentColor = Color(0xFF04141F),
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("حوّل لكل اللهجات العربية الآن ⚡", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.dialect.id }) { item ->
                val tagId = "cmp_${item.dialect.id}"
                val isPlaying = (speakingId == tagId)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isPlaying) RoyalGold else BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.dialect.flag} ${item.dialect.nameAr}",
                                style = MaterialTheme.typography.labelLarge,
                                color = RoyalGold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSpeakItem(item.text, item.dialect, tagId) },
                            ) {
                                Icon(
                                    painter = painterResource(if (isPlaying) R.drawable.ic_stop else R.drawable.ic_volume),
                                    contentDescription = "استمع",
                                    tint = if (isPlaying) CoralAlert else EmeraldBright,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            IconButton(onClick = { onCopy(item.text) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy),
                                    contentDescription = "نسخ",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
