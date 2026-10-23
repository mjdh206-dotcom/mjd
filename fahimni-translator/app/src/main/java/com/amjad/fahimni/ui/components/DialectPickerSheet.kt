package com.amjad.fahimni.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialectPickerSheet(
    title: String,
    includeAutoDetect: Boolean,
    selected: DialectProfile,
    onSelect: (DialectProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val arabicList = remember(searchQuery) {
        DialectProfile.arabicDialects.filter {
            searchQuery.isBlank() ||
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.regionHint.contains(searchQuery, ignoreCase = true)
        }
    }

    val worldList = remember(searchQuery) {
        DialectProfile.worldLanguages.filter {
            searchQuery.isBlank() ||
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.regionHint.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardDark,
        contentColor = TextWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = RoyalGold,
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن لهجة أو دولة أو لغة...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(440.dp),
            ) {
                if (includeAutoDetect && searchQuery.isBlank()) {
                    item {
                        DialectRowCard(
                            profile = DialectProfile.AUTO_DETECT,
                            isSelected = selected == DialectProfile.AUTO_DETECT,
                            highlightColor = CyanRadar,
                            onClick = {
                                onSelect(DialectProfile.AUTO_DETECT)
                                onDismiss()
                            }
                        )
                    }
                }

                if (arabicList.isNotEmpty()) {
                    item {
                        Text(
                            text = "اللهجات العربية (13 لهجة)",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldBright,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )
                    }
                    items(arabicList, key = { it.id }) { item ->
                        DialectRowCard(
                            profile = item,
                            isSelected = selected == item,
                            highlightColor = EmeraldBright,
                            onClick = {
                                onSelect(item)
                                onDismiss()
                            }
                        )
                    }
                }

                if (worldList.isNotEmpty()) {
                    item {
                        Text(
                            text = "اللغات العالمية (للتفاهم مع الأجانب بلهجتك)",
                            style = MaterialTheme.typography.labelLarge,
                            color = RoyalGold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(worldList, key = { it.id }) { item ->
                        DialectRowCard(
                            profile = item,
                            isSelected = selected == item,
                            highlightColor = RoyalGold,
                            onClick = {
                                onSelect(item)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialectRowCard(
    profile: DialectProfile,
    isSelected: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) highlightColor.copy(alpha = 0.16f) else CardElevated,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) highlightColor else BorderSubtle,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = profile.flag,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Column {
                    Text(
                        text = profile.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) highlightColor else TextWhite,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = profile.regionHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            if (isSelected) {
                BoxBadge("مفعّل", highlightColor)
            }
        }
    }
}

@Composable
private fun BoxBadge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF04131D),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
