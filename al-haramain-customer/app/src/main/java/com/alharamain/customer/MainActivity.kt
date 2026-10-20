package com.alharamain.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import com.google.firebase.firestore.ListenerRegistration

private val Pine = Color(0xFF173D31)
private val PineLight = Color(0xFF275745)
private val Cream = Color(0xFFFAF7F0)
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF252A25)
private val Muted = Color(0xFF777D74)
private val Orange = Color(0xFFD77A35)
private val PaleOrange = Color(0xFFFFE9D5)
private val Line = Color(0xFFE9E5DB)
private val ArabicFont = FontFamily(Font(R.font.tajawal_regular), Font(R.font.tajawal_medium, FontWeight.Medium), Font(R.font.tajawal_bold, FontWeight.Bold), Font(R.font.tajawal_extrabold, FontWeight.ExtraBold))

private data class Product(
    val id: String, val name: String, val category: String, val price: Double,
    val unit: String, val emoji: String, val description: String = "طازج ومنتقى بعناية",
    val featured: Boolean = false, val available: Boolean = true, val discountPercent: Int = 0
) {
    val salePrice: Double get() = price * (100 - discountPercent.coerceIn(0, 90)) / 100.0
}
private data class OrderLine(val productId: String, val name: String, val qty: Int, val price: Double)
private data class ShopOrder(
    val id: String, val customer: String, val phone: String, val address: String, val lines: List<OrderLine>,
    val total: Double, val status: String, val created: String
)

private fun seedProducts() = listOf(
    Product("p1", "فستق حلبي فاخر", "مكسرات", 850.0, "١٠٠ غرام", "🥜", "فستق محمّص باعتدال، طعم غني وجودة ممتازة", true),
    Product("p2", "كاجو محمّص", "مكسرات", 700.0, "١٠٠ غرام", "🌰", "كاجو ذهبي محمّص طازجاً", true),
    Product("p3", "قهوة عربية بالهيل", "قهوة", 1200.0, "٢٥٠ غرام", "☕", "خلطة البيت، محمّصة يومياً", true),
    Product("p4", "خلطة ضيافة الحرمين", "خلطات", 950.0, "٢٠٠ غرام", "🎁", "تشكيلة مكسرات فاخرة للضيافة", discountPercent = 15),
    Product("p5", "لوز أمريكي", "مكسرات", 600.0, "١٠٠ غرام", "🥜"),
    Product("p6", "بندق محمّص", "مكسرات", 750.0, "١٠٠ غرام", "🌰"),
    Product("p7", "كاندي مشكّل", "كاندي", 400.0, "١٥٠ غرام", "🍬", "ألوان ونكهات يحبّها الكبار والصغار"),
    Product("p8", "مقرمشات مشكلة", "مقرمشات", 350.0, "١٥٠ غرام", "🥨"),
    Product("p9", "قهوة تركية", "قهوة", 1000.0, "٢٥٠ غرام", "🫘")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(250, 247, 240)
        window.navigationBarColor = android.graphics.Color.rgb(250, 247, 240)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Pine, secondary = Orange, background = Cream, surface = Paper)) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = ArabicFont)
                ) {
                    ShopApp(getSharedPreferences("store_data", Context.MODE_PRIVATE))
                }
            }
        }
    }
}

