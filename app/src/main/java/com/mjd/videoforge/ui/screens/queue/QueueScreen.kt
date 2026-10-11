package com.mjd.videoforge.ui.screens.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(onBack: () -> Unit) {
    val items = remember {
        listOf(
            QueueItem("أغرب مخلوقات البحر", QueueItemStatus.PROCESSING, 65, "توليد الصور - مشهد ٣ من ٦"),
            QueueItem("تاريخ الحضارات القديمة", QueueItemStatus.WAITING, 0f, "في الانتظار"),
            QueueItem("نصائح للنوم الصحي", QueueItemStatus.WAITING, 0f, "في الانتظار"),
            QueueItem("حقائق عن الفضاء", QueueItemStatus.COMPLETED, 100f, "مكتمل ✓"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("قائمة الانتظار", fontWeight = FontWeight.Bold)
                        Text(
                            "${items.count { it.status == QueueItemStatus.WAITING || it.status == QueueItemStatus.PROCESSING }} نشط",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Pause all */ }) {
                        Icon(Icons.Default.PauseCircle, "إيقاف مؤقت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                QueueCard(item = item)
            }
        }
    }
}

@Composable
fun QueueCard(item: QueueItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(item.statusLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = when (item.status) {
                        QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        QueueItemStatus.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer
                        QueueItemStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        when (item.status) {
                            QueueItemStatus.WAITING -> "انتظار"
                            QueueItemStatus.PROCESSING -> "جاري"
                            QueueItemStatus.COMPLETED -> "مكتمل"
                            QueueItemStatus.FAILED -> "فشل"
                            QueueItemStatus.CANCELLED -> "ملغي"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (item.status == QueueItemStatus.PROCESSING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.progress.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class QueueItem(
    val title: String,
    val status: QueueItemStatus,
    val progress: Float,
    val statusLabel: String
)

enum class QueueItemStatus { WAITING, PROCESSING, COMPLETED, FAILED, CANCELLED }
