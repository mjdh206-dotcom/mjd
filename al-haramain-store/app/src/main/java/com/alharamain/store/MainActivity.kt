package com.alharamain.store

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.ui.text.input.PasswordVisualTransformation

private val Pine = Color(0xFF173D31)
private val PineLight = Color(0xFF275745)
private val Cream = Color(0xFFFAF7F0)
private val Paper = Color.White
private val Ink = Color(0xFF252A25)
private val Muted = Color(0xFF777D74)
private val Orange = Color(0xFFD77A35)
private val PaleOrange = Color(0xFFFFE9D5)
private val Line = Color(0xFFE9E5DB)
private val ArabicFont = FontFamily(Font(R.font.tajawal_regular), Font(R.font.tajawal_medium, FontWeight.Medium), Font(R.font.tajawal_bold, FontWeight.Bold), Font(R.font.tajawal_extrabold, FontWeight.ExtraBold))
internal const val ORDER_CHANNEL = "haramain_orders"

private data class Product(
    val id: String, val name: String, val category: String, val price: Double, val unit: String,
    val emoji: String, val description: String, val featured: Boolean = false,
    val available: Boolean = true, val discount: Int = 0, val image: String = ""
) {
    val salePrice get() = price * (100 - discount.coerceIn(0, 90)) / 100.0
}
private data class OrderLine(val productId: String, val name: String, val qty: Int, val price: Double)
private data class StoreOrder(
    val id: String, val customer: String, val phone: String, val address: String,
    val lines: List<OrderLine>, val total: Double, val status: String, val created: String
)

private fun seedProducts() = listOf(
    Product("p1", "فستق حلبي فاخر", "مكسرات", 850.0, "١٠٠ غرام", "🥜", "فستق محمّص باعتدال، طعم غني وجودة ممتازة", true),
    Product("p2", "كاجو محمّص", "مكسرات", 700.0, "١٠٠ غرام", "🌰", "كاجو ذهبي محمّص طازجاً", true),
    Product("p3", "قهوة عربية بالهيل", "قهوة", 1200.0, "٢٥٠ غرام", "☕", "خلطة البيت، محمّصة يومياً", true),
    Product("p4", "خلطة ضيافة الحرمين", "خلطات", 950.0, "٢٠٠ غرام", "🎁", "تشكيلة مكسرات فاخرة للضيافة", discount = 15),
    Product("p5", "لوز أمريكي", "مكسرات", 600.0, "١٠٠ غرام", "🥜", "طازج ومنتقى بعناية"),
    Product("p6", "بندق محمّص", "مكسرات", 750.0, "١٠٠ غرام", "🌰", "طازج ومنتقى بعناية"),
    Product("p7", "كاندي مشكّل", "كاندي", 400.0, "١٥٠ غرام", "🍬", "ألوان ونكهات يحبّها الكبار والصغار"),
    Product("p8", "مقرمشات مشكلة", "مقرمشات", 350.0, "١٥٠ غرام", "🥨", "طازجة ومقرمشة"),
    Product("p9", "قهوة تركية", "قهوة", 1000.0, "٢٥٠ غرام", "🫘", "طازجة ومطحونة حسب الطلب")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(250, 247, 240)
        window.navigationBarColor = android.graphics.Color.rgb(250, 247, 240)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        createOrderChannel(this, getSharedPreferences("haramain_store", Context.MODE_PRIVATE).getBoolean("sound_enabled", true))
        FirebaseMessaging.getInstance().subscribeToTopic("store_orders")
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Pine, secondary = Orange, background = Cream, surface = Paper)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = ArabicFont)) {
                    StoreApp(getSharedPreferences("haramain_store", Context.MODE_PRIVATE))
                }
            }
        }
    }
}