@Composable
private fun ShopApp(prefs: android.content.SharedPreferences) {
    var products by remember { mutableStateOf(seedProducts()) }
    var orders by remember { mutableStateOf(emptyList<ShopOrder>()) }
    var offer by remember { mutableStateOf<Map<String, Any>?>(null) }
    var userId by remember { mutableStateOf<String?>(null) }
    var cloudReady by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val productListener = CloudSync.watchProducts { rows -> products = rows.mapNotNull(::productFromCloud).ifEmpty { seedProducts() } }
        val offerListener = CloudSync.watchOffer { offer = it }
        var orderListener: ListenerRegistration? = null
        var disposed = false
        CloudSync.ensureSignedIn { uid ->
            if (!disposed) {
                userId = uid
                cloudReady = uid != null
                if (uid != null) orderListener = CloudSync.watchOrders(uid) { rows -> orders = rows.mapNotNull(::orderFromCloud) }
            }
        }
        onDispose { disposed = true; productListener.remove(); offerListener.remove(); orderListener?.remove() }
    }
    var cart by remember { mutableStateOf(mapOf<String, Int>()) }
    var page by remember { mutableStateOf("الرئيسية") }
    var category by remember { mutableStateOf("الكل") }
    var query by remember { mutableStateOf("") }
    var checkout by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }

    fun saveOrders() { prefs.edit().putString("orders", ordersJson(orders)).apply() }
    val cartTotal = products.sumOf { p -> p.salePrice * (cart[p.id] ?: 0) }
    val cartCount = cart.values.sum()
    val cats = listOf("الكل", "مكسرات", "قهوة", "خلطات", "كاندي", "مقرمشات")
    val visibleProducts = products.filter { it.available && (category == "الكل" || it.category == category) && (query.isBlank() || it.name.contains(query, true)) }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Paper, tonalElevation = 8.dp) {
                val items = listOf("الرئيسية" to "⌂", "طلباتي" to "▤", "حسابي" to "◉")
                items.forEach { (label, glyph) ->
                    NavigationBarItem(selected = page == label, onClick = { page = label }, icon = { Text(glyph, fontSize = 21.sp) }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Pine, selectedTextColor = Pine, indicatorColor = Color(0xFFE9F0E8)))
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                "طلباتي" -> OrdersScreen(orders)
                "حسابي" -> ProfileScreen(orderCount = orders.size)
                else -> HomeScreen(
                    products = visibleProducts, categories = cats, category = category, query = query,
                    cartCount = cartCount, cartTotal = cartTotal, offer = offer,
                    onCategory = { category = it }, onQuery = { query = it },
                    onAdd = { p -> cart = cart + (p.id to ((cart[p.id] ?: 0) + 1)); toast = "أُضيف ${p.name} إلى السلة" },
                    onCart = { checkout = true }
                )
            }
            if (toast.isNotBlank()) {
                LaunchedEffect(toast) { kotlinx.coroutines.delay(1800); toast = "" }
                Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).clickable { toast = "" }, color = Pine, shape = RoundedCornerShape(30.dp), shadowElevation = 6.dp) {
                    Text(toast, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontSize = 13.sp)
                }
            }
        }
    }

    if (checkout) CheckoutDialog(
        products = products, cart = cart, onDismiss = { checkout = false },
        onSubmit = { name, phone, address, lines, total ->
            val uid = userId
            if (uid == null || !cloudReady) { toast = "تعذّر الاتصال بالخادم؛ تحقّق من الإنترنت ثم أعد المحاولة" }
            else {
                val orderData = mapOf("customer" to name, "phone" to phone, "address" to address, "lines" to lines.map { mapOf("productId" to it.productId, "name" to it.name, "qty" to it.qty, "price" to it.price) }, "total" to total)
                CloudSync.submitOrder(uid, orderData) { ok ->
                    if (ok) { cart = emptyMap(); checkout = false; page = "طلباتي"; toast = "وصل طلبك إلى محامص الحرمين" }
                    else toast = "لم يُرسل الطلب؛ تحقّق من الاتصال وحاول مجدداً"
                }
            }
        }, onQty = { id, delta -> val next = (cart[id] ?: 0) + delta; cart = if (next <= 0) cart - id else cart + (id to next) }
    )

}

