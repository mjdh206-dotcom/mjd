package com.alharamayn.mahamess

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alharamayn.mahamess.data.AppSettings
import com.alharamayn.mahamess.data.ClosingEntity
import com.alharamayn.mahamess.data.CustomerEntity
import com.alharamayn.mahamess.data.ExpenseEntity
import com.alharamayn.mahamess.data.InvoiceDraft
import com.alharamayn.mahamess.data.InvoiceWithLines
import com.alharamayn.mahamess.data.ItemEntity
import com.alharamayn.mahamess.data.PaymentEntity
import com.alharamayn.mahamess.data.PreorderEntity
import com.alharamayn.mahamess.data.Repo
import com.alharamayn.mahamess.data.SettingsStore
import com.alharamayn.mahamess.data.SupplierEntity
import com.alharamayn.mahamess.data.UserEntity
import com.alharamayn.mahamess.data.WorkerEntity
import com.alharamayn.mahamess.data.WorkerTxEntity
import com.alharamayn.mahamess.data.WithdrawalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = Repo(app, SettingsStore(app))

    val settings: StateFlow<AppSettings> =
        repo.settingsStore.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val items = repo.dao.items().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customers = repo.dao.customers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val suppliers = repo.dao.suppliers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val workers = repo.dao.workers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val invoices = repo.dao.allInvoicesWithLines().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val payments = repo.dao.payments().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val withdrawals = repo.dao.withdrawals().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val workerTx = repo.dao.workerTx().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val expenses = repo.dao.expenses().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val closings = repo.dao.closings().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val preorders = repo.dao.preorders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val stockMoves = repo.dao.stockMoves().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val roasts = repo.dao.roasts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val stocktakes = repo.dao.stocktakes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val audit = repo.dao.audit().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val users = repo.dao.users().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _session = MutableStateFlow<UserEntity?>(null)
    val session: StateFlow<UserEntity?> = _session.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val isManager: Boolean get() = _session.value?.role == "manager"
    val userName: String get() = _session.value?.name ?: "المدير"

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            if (!settings.value.pinEnabled) login("1234")
        }
    }

    fun notify(text: String) { _message.value = text }
    fun consumeMessage() { _message.value = null }

    fun login(pin: String) {
        viewModelScope.launch {
            val user = repo.verifyPin(pin)
            if (user != null) _session.value = user else notify("الرقم السري غير صحيح")
        }
    }

    fun loginAs(user: UserEntity) { _session.value = user }
    fun logout() { _session.value = null }

    /* ---------- إعدادات ---------- */
    fun updateSettings(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.settingsStore.update(block) }
    }

    /* ---------- أصناف ---------- */
    fun saveItem(item: ItemEntity, done: () -> Unit = {}) = viewModelScope.launch {
        repo.saveItem(item, userName); notify("تم حفظ الصنف"); done()
    }

    fun deleteItem(item: ItemEntity) = viewModelScope.launch {
        repo.deleteItem(item, userName); notify("تم حذف الصنف")
    }

    /* ---------- فواتير ---------- */
    fun createInvoice(draft: InvoiceDraft, done: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repo.createInvoice(draft)
        notify("تم حفظ الفاتورة")
        done(id)
    }

    fun updateInvoice(invoice: InvoiceWithLines, draft: InvoiceDraft, done: () -> Unit = {}) = viewModelScope.launch {
        repo.updateInvoice(invoice, draft, userName); notify("تم تعديل الفاتورة"); done()
    }

    fun cancelInvoice(invoice: InvoiceWithLines, reason: String, done: () -> Unit = {}) = viewModelScope.launch {
        repo.cancelInvoice(invoice, reason, userName); notify("تم إلغاء الفاتورة"); done()
    }

    /* ---------- أطراف ---------- */
    fun saveCustomer(c: CustomerEntity) = viewModelScope.launch {
        repo.dao.insertCustomer(c)
        repo.audit(userName, if (c.id == 0L) "إنشاء" else "تعديل", "زبون", c.name, "")
        notify("تم حفظ الزبون")
    }

    fun saveSupplier(s: SupplierEntity) = viewModelScope.launch {
        repo.dao.insertSupplier(s)
        repo.audit(userName, if (s.id == 0L) "إنشاء" else "تعديل", "مورد", s.name, "")
        notify("تم حفظ المورد")
    }

    fun saveWorker(w: WorkerEntity) = viewModelScope.launch {
        repo.dao.insertWorker(w)
        repo.audit(userName, if (w.id == 0L) "إنشاء" else "تعديل", "عامل", w.name, "")
        notify("تم حفظ العامل")
    }

    fun addPayment(p: PaymentEntity) = viewModelScope.launch {
        repo.addPayment(p.copy(createdBy = userName)); notify("تم تسجيل الدفعة")
    }

    /* ---------- عمليات ---------- */
    fun addWithdrawal(w: WithdrawalEntity) = viewModelScope.launch {
        repo.addWithdrawal(w.copy(createdBy = userName)); notify("تم تسجيل المسحوب")
    }

    fun addWorkerTx(t: WorkerTxEntity) = viewModelScope.launch {
        repo.addWorkerTx(t); notify("تم تسجيل السحب")
    }

    fun addExpense(e: ExpenseEntity) = viewModelScope.launch {
        repo.addExpense(e.copy(createdBy = userName)); notify("تم تسجيل المصروف")
    }

    fun addClosing(c: ClosingEntity) = viewModelScope.launch {
        repo.addClosing(c.copy(createdBy = userName)); notify("تم حفظ إقفال الصندوق")
    }

    fun savePreorder(p: PreorderEntity) = viewModelScope.launch {
        repo.savePreorder(p, userName); notify("تم حفظ الطلب")
    }

    fun addStocktake(item: ItemEntity, countedBase: Long, note: String, date: String) = viewModelScope.launch {
        repo.addStocktake(
            com.alharamayn.mahamess.data.StocktakeEntity(
                date = date, itemId = item.id, systemQty = item.stock, countedQty = countedBase,
                diff = countedBase - item.stock, note = note, createdBy = userName,
            ),
            countedBase,
        )
        notify("تم تسجيل الجرد")
    }

    fun addRoast(rawId: Long, roastedId: Long, rawQty: Long, roastedQty: Long, date: String, notes: String) =
        viewModelScope.launch {
            repo.addRoast(rawId, roastedId, rawQty, roastedQty, date, notes, userName)
            notify("تم تسجيل عملية التحميص")
        }

    /* ---------- مستخدمون ---------- */
    fun saveUser(u: UserEntity) = viewModelScope.launch {
        repo.saveUser(u); notify("تم حفظ المستخدم")
    }

    fun loadDemo() = viewModelScope.launch { repo.loadDemo(); notify("تم تحميل البيانات التجريبية") }

    /* ---------- نسخ احتياطي ---------- */
    fun exportBackup(onReady: (String) -> Unit) = viewModelScope.launch {
        val json = repo.exportJson()
        repo.settingsStore.update { it.copy(lastBackup = System.currentTimeMillis()) }
        onReady(json)
    }

    fun importBackup(text: String, done: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            repo.importJson(text)
            notify("تمت استعادة البيانات")
            done(true)
        } catch (e: Exception) {
            notify("ملف غير صالح")
            done(false)
        }
    }
}
