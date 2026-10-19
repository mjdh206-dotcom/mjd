package com.alharamayn.mahamess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.ui.screens.AuditScreen
import com.alharamayn.mahamess.ui.screens.CashScreen
import com.alharamayn.mahamess.ui.screens.CustomersScreen
import com.alharamayn.mahamess.ui.screens.DashboardScreen
import com.alharamayn.mahamess.ui.screens.ExpensesScreen
import com.alharamayn.mahamess.ui.screens.InventoryScreen
import com.alharamayn.mahamess.ui.screens.InvoiceDetailScreen
import com.alharamayn.mahamess.ui.screens.InvoicesScreen
import com.alharamayn.mahamess.ui.screens.ItemsScreen
import com.alharamayn.mahamess.ui.screens.LockScreen
import com.alharamayn.mahamess.ui.screens.MoreScreen
import com.alharamayn.mahamess.ui.screens.PreordersScreen
import com.alharamayn.mahamess.ui.screens.PurchasesScreen
import com.alharamayn.mahamess.ui.screens.ReportsScreen
import com.alharamayn.mahamess.ui.screens.SaleScreen
import com.alharamayn.mahamess.ui.screens.SettingsScreen
import com.alharamayn.mahamess.ui.screens.StatementScreen
import com.alharamayn.mahamess.ui.screens.SuppliersScreen
import com.alharamayn.mahamess.ui.screens.UsersScreen
import com.alharamayn.mahamess.ui.screens.WorkersScreen
import com.alharamayn.mahamess.ui.screens.WithdrawalsScreen
import com.alharamayn.mahamess.util.Money

sealed class Screen {
    data object Dashboard : Screen()
    data object Sale : Screen()
    data class EditInvoice(val id: Long) : Screen()
    data object Invoices : Screen()
    data class InvoiceDetail(val id: Long) : Screen()
    data object Items : Screen()
    data object Customers : Screen()
    data class Statement(val customerId: Long) : Screen()
    data object Suppliers : Screen()
    data object Purchases : Screen()
    data object Inventory : Screen()
    data object Workers : Screen()
    data object Withdrawals : Screen()
    data object Expenses : Screen()
    data object Cash : Screen()
    data object Preorders : Screen()
    data object Reports : Screen()
    data object Users : Screen()
    data object Audit : Screen()
    data object Settings : Screen()
    data object More : Screen()
}

@Composable
fun MahamessApp(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MahamessTheme(darkTheme = settings.theme == "dark") {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (settings.pinEnabled && session == null) {
                    LockScreen(vm, settings)
                } else {
                    LaunchedEffect(Unit) {
                        if (session == null) vm.login(settings.managerPin)
                    }
                    MainShell(vm)
                }
            }
        }
    }
}

@Composable
private fun MainShell(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Dashboard)) }
    val current = stack.last()

    fun navigate(screen: Screen) { stack = stack + screen }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home(screen: Screen) { stack = listOf(screen) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ShopHeader(
                shopName = settings.shopName,
                branch = settings.branch,
                rate = settings.rate,
                userName = session?.name ?: "",
                isManager = session?.role == "manager",
                dark = settings.theme == "dark",
                onToggleTheme = {
                    vm.updateSettings { it.copy(theme = if (it.theme == "dark") "light" else "dark") }
                },
                onRateClick = { navigate(Screen.Settings) },
                onLogout = { vm.logout() },
                onBack = if (stack.size > 1) ({ back() }) else null,
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val tabs = listOf(
                    Triple("الرئيسية", "🏠", Screen.Dashboard),
                    Triple("فاتورة", "🧾", Screen.Sale),
                    Triple("الفواتير", "📑", Screen.Invoices),
                    Triple("الزبائن", "👥", Screen.Customers),
                    Triple("المزيد", "☰", Screen.More),
                )
                tabs.forEach { (label, icon, screen) ->
                    val selected = when (screen) {
                        Screen.Dashboard -> current is Screen.Dashboard
                        Screen.Sale -> current is Screen.Sale
                        Screen.Invoices -> current is Screen.Invoices
                        Screen.Customers -> current is Screen.Customers || current is Screen.Statement
                        else -> current is Screen.More || current is Screen.Settings || current is Screen.Reports ||
                            current is Screen.Items || current is Screen.Inventory || current is Screen.Expenses ||
                            current is Screen.Cash || current is Screen.Withdrawals || current is Screen.Workers ||
                            current is Screen.Purchases || current is Screen.Suppliers || current is Screen.Preorders ||
                            current is Screen.Users || current is Screen.Audit
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (screen == Screen.Sale) { if (current !is Screen.Sale) navigate(Screen.Sale) } else home(screen) },
                        icon = { Text(icon, fontSize = 18.sp) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val screen = current) {
                is Screen.Dashboard -> DashboardScreen(vm, ::navigate)
                is Screen.Sale -> SaleScreen(vm, null, ::back)
                is Screen.EditInvoice -> SaleScreen(vm, screen.id, ::back)
                is Screen.Invoices -> InvoicesScreen(vm, ::navigate)
                is Screen.InvoiceDetail -> InvoiceDetailScreen(vm, screen.id, ::navigate, ::back)
                is Screen.Items -> ItemsScreen(vm)
                is Screen.Customers -> CustomersScreen(vm, ::navigate)
                is Screen.Statement -> StatementScreen(vm, screen.customerId, ::back)
                is Screen.Suppliers -> SuppliersScreen(vm)
                is Screen.Purchases -> PurchasesScreen(vm, ::navigate)
                is Screen.Inventory -> InventoryScreen(vm)
                is Screen.Workers -> WorkersScreen(vm)
                is Screen.Withdrawals -> WithdrawalsScreen(vm)
                is Screen.Expenses -> ExpensesScreen(vm)
                is Screen.Cash -> CashScreen(vm)
                is Screen.Preorders -> PreordersScreen(vm)
                is Screen.Reports -> ReportsScreen(vm)
                is Screen.Users -> UsersScreen(vm)
                is Screen.Audit -> AuditScreen(vm)
                is Screen.Settings -> SettingsScreen(vm)
                is Screen.More -> MoreScreen(::navigate)
            }
        }
    }
}

@Composable
private fun ShopHeader(
    shopName: String,
    branch: String,
    rate: Long,
    userName: String,
    isManager: Boolean,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onRateClick: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(BrownDark, Brown, Color(0xFF6D4C41))))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) { Text("→", color = Color.White, fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Gold, GoldLight))),
                contentAlignment = Alignment.Center,
            ) { Text("مح", color = BrownDark, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(shopName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("إدارة الفواتير والحسابات • $branch", color = Color(0xCCFFFFFF), fontSize = 9.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x22FFFFFF))
                    .clickable { onRateClick() }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text("💵 ${Money.formatRate(rate)} ل.س", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x22FFFFFF)).clickable { onToggleTheme() },
                contentAlignment = Alignment.Center,
            ) { Text(if (dark) "☀️" else "🌙", fontSize = 13.sp) }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x22FFFFFF)).clickable { onLogout() },
                contentAlignment = Alignment.Center,
            ) { Text("⎋", color = Color.White, fontSize = 14.sp) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${userName} • ${if (isManager) "مدير" else "عامل"}",
            color = Color(0xAAFFFFFF), fontSize = 9.sp,
        )
    }
}

@Composable
fun ActionChip(text: String, onClick: () -> Unit, filled: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RowActions(vararg actions: Pair<String, () -> Unit>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        actions.forEach { (label, action) -> ActionChip(label, action) }
    }
}