@Composable
private fun StoreApp(prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    val orderNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showTestNotification(context)
    }
    var products by remember { mutableStateOf(loadProducts(prefs)) }
    var orders by remember { mutableStateOf(emptyList<StoreOrder>()) }
    var signedInUid by remember { mutableStateOf(CloudSync.currentUid()) }
    var seedUploadStarted by remember { mutableStateOf(false) }
    DisposableEffect(signedInUid) {
        val registrations = mutableListOf<ListenerRegistration>()
        if (signedInUid != null) {
            registrations += CloudSync.watchProducts { rows ->
                if (rows.isNotEmpty()) products = rows.mapNotNull(::productFromCloud)
                else if (!seedUploadStarted) {
                    seedUploadStarted = true
                    seedProducts().forEach { CloudSync.saveProduct(it.toCloudMap()) {} }
                }
            }
            registrations += CloudSync.watchOrders { rows -> orders = rows.mapNotNull(::orderFromCloud) }
        }
        onDispose { registrations.forEach { it.remove() } }
    }
    var tab by remember { mutableStateOf("الرئيسية") }
    var editor by remember { mutableStateOf<Product?>(null) }
    var addMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun saveProducts() { prefs.edit().putString("products", productsJson(products)).apply() }
    fun saveOrders() { prefs.edit().putString("orders", ordersJson(orders)).apply() }

    Scaffold(containerColor = Cream, bottomBar = {
        NavigationBar(containerColor = Paper) {
            listOf("الرئيسية" to "⌂", "الطلبات" to "▤", "المنتجات" to "◈", "الإعدادات" to "⚙").forEach { (label, glyph) ->
                NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Text(glyph, fontSize = 20.sp) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Pine, selectedTextColor = Pine, indicatorColor = Color(0xFFE9F0E8)))
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "الطلبات" -> OrdersPage(orders, onStatus = { order, state ->
                    CloudSync.updateOrderStatus(order.id, state) { ok -> message = if (ok) "تم تحديث حالة الطلب" else "تعذّر تحديث الطلب؛ تحقق من صلاحية المدير والاتصال" }
                })
                "المنتجات" -> ProductsPage(products, onAdd = { addMode = true }, onEdit = { editor = it }, onAvailability = { p ->
                    val updated = p.copy(available = !p.available)
                    products = products.map { if (it.id == p.id) updated else it }; saveProducts()
                    CloudSync.saveProduct(updated.toCloudMap()) { ok -> if (!ok) message = "تعذّر مزامنة المنتج؛ سجّل دخول المدير" }
                })
                "الإعدادات" -> SettingsPage(
                    prefs, loggedIn = signedInUid != null,
                    onLogin = { email, password -> CloudSync.signIn(email, password) { uid ->
                        signedInUid = uid
                        if (uid == null) message = "فشل الدخول. تأكد من بريد المدير وكلمة المرور وإعداد Firebase."
                        else { seedUploadStarted = false; FirebaseMessaging.getInstance().subscribeToTopic("store_orders"); message = "تم تسجيل الدخول والتحقق من صلاحية المدير" }
                    } },
                    onSignOut = { CloudSync.signOut(); signedInUid = null; orders = emptyList(); message = "تم تسجيل الخروج" },
                    onPublishOffer = { offer -> CloudSync.publishOffer(offer) { ok -> message = if (ok) "تم نشر العرض للزبائن" else "تعذّر النشر؛ تأكد من تسجيل دخول المدير" } },
                    onTest = { message = it }
                )
                else -> DashboardPage(products, orders, cloudConnected = signedInUid != null, onOrders = { tab = "الطلبات" }, onCatalog = { tab = "المنتجات" }, onTestOrder = {
                    val order = StoreOrder(UUID.randomUUID().toString().take(7).uppercase(), "زبون تجريبي", "+963 9XX XXX XXX", "طلب تجريبي", listOf(OrderLine("p1", products.firstOrNull()?.name ?: "فستق حلبي", 1, products.firstOrNull()?.salePrice ?: 850.0)), products.firstOrNull()?.salePrice ?: 850.0, "جديد", "الآن")
                    orders = listOf(order) + orders; saveOrders(); if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) orderNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else showTestNotification(context)
                })
            }
            if (message.isNotBlank()) {
                LaunchedEffect(message) { kotlinx.coroutines.delay(2200); message = "" }
                Surface(Modifier.align(Alignment.BottomCenter).padding(14.dp), color = Pine, shape = RoundedCornerShape(24.dp)) { Text(message, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp), fontSize = 12.sp) }
            }
        }
    }
    if (addMode || editor != null) ProductEditorDialog(editor, onDismiss = { addMode = false; editor = null }, onSave = { product ->
        val saved = if (editor == null) product.copy(id = UUID.randomUUID().toString()) else product
        products = if (editor == null) products + saved else products.map { if (it.id == saved.id) saved else it }
        saveProducts(); CloudSync.saveProduct(saved.toCloudMap()) { ok -> if (!ok) message = "لم تتم المزامنة. سجّل دخول المدير وتحقق من إعداد Firebase." }
        addMode = false; editor = null
    })
}