@Composable
private fun HomeScreen(products: List<Product>, categories: List<String>, category: String, query: String, cartCount: Int, cartTotal: Double, offer: Map<String, Any>?, onCategory: (String) -> Unit, onQuery: (String) -> Unit, onAdd: (Product) -> Unit, onCart: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("محامص الحرمين", fontSize = 23.sp, color = Pine, fontWeight = FontWeight.ExtraBold); Text("نكهة أصيلة... وضيافة من القلب", fontSize = 12.sp, color = Muted) }
            Surface(shape = CircleShape, color = Color(0xFFE9F0E8), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Text("🌰", fontSize = 22.sp) } }
        } }
        item { Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(182.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Pine, Color(0xFF31644A))))) {
            Column(Modifier.align(Alignment.CenterStart).padding(start = 23.dp, end = 20.dp)) {
                Surface(color = Orange, shape = RoundedCornerShape(20.dp)) { Text(offer?.get("badge")?.toString()?.takeIf { it.isNotBlank() } ?: "عرض الأسبوع", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)) }
                Spacer(Modifier.height(12.dp)); Text(offer?.get("title")?.toString()?.takeIf { it.isNotBlank() } ?: "ضيافتك علينا أطيب", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(offer?.get("description")?.toString()?.takeIf { it.isNotBlank() } ?: "خلطة الحرمين الفاخرة بانتظارك", color = Color(0xFFE7E8D9), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                Text(offer?.get("footer")?.toString()?.takeIf { it.isNotBlank() } ?: "خصومات على أصناف مختارة", color = Color(0xFFFFD5A8), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            }
            Text("🥜", Modifier.align(Alignment.CenterEnd).padding(end = 22.dp), fontSize = 64.sp)
        } }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("طازج يومياً" to "☀", "توصيل سريع" to "♧", "ضيافة مميزة" to "✦").forEach { (s, i) -> Surface(Modifier.weight(1f), color = Paper, shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(horizontal = 8.dp, vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("$i  ", color = Orange, fontSize = 14.sp); Text(s, fontSize = 10.sp, color = Pine, fontWeight = FontWeight.SemiBold) } } }
        } }
        item { OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), placeholder = { Text("ابحث عن مكسراتك المفضلة", fontSize = 13.sp) }, leadingIcon = { Text("⌕", fontSize = 22.sp, color = Muted) }, shape = RoundedCornerShape(15.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Line, focusedBorderColor = Pine, unfocusedContainerColor = Paper, focusedContainerColor = Paper)) }
        item { Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("تسوّق حسب القسم", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold); Text("عرض الكل  ←", color = Orange, fontSize = 12.sp) } }
        item { androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { c -> val selected = c == category; Surface(onClick = { onCategory(c) }, color = if (selected) Pine else Paper, shape = RoundedCornerShape(24.dp), border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Line)) { Text(c, color = if (selected) Color.White else Ink, modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } } }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(if (category == "الكل") "الأكثر طلباً" else category, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold); Text("${products.size} منتجات", color = Muted, fontSize = 11.sp) } }
        if (products.isEmpty()) item { Text("لا توجد منتجات بهذا الاسم حالياً", Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center, color = Muted) }
        items(products.chunked(2)) { row -> Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { p -> ProductCard(p, Modifier.weight(1f), onAdd) }; if (row.size == 1) Spacer(Modifier.weight(1f)) }; Spacer(Modifier.height(10.dp)) }
        if (cartCount > 0) item { Button(onClick = onCart, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Pine), shape = RoundedCornerShape(16.dp)) { Text("السلة  ·  $cartCount منتجات", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(money(cartTotal), fontWeight = FontWeight.Bold) } }
        item { Text("جودة نختارها لك بكل محبة  ·  محامص الحرمين", Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), color = Muted, textAlign = TextAlign.Center, fontSize = 11.sp) }
    }
}

@Composable
private fun ProductCard(p: Product, modifier: Modifier, onAdd: (Product) -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(19.dp), color = Paper, shadowElevation = 1.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(112.dp).background(if (p.featured) Color(0xFFF0E8D9) else Color(0xFFF4F0E8)), contentAlignment = Alignment.Center) {
                Text(p.emoji, fontSize = 50.sp, modifier = Modifier.align(Alignment.Center))
                if (p.featured || p.discountPercent > 0) Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = PaleOrange, shape = RoundedCornerShape(14.dp)) { Text(if (p.discountPercent > 0) "خصم ${p.discountPercent}%" else "الأكثر طلباً", color = Orange, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) }
            }
            Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.unit, fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        if (p.discountPercent > 0) Text("${p.discountPercent}% خصم", fontSize = 9.sp, color = Orange, fontWeight = FontWeight.Bold)
                        Text(money(p.salePrice), fontSize = 14.sp, color = Pine, fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(onClick = { onAdd(p) }, color = Pine, shape = CircleShape, modifier = Modifier.size(31.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 21.sp, lineHeight = 21.sp) } }
                }
            }
        }
    }
}

