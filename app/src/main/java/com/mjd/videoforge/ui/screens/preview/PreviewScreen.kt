package com.mjd.videoforge.ui.screens.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * شاشة المعاينة والتصدير
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    projectId: String,
    onBack: () -> Unit,
    onNewVideo: () -> Unit
) {
    var title by remember { mutableStateOf("أغرب ٥ حيوانات في أعماق المحيط") }
    var description by remember { mutableStateOf("في أعماق المحيط حيث لا يصل الضوء، تعيش كائنات غريبة تبدو كأنها من كوكب آخر...") }
    var hashtags by remember {
        mutableStateOf("#محيط #حقائق #علوم #مخلوقات_بحرية #أعماق #طبيعة #معلومات #تعلم")
    }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المعاينة والتصدير", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video preview placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { /* Play video */ },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayCircleFilled,
                                "معاينة الفيديو",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "معاينة الفيديو",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان الفيديو") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Title, null) }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                label = { Text("وصف الفيديو") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            // Hashtags
            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("هاشتاغات") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Tag, null) }
            )

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { /* Regenerate titles */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("توليد عناوين بديلة (A/B)")
                }

                // Export / Share buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Save to device */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("حفظ")
                    }

                    Button(
                        onClick = { /* Share */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("مشاركة")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* YouTube */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text("يوتيوب")
                    }

                    OutlinedButton(
                        onClick = { /* TikTok */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, null)
                        Spacer(Modifier.width(4.dp))
                        Text("تيك توك")
                    }

                    OutlinedButton(
                        onClick = { /* Instagram */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(4.dp))
                        Text("إنستا")
                    }
                }

                // New video button
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onNewVideo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("فيديو جديد")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