@Composable
private fun DashboardPage(products: List<Product>, orders: List<StoreOrder>, cloudConnected: Boolean, onOrders: () -> Unit, onCatalog: () -> Unit, onTestOrder: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { Row(Modifier.fillMaxWidth().background(Pine).padding(horizontal = 20.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("إدارة محامص الحرمين", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text("لوحة المحل", color = Color(0xFFD6E1D4), fontSize = 12.sp) }
            Text("☕", fontSize = 30.sp)
        } }
        item { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatCard("طلبات اليوم", orders.size.toString(), "▤", Modifier.weight(1f)); StatCard("المنتجات النشطة", products.count { it.available }.toString(), "◈", Modifier.weight(1f)); StatCard("مبيعات مسجلة", money(orders.filter { it.status == "تم التسليم" }.sumOf { it.total }), "↗", Modifier.weight(1f))
        } }
        item { Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), color = PaleOrange, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) {
            Text(if (cloudConnected) "المتجر متصل بحساب المدير" else "سجّل دخول المدير لبدء المزامنة", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(if (cloudConnected) "الطلبات والمنتجات تُزامن عبر Firebase. إشعار الطلب يحتاج نشر Cloud Function وتفعيل الإشعارات." else "أنشئ حساب المدير وأدخل بريده في تبويب الإعدادات، ثم أتم إعداد Firebase حسب دليل المشروع.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        } } }
        item { Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("أحدث الطلبات", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink); TextButton(onClick = onOrders) { Text("عرض الكل", color = Orange) } } }
        if (orders.isEmpty()) item { Empty("🔔", "بانتظار الطلبات", "ستظهر الطلبات الواردة عند تفعيل خدمة Firebase وتوفر اتصال الإنترنت") }
        items(orders.take(3)) { order -> OrderCard(order, onStatus = { _, _ -> }) }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCatalog, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Pine), shape = RoundedCornerShape(14.dp)) { Text("إدارة المنتجات") }
            OutlinedButton(onClick = onTestOrder, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("تجربة إشعار الطلب", color = Pine, fontSize = 11.sp) }
        } }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: String, modifier: Modifier) {
    Surface(modifier, color = Paper, shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(12.dp)) { Text(icon, color = Orange, fontSize = 17.sp); Text(value, color = Pine, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)); Text(title, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp)) } }
}

@Composable
private fun ProductsPage(products: List<Product>, onAdd: () -> Unit, onEdit: (Product) -> Unit, onAvailability: (Product) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Pine).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("المنتجات", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("صور وأسعار وعروض المحل", color = Color(0xFFD6E1D4), fontSize = 11.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Orange), shape = RoundedCornerShape(12.dp)) { Text("+ إضافة") } }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { p -> Surface(color = Paper, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2EEE5))) { ProductImage(p.image, p.emoji, Modifier.fillMaxSize()) }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(p.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1); Text("${p.category} · ${money(p.salePrice)} / ${p.unit}", color = Pine, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)); Text(if (p.discount > 0) "خصم ${p.discount}% · متوفر" else if (p.available) "متوفر" else "غير متوفر", color = if (p.available) PineLight else Muted, fontSize = 10.sp) }
                TextButton(onClick = { onEdit(p) }) { Text("تعديل", color = Orange, fontSize = 11.sp) }
                Switch(checked = p.available, onCheckedChange = { onAvailability(p) })
            } } }
        }
    }
}

@Composable
private fun OrdersPage(orders: List<StoreOrder>, onStatus: (StoreOrder, String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Pine).padding(18.dp)) { Text("طلبات الزبائن", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("إدارة التحضير والتسليم", color = Color(0xFFD6E1D4), fontSize = 11.sp) }
        if (orders.isEmpty()) Empty("📦", "لا توجد طلبات بعد", "عند تفعيل الربط ستصل طلبات الزبائن إلى هنا")
        else LazyColumn(contentPadding = PaddingValues(vertical = 9.dp)) { items(orders) { OrderCard(it, onStatus) } }
    }
}