@Composable
private fun OrdersScreen(orders: List<ShopOrder>) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("طلباتي", Modifier.padding(top = 22.dp), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Pine)
        Text("تابع طلباتك أولاً بأول", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

        if (orders.isEmpty()) EmptyState("🧺", "لسّه ما طلبت شي", "اكتشف ألذ المكسرات والقهوة من متجرنا")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(orders) { order ->
            Surface(color = Paper, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("طلب #${order.id}", color = Pine, fontWeight = FontWeight.Bold); StatusPill(order.status) }
                Text(order.lines.joinToString("، ") { "${it.name} ×${it.qty}" }, color = Muted, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 10.dp))
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${order.created}  ·  ${order.customer}", color = Muted, fontSize = 11.sp); Text(money(order.total), color = Pine, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Row(Modifier.fillMaxWidth().padding(top = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("جديد", "قيد التجهيز", "جاهز", "تم التسليم").forEachIndexed { idx, label -> val active = listOf("جديد", "قيد التجهيز", "جاهز", "تم التسليم").indexOf(order.status) >= idx
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(12.dp).clip(CircleShape).background(if (active) Orange else Line)); Text(label, fontSize = 8.sp, color = if (active) Pine else Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            } }
        } }
    }
}

@Composable
private fun ProfileScreen(orderCount: Int) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("حسابي", Modifier.padding(top = 10.dp), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Pine)
        Surface(Modifier.fillMaxWidth().padding(top = 20.dp), color = Pine, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("أهلاً بك في محامص الحرمين", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$orderCount طلبات محفوظة على هذا الجهاز", color = Color(0xFFD6E1D4), fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        Text("عن التطبيق", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        Surface(Modifier.fillMaxWidth(), color = Paper, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text("محامص الحرمين", color = Pine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("مكسرات، قهوة، ضيافة، كاندي ومقرمشات", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)
                Text("تم تطوير هذا البرنامج بواسطة المبرمج أمجد حبيب", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("+963988423100", color = PineLight, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Surface(Modifier.fillMaxWidth().padding(top = 14.dp), color = PaleOrange, shape = RoundedCornerShape(16.dp)) {
            Text("الطلبات والعروض تتزامن عبر Firebase عند تفعيل إعدادات المشروع وقواعده.", Modifier.padding(15.dp), color = Ink, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatusPill(status: String) { Surface(color = if (status == "جديد") PaleOrange else Color(0xFFE9F0E8), shape = RoundedCornerShape(18.dp)) { Text(status, color = if (status == "جديد") Orange else Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }

@Composable
private fun CheckoutDialog(products: List<Product>, cart: Map<String, Int>, onDismiss: () -> Unit, onSubmit: (String, String, String, List<OrderLine>, Double) -> Unit, onQty: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
    val lines = products.filter { (cart[it.id] ?: 0) > 0 }.map { OrderLine(it.id, it.name, cart[it.id] ?: 0, it.salePrice) }
    val total = lines.sumOf { it.qty * it.price }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Cream, shape = RoundedCornerShape(24.dp), title = { Text("سلة مشترياتك", color = Pine, fontWeight = FontWeight.ExtraBold) }, text = {
        Column(Modifier.heightIn(max = 530.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(lines) { line -> Row(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(13.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(line.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(money(line.price * line.qty), color = Pine, fontSize = 11.sp) }; TextButton(onClick = { onQty(line.productId, -1) }) { Text("−", fontSize = 18.sp, color = Pine) }; Text("${line.qty}", color = Ink); TextButton(onClick = { onQty(line.productId, 1) }) { Text("+", fontSize = 18.sp, color = Pine) } } } }
            Text("الاسم", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)); OutlinedTextField(name, { name = it }, placeholder = { Text("الاسم الكامل", fontSize = 12.sp) }, singleLine = true, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth())
            Text("رقم الهاتف", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)); OutlinedTextField(phone, { phone = it }, placeholder = { Text("للتواصل وتأكيد التوصيل", fontSize = 12.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth())
            Text("عنوان التوصيل", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)); OutlinedTextField(address, { address = it }, placeholder = { Text("الشارع، الحي، أو أقرب علامة", fontSize = 12.sp) }, singleLine = true, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("الإجمالي", fontWeight = FontWeight.Bold, color = Ink); Text(money(total), fontWeight = FontWeight.ExtraBold, color = Pine) }
            Text("يرسل الطلب مباشرة إلى المتجر عند توفر اتصال الإنترنت.", color = Orange, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }, confirmButton = { Button(onClick = { onSubmit(name.trim(), phone.trim(), address.trim(), lines, total) }, enabled = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank() && lines.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Pine), shape = RoundedCornerShape(13.dp)) { Text("إرسال الطلب") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("متابعة التسوق", color = Muted) } })
}

@Composable
private fun EmptyState(emoji: String, title: String, message: String) { Column(Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, fontSize = 42.sp); Text(title, color = Pine, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)) } }

private fun productFromCloud(data: Map<String, Any>): Product? = runCatching {
    Product(data["id"].toString(), data["name"].toString(), data["category"].toString(), (data["price"] as Number).toDouble(), data["unit"]?.toString() ?: "١٠٠ غرام", data["emoji"]?.toString() ?: "🥜", data["description"]?.toString() ?: "طازج ومنتقى بعناية", data["featured"] as? Boolean ?: false, data["available"] as? Boolean ?: true, (data["discount"] as? Number)?.toInt() ?: 0)
}.getOrNull()

private fun orderFromCloud(data: Map<String, Any>): ShopOrder? = runCatching {
    val lines = (data["lines"] as List<*>).map { raw ->
        val item = raw as Map<*, *>
        OrderLine(item["productId"].toString(), item["name"].toString(), (item["qty"] as Number).toInt(), (item["price"] as Number).toDouble())
    }
    ShopOrder(data["id"].toString().takeLast(8).uppercase(), data["customer"].toString(), data["phone"].toString(), data["address"].toString(), lines, (data["total"] as Number).toDouble(), data["status"]?.toString() ?: "جديد", "وصل حديثاً")
}.getOrNull()

private fun money(amount: Double): String = NumberFormat.getNumberInstance(Locale("ar")).apply { minimumFractionDigits = 0; maximumFractionDigits = 0 }.format(amount) + " ل.س"

private fun loadOrders(prefs: android.content.SharedPreferences): List<ShopOrder> = try { val a = JSONArray(prefs.getString("orders", "[]")); (0 until a.length()).map { i -> val o = a.getJSONObject(i); val ls = o.getJSONArray("lines"); ShopOrder(o.getString("id"), o.getString("customer"), o.getString("phone"), o.optString("address", ""), (0 until ls.length()).map { j -> val l = ls.getJSONObject(j); OrderLine(l.getString("productId"), l.getString("name"), l.getInt("qty"), l.getDouble("price")) }, o.getDouble("total"), o.getString("status"), o.getString("created")) } } catch (_: Exception) { emptyList() }
private fun ordersJson(orders: List<ShopOrder>) = JSONArray().apply { orders.forEach { o -> val lines = JSONArray().apply { o.lines.forEach { l -> put(JSONObject().put("productId", l.productId).put("name", l.name).put("qty", l.qty).put("price", l.price)) } }; put(JSONObject().put("id", o.id).put("customer", o.customer).put("phone", o.phone).put("address", o.address).put("lines", lines).put("total", o.total).put("status", o.status).put("created", o.created)) } }.toString()
