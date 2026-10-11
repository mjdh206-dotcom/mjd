package com.mjd.videoforge.ui.screens.brand

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandKitScreen(onBack: () -> Unit) {
    var channelName by remember { mutableStateOf("") }
    var primaryColor by remember { mutableStateOf("#1B6B93") }
    var secondaryColor by remember { mutableStateOf("#FC9D4A") }
    var watermarkEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("هوية القناة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Channel name
            OutlinedTextField(
                value = channelName,
                onValueChange = { channelName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم القناة") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Badge, null) }
            )

            // Colors
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الألوان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("لون أساسي", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFF1B6B93),
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {}
                            OutlinedTextField(
                                value = primaryColor,
                                onValueChange = { primaryColor = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("لون ثانوي", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFFFC9D4A),
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {}
                            OutlinedTextField(
                                value = secondaryColor,
                                onValueChange = { secondaryColor = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Logo
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الشعار والعلامة المائية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* Pick logo */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text("اختيار شعار")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Checkbox(checked = watermarkEnabled, onCheckedChange = { watermarkEnabled = it })
                        Text("تفعيل العلامة المائية", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }

            // Default voice
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الصوت الثابت", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "حدد صوتاً ثابتاً يُستخدم في كل الفيديوهات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* Pick voice */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, null)
                        Spacer(Modifier.width(8.dp))
                        Text("اختيار صوت")
                    }
                }
            }

            Button(
                onClick = { /* Save brand kit */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("حفظ الهوية")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