@Composable
private fun OrderCard(order: StoreOrder, onStatus: (StoreOrder, String) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 6.dp), color = Paper, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("طلب #${order.id}", color = Pine, fontWeight = FontWeight.Bold); Text("${order.customer}  ·  ${order.phone}", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp)) }; Status(order.status) }
        Text("📍 ${order.address}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text(order.lines.joinToString("، ") { "${it.name} ×${it.qty}" }, color = Ink, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(order.created, color = Muted, fontSize = 10.sp); Text(money(order.total), color = Pine, fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("قيد التجهيز", "جاهز", "تم التسليم").forEach { s -> OutlinedButton(onClick = { onStatus(order, s) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp), shape = RoundedCornerShape(11.dp)) { Text(s, color = Pine, fontSize = 9.sp) } } }
    } }
}

@Composable
private fun SettingsPage(prefs: android.content.SharedPreferences, loggedIn: Boolean, onLogin: (String, String) -> Unit, onSignOut: () -> Unit, onPublishOffer: (Map<String, Any>) -> Unit, onTest: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var offerTitle by remember { mutableStateOf("") }
    var offerDescription by remember { mutableStateOf("") }
    var offerFooter by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf(prefs.getString("store_name", "محامص الحرمين") ?: "محامص الحرمين") }
    var managerPhone by remember { mutableStateOf(prefs.getString("manager_phone", "") ?: "") }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { showTestNotification(context); onTest("تم إرسال إشعار الاختبار") } else onTest("يرجى السماح بإشعارات التطبيق من إعدادات الهاتف")
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Pine).padding(18.dp)) { Text("إعدادات المتجر", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("إعداد استقبال الطلبات", color = Color(0xFFD6E1D4), fontSize = 11.sp) }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = Paper, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (loggedIn) "حساب المدير متصل" else "دخول المدير الآمن", color = Pine, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (loggedIn) Button(onClick = onSignOut, colors = ButtonDefaults.buttonColors(containerColor = Pine)) { Text("تسجيل الخروج") }
                    else {
                        OutlinedTextField(email, { email = it }, label = { Text("بريد المدير المسجل في Firebase") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        Button(onClick = { onLogin(email, password) }, enabled = email.isNotBlank() && password.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Pine)) { Text("دخول ومزامنة المتجر") }
                    }
                    Text("يتطلب إنشاء حساب المدير في Firebase ثم اعتماد UID الخاص به في قواعد Firestore.", color = Muted, fontSize = 10.sp)
                }
            }
            Text("العرض الظاهر للزبائن", color = Pine, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(offerTitle, { offerTitle = it }, label = { Text("عنوان العرض") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(offerDescription, { offerDescription = it }, label = { Text("وصف العرض") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(offerFooter, { offerFooter = it }, label = { Text("نص الخصم أو الدعوة للشراء") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(onClick = { onPublishOffer(mapOf("badge" to "عرض محامص الحرمين", "title" to offerTitle, "description" to offerDescription, "footer" to offerFooter, "updatedAt" to System.currentTimeMillis())) }, enabled = loggedIn && offerTitle.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Pine), modifier = Modifier.fillMaxWidth()) { Text("نشر العرض فوراً") }
            OutlinedTextField(storeName, { storeName = it; prefs.edit().putString("store_name", it).apply() }, label = { Text("اسم المتجر") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp))
            OutlinedTextField(managerPhone, { managerPhone = it; prefs.edit().putString("manager_phone", it).apply() }, label = { Text("رقم مدير استقبال الطلبات") }, placeholder = { Text("مثال: +963…") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp))
            Surface(color = Color(0xFFE9F0E8), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("العملة المعتمدة: الليرة السورية الجديدة (ل.س)", color = Pine, modifier = Modifier.padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            Surface(color = Paper, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("صوت إشعار الطلب", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("يتبع إعداد نغمة الإشعارات في الهاتف", color = Muted, fontSize = 10.sp) }; Switch(soundEnabled, { soundEnabled = it; prefs.edit().putBoolean("sound_enabled", it).apply(); recreateOrderChannel(context, it) }) } }
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { showTestNotification(context); onTest("تم إرسال إشعار الاختبار — تحقق من صوت الهاتف") }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Pine), shape = RoundedCornerShape(13.dp)) { Text("جرّب الإشعار والرنين") }
            Text("طلبات الزبائن والمنتجات والعروض تتزامن عبر Firebase بعد تسجيل دخول المدير وإعداد قواعد المشروع.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            HorizontalDivider(Modifier.padding(vertical = 5.dp), color = Line)
            Text("تم تطوير هذا البرنامج بواسطة المبرمج أمجد حبيب", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("+963988423100", color = PineLight, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProductEditorDialog(product: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var category by remember(product) { mutableStateOf(product?.category ?: "مكسرات") }
    var price by remember(product) { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    var unit by remember(product) { mutableStateOf(product?.unit ?: "١٠٠ غرام") }
    var desc by remember(product) { mutableStateOf(product?.description ?: "طازج ومنتقى بعناية") }
    var emoji by remember(product) { mutableStateOf(product?.emoji ?: "🥜") }
    var discount by remember(product) { mutableStateOf(product?.discount?.toString() ?: "0") }
    var featured by remember(product) { mutableStateOf(product?.featured ?: false) }
    var image by remember(product) { mutableStateOf(product?.image ?: "") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            image = uri.toString()
        }
    }
    val numericPrice = normalizeDigits(price).replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cream,
        shape = RoundedCornerShape(22.dp),
        title = { Text(if (product == null) "إضافة منتج" else "تعديل المنتج", color = Pine, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(onClick = { picker.launch(arrayOf("image/*")) }, color = Paper, shape = RoundedCornerShape(13.dp)) {
                    Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2EEE5))) {
                            ProductImage(image, emoji, Modifier.fillMaxSize())
                        }
                        Text(if (image.isBlank()) "اختيار صورة المنتج" else "تغيير الصورة", Modifier.padding(start = 10.dp), color = Pine, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("＋", color = Orange, fontSize = 22.sp)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(category, { category = it }, label = { Text("القسم") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(price, { price = it }, label = { Text("السعر · ل.س") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(unit, { unit = it }, label = { Text("الوزن") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                OutlinedTextField(discount, { discount = it }, label = { Text("خصم % (0 بدون خصم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(desc, { desc = it }, label = { Text("وصف المنتج") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(featured, { featured = it })
                    Text("منتج مميز", fontSize = 12.sp, color = Ink)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    numericPrice?.takeIf { it > 0 }?.let { amount ->
                        val discountPercent = normalizeDigits(discount).toIntOrNull()?.coerceIn(0, 90) ?: 0
                        onSave(Product(product?.id ?: "", name.trim(), category.trim().ifBlank { "مكسرات" }, amount, unit.trim().ifBlank { "١٠٠ غرام" }, emoji, desc, featured, true, discountPercent, image))
                    }
                },
                enabled = name.isNotBlank() && numericPrice != null && numericPrice > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Pine),
                shape = RoundedCornerShape(12.dp)
            ) { Text("حفظ المنتج") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = Muted) } }
    )
}

@Composable
private fun ProductImage(uri: String, emoji: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) { if (uri.isBlank()) null else runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
}

@Composable
private fun Status(status: String) { Surface(color = if (status == "جديد") PaleOrange else Color(0xFFE9F0E8), shape = RoundedCornerShape(20.dp)) { Text(status, color = if (status == "جديد") Orange else Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }

@Composable
private fun Empty(icon: String, title: String, hint: String) { Column(Modifier.fillMaxWidth().padding(38.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 38.sp); Text(title, color = Pine, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp)); Text(hint, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)) } }

internal fun createOrderChannel(context: Context, soundEnabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val sound = if (soundEnabled) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else null
        val attrs = if (soundEnabled) AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build() else null
        val channel = NotificationChannel(ORDER_CHANNEL, "طلبات محامص الحرمين", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "تنبيه صوتي عند وصول طلب جديد"
            enableVibration(true)
            setSound(sound, attrs)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
private fun recreateOrderChannel(context: Context, soundEnabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).deleteNotificationChannel(ORDER_CHANNEL)
        createOrderChannel(context, soundEnabled)
    }
}
private fun showTestNotification(context: Context) {
    val notification = NotificationCompat.Builder(context, ORDER_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("محامص الحرمين")
        .setContentText("هذا إشعار تجريبي لوصول طلب جديد")
        .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
    NotificationManagerCompat.from(context).notify(4001, notification)
}
private fun normalizeDigits(value: String): String = buildString {
    value.forEach { ch ->
        when (ch) {
            in '٠'..'٩' -> append(('0'.code + (ch - '٠')).toChar())
            in '۰'..'۹' -> append(('0'.code + (ch - '۰')).toChar())
            else -> append(ch)
        }
    }
}
private fun money(value: Double): String = NumberFormat.getNumberInstance(Locale("ar")).apply { minimumFractionDigits = 0; maximumFractionDigits = 0 }.format(value) + " ل.س"

private fun Product.toCloudMap(): Map<String, Any> = mapOf(
    "id" to id, "name" to name, "category" to category, "price" to price, "unit" to unit,
    "emoji" to emoji, "description" to description, "featured" to featured,
    "available" to available, "discount" to discount
)
private fun productFromCloud(data: Map<String, Any>): Product? = runCatching {
    Product(data["id"].toString(), data["name"].toString(), data["category"].toString(), (data["price"] as Number).toDouble(), data["unit"]?.toString() ?: "١٠٠ غرام", data["emoji"]?.toString() ?: "🥜", data["description"]?.toString() ?: "", data["featured"] as? Boolean ?: false, data["available"] as? Boolean ?: true, (data["discount"] as? Number)?.toInt() ?: 0)
}.getOrNull()
private fun orderFromCloud(data: Map<String, Any>): StoreOrder? = runCatching {
    val lines = (data["lines"] as List<*>).map { raw -> val line = raw as Map<*, *>; OrderLine(line["productId"].toString(), line["name"].toString(), (line["qty"] as Number).toInt(), (line["price"] as Number).toDouble()) }
    StoreOrder(data["id"].toString(), data["customer"].toString(), data["phone"].toString(), data["address"].toString(), lines, (data["total"] as Number).toDouble(), data["status"]?.toString() ?: "جديد", "وصل حديثاً")
}.getOrNull()

private fun loadProducts(prefs: android.content.SharedPreferences): List<Product> {
    val raw = prefs.getString("products", null) ?: return seedProducts()
    return runCatching { val a = JSONArray(raw); (0 until a.length()).map { i -> val o = a.getJSONObject(i); Product(o.getString("id"), o.getString("name"), o.getString("category"), o.getDouble("price"), o.optString("unit", "١٠٠ غرام"), o.optString("emoji", "🥜"), o.optString("description", ""), o.optBoolean("featured"), o.optBoolean("available", true), o.optInt("discount"), o.optString("image", "")) } }.getOrElse { seedProducts() }
}
private fun productsJson(list: List<Product>) = JSONArray().apply { list.forEach { p -> put(JSONObject().put("id", p.id).put("name", p.name).put("category", p.category).put("price", p.price).put("unit", p.unit).put("emoji", p.emoji).put("description", p.description).put("featured", p.featured).put("available", p.available).put("discount", p.discount).put("image", p.image)) } }.toString()
private fun loadOrders(prefs: android.content.SharedPreferences): List<StoreOrder> = runCatching { val a = JSONArray(prefs.getString("orders", "[]")); (0 until a.length()).map { i -> val o = a.getJSONObject(i); val ls = o.getJSONArray("lines"); StoreOrder(o.getString("id"), o.getString("customer"), o.getString("phone"), o.getString("address"), (0 until ls.length()).map { j -> val l = ls.getJSONObject(j); OrderLine(l.getString("productId"), l.getString("name"), l.getInt("qty"), l.getDouble("price")) }, o.getDouble("total"), o.getString("status"), o.getString("created")) } }.getOrElse { emptyList() }
private fun ordersJson(list: List<StoreOrder>) = JSONArray().apply { list.forEach { o -> val lines = JSONArray().apply { o.lines.forEach { l -> put(JSONObject().put("productId", l.productId).put("name", l.name).put("qty", l.qty).put("price", l.price)) } }; put(JSONObject().put("id", o.id).put("customer", o.customer).put("phone", o.phone).put("address", o.address).put("lines", lines).put("total", o.total).put("status", o.status).put("created", o.created)) } }.toString()
