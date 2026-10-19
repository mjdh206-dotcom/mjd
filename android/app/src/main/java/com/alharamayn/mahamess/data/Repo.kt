package com.alharamayn.mahamess.data

import android.content.Context
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class Repo(context: Context, val settingsStore: SettingsStore) {

    val dao: AppDao = AppDatabase.get(context).dao()

    /* ================= التهيئة ================= */
    suspend fun seedIfNeeded() {
        val existing = dao.dumpItems()
        if (existing.isNotEmpty()) return
        dao.insertCustomer(CustomerEntity(id = 1, name = "زبون نقدي", type = "cash"))
        if (dao.dumpUsers().isEmpty()) {
            dao.insertUser(UserEntity(name = "المدير", role = "manager", pin = "1234"))
            dao.insertUser(UserEntity(name = "عامل المحل", role = "worker", pin = "1111"))
        }
        val base = listOf(
            ItemEntity(name = "فستق حلبي", category = "nuts", unit = "kg", retail = 250000, wholesale = 235000, cost = 205000, stock = 15000, minStock = 3000),
            ItemEntity(name = "لوز مقشر", category = "nuts", unit = "kg", retail = 120000, wholesale = 112000, cost = 96000, stock = 20000, minStock = 4000),
            ItemEntity(name = "كاجو", category = "nuts", unit = "kg", retail = 180000, wholesale = 170000, cost = 149000, stock = 10000, minStock = 3000),
            ItemEntity(name = "بندق محمص", category = "nuts", unit = "kg", retail = 110000, wholesale = 103000, cost = 88000, stock = 12000, minStock = 3000),
            ItemEntity(name = "جوز مجروش", category = "nuts", unit = "kg", retail = 95000, wholesale = 89000, cost = 76000, stock = 8000, minStock = 2000),
            ItemEntity(name = "تمر مجدول", category = "nuts", unit = "kg", retail = 45000, wholesale = 42000, cost = 34000, stock = 25000, minStock = 5000),
            ItemEntity(name = "شوكولا مشكل", category = "candy", unit = "kg", retail = 85000, wholesale = 79000, cost = 64000, stock = 9000, minStock = 2500),
            ItemEntity(name = "كاندي ملون", category = "candy", unit = "kg", retail = 40000, wholesale = 36000, cost = 27000, stock = 12000, minStock = 3000),
            ItemEntity(name = "ملبس لوز", category = "candy", unit = "kg", retail = 62000, wholesale = 58000, cost = 47000, stock = 5000, minStock = 1500),
            ItemEntity(name = "قهوة عربية بالهيل", category = "coffee", unit = "kg", retail = 90000, wholesale = 84000, cost = 68000, stock = 9000, minStock = 2000),
            ItemEntity(name = "قهوة تركية محمصة", category = "coffee", unit = "kg", retail = 120000, wholesale = 112000, cost = 88000, stock = 6000, minStock = 2000),
            ItemEntity(name = "بن برازيلي خام", category = "coffee", unit = "kg", retail = 0, wholesale = 0, cost = 52000, stock = 20000, minStock = 5000),
            ItemEntity(name = "علبة ضيافة صغيرة", category = "hospitality", unit = "piece", retail = 35000, wholesale = 32000, cost = 26000, stock = 100, minStock = 20),
            ItemEntity(name = "علبة ضيافة كبيرة", category = "hospitality", unit = "piece", retail = 90000, wholesale = 85000, cost = 70000, stock = 50, minStock = 10),
        )
        base.forEach { dao.insertItem(it) }
        dao.insertSupplier(SupplierEntity(name = "مؤسسة المكسرات السورية", phone = "0931112222"))
        dao.insertSupplier(SupplierEntity(name = "شركة البن البرازيلي", phone = "0942223333"))
        dao.insertWorker(WorkerEntity(name = "أحمد شحادة", phone = "0999888777", salary = 3500000))
    }

    /** تحميل بيانات تجريبية كاملة (اختياري من الإعدادات) */
    suspend fun loadDemo() {
        seedIfNeeded()
        val items = dao.dumpItems()
        if (items.isEmpty()) return
        val cust = dao.insertCustomer(CustomerEntity(name = "أبو محمد الخضري", phone = "0932445566", type = "credit"))
        val cust2 = dao.insertCustomer(CustomerEntity(name = "مطعم الشام", phone = "0944556677", type = "credit"))
        val sup = dao.dumpSuppliers().firstOrNull()?.id ?: 1L
        val rate = 1250000L
        val today = Dates.today()
        for (i in 0 until 6) {
            val item = items[i % items.size]
            val draft = InvoiceDraft(
                type = "sale",
                date = Dates.addDays(-i),
                partyId = if (i % 2 == 0) cust else cust2,
                partyName = if (i % 2 == 0) "أبو محمد الخضري" else "مطعم الشام",
                currency = if (i % 3 == 0) "USD" else "SYP",
                rate = rate,
                lines = listOf(
                    DraftLine(
                        itemId = item.id, name = item.name, unit = item.unit,
                        qtyMilli = if (item.unit == "kg") 2000 else 2000,
                        unitPrice = if (item.unit == "kg") item.retail else item.retail,
                        costPerBase = item.cost,
                        baseQty = if (item.unit == "kg") 2000 else 2,
                    ),
                ),
                discountType = "none", discountValue = 0,
                payMethod = if (i % 2 == 0) "cash" else "debt",
                paidCurrency = "SYP", paidAmount = 0,
                notes = "", createdBy = "المدير",
            )
            createInvoice(draft)
        }
        // فاتورة شراء
        createInvoice(
            InvoiceDraft(
                type = "purchase", date = today, partyId = sup,
                partyName = dao.dumpSuppliers().firstOrNull()?.name ?: "مورد",
                currency = "SYP", rate = rate,
                lines = listOf(DraftLine(items[0].id, items[0].name, "kg", 10000, items[0].cost, 0, items[0].cost, 10000)),
                payMethod = "cash", paidCurrency = "SYP", paidAmount = 0, createdBy = "المدير",
            ),
        )
        addExpense(ExpenseEntity(date = today, category = "كهرباء", amount = 850000, currency = "SYP", createdBy = "المدير"))
        addWithdrawal(WithdrawalEntity(date = today, amount = 500000, currency = "SYP", note = "مصروف شخصي", createdBy = "المدير"))
        addWorkerTx(WorkerTxEntity(workerId = dao.dumpWorkers().firstOrNull()?.id ?: 1L, date = today, type = "advance", amount = 300000, currency = "SYP", note = "سلفة"))
    }

    /* ================= الأصناف ================= */
    suspend fun saveItem(item: ItemEntity, user: String): Long {
        val id = if (item.id == 0L) dao.insertItem(item) else { dao.updateItem(item); item.id }
        audit(user, if (item.id == 0L) "إنشاء" else "تعديل", "صنف", item.name, "حفظ بيانات الصنف")
        return id
    }

    suspend fun deleteItem(item: ItemEntity, user: String) {
        dao.deleteItem(item.id)
        audit(user, "حذف", "صنف", item.name, "حذف الصنف")
    }

    /* ================= الفواتير ================= */
    suspend fun createInvoice(draft: InvoiceDraft): Long {
        val totals = Calc.totals(draft.lines, draft.discountType, draft.discountValue)
        val prefix = if (draft.type == "sale") "S" else "P"
        val lastNumber = dao.dumpInvoices()
            .filter { it.type == draft.type }
            .mapNotNull { it.no.substringAfter("-", "").toIntOrNull() }
            .maxOrNull() ?: 0
        val no = "$prefix-" + (lastNumber + 1).toString().padStart(4, '0')
        val paid = when (draft.payMethod) {
            "cash" -> totals.total
            "debt" -> 0L
            else -> Money.convert(draft.paidAmount, draft.paidCurrency, draft.currency, draft.rate)
        }
        val inv = InvoiceEntity(
            no = no,
            type = draft.type,
            date = draft.date,
            partyId = draft.partyId,
            partyName = draft.partyName,
            currency = draft.currency,
            rate = draft.rate,
            subtotal = totals.subtotal,
            discount = totals.discount,
            total = totals.total,
            cost = totals.cost,
            profit = totals.profit,
            paid = paid,
            paidCurrency = draft.paidCurrency,
            paidForeign = if (draft.paidCurrency != draft.currency) draft.paidAmount else 0,
            remaining = (totals.total - paid).coerceAtLeast(0),
            change = (paid - totals.total).coerceAtLeast(0),
            payMethod = draft.payMethod,
            status = "completed",
            notes = draft.notes,
            createdBy = draft.createdBy,
        )
        val id = dao.insertInvoice(inv)
        dao.insertLines(draft.lines.map {
            InvoiceLineEntity(
                invoiceId = id, itemId = it.itemId, name = it.name, unit = it.unit,
                qtyMilli = it.qtyMilli, unitPrice = it.unitPrice, discount = it.discount,
                costPerBase = it.costPerBase, baseQty = it.baseQty, isBasket = it.isBasket,
            )
        })
        applyStock(draft, no, -1)
        audit(draft.createdBy, "إنشاء", "فاتورة", no, "${if (draft.type == "sale") "فاتورة مبيعات" else "فاتورة شراء"} بقيمة ${Money.format(totals.total, draft.currency)} — ${draft.partyName}")
        return id
    }

    private suspend fun applyStock(draft: InvoiceDraft, ref: String, signOverride: Int? = null) {
        val sign = signOverride ?: if (draft.type == "sale") -1 else 1
        for (line in draft.lines) {
            val item = dao.item(line.itemId) ?: continue
            var newStock = (item.stock + sign * line.baseQty).coerceAtLeast(0)
            var newCost = item.cost
            if (draft.type == "purchase") {
                val factor = Calc.unitFactor(line.unit)
                val costPerBase = Math.round(line.unitPrice / factor.toDouble())
                val totalCost = item.stock * item.cost + line.baseQty * costPerBase
                newCost = if (newStock > 0) Math.round(totalCost.toDouble() / newStock) else costPerBase
            }
            dao.updateItem(item.copy(stock = newStock, cost = newCost))
            dao.insertStockMove(
                StockMoveEntity(
                    itemId = item.id, qtyBase = sign * line.baseQty,
                    type = draft.type, note = "فاتورة $ref", refId = ref,
                )
            )
            // خصم مكونات السلة عند البيع
            if (draft.type == "sale" && line.isBasket) {
                val basket = dao.item(line.itemId)
                parseComponents(basket?.componentsJson ?: "").forEach { (compId, qtyBase) ->
                    val comp = dao.item(compId) ?: return@forEach
                    val times = line.qtyMilli / 1000.0
                    dao.updateItem(comp.copy(stock = (comp.stock - Math.round(qtyBase * times)).coerceAtLeast(0)))
                }
            }
        }
    }

    suspend fun cancelInvoice(invoice: InvoiceWithLines, reason: String, user: String) {
        if (invoice.invoice.status == "cancelled") return
        val inv = invoice.invoice
        val sign = if (inv.type == "sale") 1 else -1
        invoice.lines.forEach { line ->
            val item = dao.item(line.itemId) ?: return@forEach
            dao.updateItem(item.copy(stock = (item.stock + sign * line.baseQty).coerceAtLeast(0)))
        }
        dao.updateInvoice(
            inv.copy(status = "cancelled", cancelReason = reason, updatedAt = System.currentTimeMillis())
        )
        audit(user, "إلغاء", "فاتورة", inv.no, "سبب الإلغاء: ${reason.ifBlank { "غير محدد" }}")
    }

    suspend fun updateInvoice(invoice: InvoiceWithLines, draft: InvoiceDraft, user: String) {
        val old = invoice.invoice
        // إرجاع مخزون الفاتورة القديمة
        val restoreSign = if (old.type == "sale") 1 else -1
        invoice.lines.forEach { line ->
            val item = dao.item(line.itemId) ?: return@forEach
            dao.updateItem(item.copy(stock = (item.stock + restoreSign * line.baseQty).coerceAtLeast(0)))
        }
        dao.deleteLines(old.id)
        val totals = Calc.totals(draft.lines, draft.discountType, draft.discountValue)
        val paid = when (draft.payMethod) {
            "cash" -> totals.total
            "debt" -> 0L
            else -> Money.convert(draft.paidAmount, draft.paidCurrency, draft.currency, draft.rate)
        }
        dao.updateInvoice(
            old.copy(
                date = draft.date, partyId = draft.partyId, partyName = draft.partyName,
                currency = draft.currency, rate = draft.rate,
                subtotal = totals.subtotal, discount = totals.discount, total = totals.total,
                cost = totals.cost, profit = totals.profit, paid = paid,
                paidCurrency = draft.paidCurrency,
                paidForeign = if (draft.paidCurrency != draft.currency) draft.paidAmount else 0,
                remaining = (totals.total - paid).coerceAtLeast(0),
                change = (paid - totals.total).coerceAtLeast(0),
                payMethod = draft.payMethod, notes = draft.notes,
                updatedAt = System.currentTimeMillis(),
            )
        )
        dao.insertLines(draft.lines.map {
            InvoiceLineEntity(
                invoiceId = old.id, itemId = it.itemId, name = it.name, unit = it.unit,
                qtyMilli = it.qtyMilli, unitPrice = it.unitPrice, discount = it.discount,
                costPerBase = it.costPerBase, baseQty = it.baseQty, isBasket = it.isBasket,
            )
        })
        applyStock(draft, old.no, if (old.type == "sale") -1 else 1)
        audit(user, "تعديل", "فاتورة", old.no, "تعديل الفاتورة — الإجمالي الجديد ${Money.format(totals.total, draft.currency)}")
    }

    /* ================= العمليات المالية ================= */
    suspend fun addPayment(p: PaymentEntity) {
        dao.insertPayment(p)
        audit(p.createdBy, "إنشاء", "دفعة", p.partyId.toString(), "دفعة ${Money.format(p.amount, p.currency)}")
    }

    suspend fun addWithdrawal(w: WithdrawalEntity) {
        dao.insertWithdrawal(w)
        audit(w.createdBy, "إنشاء", "مسحوب", w.id.toString(), "${Money.format(w.amount, w.currency)} — ${w.note}")
    }

    suspend fun addWorkerTx(t: WorkerTxEntity) {
        dao.insertWorkerTx(t)
        audit("المدير", "إنشاء", "سحب عامل", t.workerId.toString(), "${Money.format(t.amount, t.currency)} — ${if (t.type == "advance") "سلفة" else "بضاعة"}")
    }

    suspend fun addExpense(e: ExpenseEntity) {
        dao.insertExpense(e)
        audit(e.createdBy, "إنشاء", "مصروف", e.category, Money.format(e.amount, e.currency))
    }

    suspend fun addClosing(c: ClosingEntity) {
        dao.insertClosing(c)
        audit(c.createdBy, "إنشاء", "إقفال صندوق", c.date, "الفرق ${Money.format(c.diff, c.currency)}")
    }

    suspend fun savePreorder(p: PreorderEntity, user: String) {
        dao.insertPreorder(p)
        audit(user, if (p.id == 0L) "إنشاء" else "تعديل", "طلب مسبق", p.no, p.customerName)
    }

    suspend fun addStocktake(s: StocktakeEntity, countedBase: Long) {
        val item = dao.item(s.itemId) ?: return
        dao.updateItem(item.copy(stock = countedBase))
        dao.insertStocktake(s.copy(countedQty = countedBase, diff = countedBase - item.stock, systemQty = item.stock))
        dao.insertStockMove(StockMoveEntity(itemId = item.id, qtyBase = countedBase - item.stock, type = "stocktake", note = s.note.ifBlank { "جرد يدوي" }))
        audit(s.createdBy, "جرد", "مخزون", item.name, "الفرق ${countedBase - item.stock}")
    }

    suspend fun addRoast(
        rawItemId: Long, roastedItemId: Long, rawQty: Long, roastedQty: Long,
        date: String, notes: String, user: String,
    ) {
        val raw = dao.item(rawItemId) ?: return
        val roasted = dao.item(roastedItemId) ?: return
        val loss = rawQty - roastedQty
        val rawCost = raw.cost * rawQty
        val costPerKg = if (roastedQty > 0) Math.round(rawCost * 1000.0 / roastedQty) else 0L
        dao.updateItem(raw.copy(stock = (raw.stock - rawQty).coerceAtLeast(0)))
        dao.updateItem(roasted.copy(stock = roasted.stock + roastedQty, cost = costPerKg))
        dao.insertRoast(
            RoastEntity(
                date = date, rawItemId = rawItemId, roastedItemId = roastedItemId,
                rawQtyBase = rawQty, roastedQtyBase = roastedQty, loss = loss,
                lossPct = if (rawQty > 0) loss * 100.0 / rawQty else 0.0,
                rawCost = rawCost, actualCostPerKg = costPerKg, notes = notes, createdBy = user,
            )
        )
        audit(user, "تحميص", "مخزون", roasted.name, "الفاقد $loss غ — التكلفة الفعلية $costPerKg/كغ")
    }

    /* ================= المستخدمون والسجل ================= */
    suspend fun verifyPin(pin: String): UserEntity? = dao.userByPin(pin)

    suspend fun saveUser(u: UserEntity) {
        dao.insertUser(u)
        audit("المدير", if (u.id == 0L) "إنشاء" else "تعديل", "مستخدم", u.name, if (u.role == "manager") "مدير" else "عامل")
    }

    suspend fun audit(user: String, action: String, entity: String, entityId: String, details: String) {
        dao.insertAudit(AuditEntity(user = user, action = action, entity = entity, entityId = entityId, details = details))
    }

    /* ================= السلال ================= */
    fun parseComponents(json: String): List<Pair<Long, Long>> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.getLong("itemId") to o.getLong("qtyBase")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun componentsJson(components: List<Pair<Long, Long>>): String {
        val arr = JSONArray()
        components.forEach {
            arr.put(JSONObject().put("itemId", it.first).put("qtyBase", it.second))
        }
        return arr.toString()
    }

    /* ================= النسخ الاحتياطي ================= */
    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "mahamess-alharamayn")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("items", JSONArray().apply { dao.dumpItems().forEach { put(itemJson(it)) } })
        root.put("customers", JSONArray().apply { dao.dumpCustomers().forEach { put(customerJson(it)) } })
        root.put("suppliers", JSONArray().apply { dao.dumpSuppliers().forEach { put(supplierJson(it)) } })
        root.put("workers", JSONArray().apply { dao.dumpWorkers().forEach { put(workerJson(it)) } })
        root.put("invoices", JSONArray().apply { dao.dumpInvoices().forEach { put(invoiceJson(it)) } })
        root.put("lines", JSONArray().apply { dao.dumpLines().forEach { put(lineJson(it)) } })
        root.put("payments", JSONArray().apply { dao.dumpPayments().forEach { put(paymentJson(it)) } })
        root.put("withdrawals", JSONArray().apply { dao.dumpWithdrawals().forEach { put(withdrawalJson(it)) } })
        root.put("workerTx", JSONArray().apply { dao.dumpWorkerTx().forEach { put(workerTxJson(it)) } })
        root.put("expenses", JSONArray().apply { dao.dumpExpenses().forEach { put(expenseJson(it)) } })
        root.put("closings", JSONArray().apply { dao.dumpClosings().forEach { put(closingJson(it)) } })
        root.put("preorders", JSONArray().apply { dao.dumpPreorders().forEach { put(preorderJson(it)) } })
        root.put("roasts", JSONArray().apply { dao.dumpRoasts().forEach { put(roastJson(it)) } })
        return root.toString(2)
    }

    suspend fun importJson(text: String) {
        val root = JSONObject(text)
        dao.wipeLines(); dao.wipeInvoices(); dao.wipeItems(); dao.wipeCustomers()
        dao.wipeSuppliers(); dao.wipeWorkers(); dao.wipePayments(); dao.wipeWithdrawals()
        dao.wipeWorkerTx(); dao.wipeExpenses(); dao.wipeClosings(); dao.wipePreorders(); dao.wipeRoasts()

        root.optJSONArray("items")?.let { arr ->
            dao.restoreItems((0 until arr.length()).map { readItem(arr.getJSONObject(it)) })
        }
        root.optJSONArray("customers")?.let { arr ->
            dao.restoreCustomers((0 until arr.length()).map { readCustomer(arr.getJSONObject(it)) })
        }
        root.optJSONArray("suppliers")?.let { arr ->
            dao.restoreSuppliers((0 until arr.length()).map { readSupplier(arr.getJSONObject(it)) })
        }
        root.optJSONArray("workers")?.let { arr ->
            dao.restoreWorkers((0 until arr.length()).map { readWorker(arr.getJSONObject(it)) })
        }
        root.optJSONArray("invoices")?.let { arr ->
            dao.restoreInvoices((0 until arr.length()).map { readInvoice(arr.getJSONObject(it)) })
        }
        root.optJSONArray("lines")?.let { arr ->
            dao.restoreLines((0 until arr.length()).map { readLine(arr.getJSONObject(it)) })
        }
        root.optJSONArray("payments")?.let { arr ->
            dao.restorePayments((0 until arr.length()).map { readPayment(arr.getJSONObject(it)) })
        }
        root.optJSONArray("withdrawals")?.let { arr ->
            dao.restoreWithdrawals((0 until arr.length()).map { readWithdrawal(arr.getJSONObject(it)) })
        }
        root.optJSONArray("workerTx")?.let { arr ->
            dao.restoreWorkerTx((0 until arr.length()).map { readWorkerTx(arr.getJSONObject(it)) })
        }
        root.optJSONArray("expenses")?.let { arr ->
            dao.restoreExpenses((0 until arr.length()).map { readExpense(arr.getJSONObject(it)) })
        }
        root.optJSONArray("closings")?.let { arr ->
            dao.restoreClosings((0 until arr.length()).map { readClosing(arr.getJSONObject(it)) })
        }
        root.optJSONArray("preorders")?.let { arr ->
            dao.restorePreorders((0 until arr.length()).map { readPreorder(arr.getJSONObject(it)) })
        }
        root.optJSONArray("roasts")?.let { arr ->
            dao.restoreRoasts((0 until arr.length()).map { readRoast(arr.getJSONObject(it)) })
        }
        audit("المدير", "استعادة", "نسخة احتياطية", "", "تمت استعادة البيانات")
    }

    /* ---- JSON helpers ---- */
    private fun itemJson(i: ItemEntity) = JSONObject()
        .put("id", i.id).put("name", i.name).put("category", i.category).put("unit", i.unit)
        .put("retail", i.retail).put("wholesale", i.wholesale).put("cost", i.cost)
        .put("stock", i.stock).put("minStock", i.minStock).put("barcode", i.barcode)
        .put("expiry", i.expiry ?: "").put("isBasket", i.isBasket)
        .put("components", i.componentsJson).put("active", i.active).put("createdAt", i.createdAt)

    private fun readItem(o: JSONObject) = ItemEntity(
        id = o.getLong("id"), name = o.getString("name"), category = o.getString("category"),
        unit = o.getString("unit"), retail = o.getLong("retail"), wholesale = o.getLong("wholesale"),
        cost = o.getLong("cost"), stock = o.getLong("stock"), minStock = o.getLong("minStock"),
        barcode = o.optString("barcode"), expiry = o.optString("expiry").ifBlank { null },
        isBasket = o.optBoolean("isBasket"), componentsJson = o.optString("components"),
        active = o.optBoolean("active", true), createdAt = o.optLong("createdAt"),
    )

    private fun customerJson(c: CustomerEntity) = JSONObject()
        .put("id", c.id).put("name", c.name).put("phone", c.phone).put("type", c.type)
        .put("notes", c.notes).put("active", c.active).put("createdAt", c.createdAt)

    private fun readCustomer(o: JSONObject) = CustomerEntity(
        id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone"),
        type = o.optString("type", "credit"), notes = o.optString("notes"),
        active = o.optBoolean("active", true), createdAt = o.optLong("createdAt"),
    )

    private fun supplierJson(s: SupplierEntity) = JSONObject()
        .put("id", s.id).put("name", s.name).put("phone", s.phone).put("notes", s.notes)
        .put("active", s.active).put("createdAt", s.createdAt)

    private fun readSupplier(o: JSONObject) = SupplierEntity(
        id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone"),
        notes = o.optString("notes"), active = o.optBoolean("active", true), createdAt = o.optLong("createdAt"),
    )

    private fun workerJson(w: WorkerEntity) = JSONObject()
        .put("id", w.id).put("name", w.name).put("phone", w.phone).put("salary", w.salary)
        .put("currency", w.currency).put("active", w.active).put("createdAt", w.createdAt)

    private fun readWorker(o: JSONObject) = WorkerEntity(
        id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone"),
        salary = o.getLong("salary"), currency = o.optString("currency", "SYP"),
        active = o.optBoolean("active", true), createdAt = o.optLong("createdAt"),
    )

    private fun invoiceJson(i: InvoiceEntity) = JSONObject()
        .put("id", i.id).put("no", i.no).put("type", i.type).put("date", i.date)
        .put("partyId", i.partyId).put("partyName", i.partyName).put("currency", i.currency)
        .put("rate", i.rate).put("subtotal", i.subtotal).put("discount", i.discount)
        .put("total", i.total).put("cost", i.cost).put("profit", i.profit).put("paid", i.paid)
        .put("paidCurrency", i.paidCurrency).put("paidForeign", i.paidForeign).put("remaining", i.remaining)
        .put("change", i.change).put("payMethod", i.payMethod).put("status", i.status)
        .put("cancelReason", i.cancelReason ?: "").put("notes", i.notes)
        .put("createdBy", i.createdBy).put("createdAt", i.createdAt).put("updatedAt", i.updatedAt)

    private fun readInvoice(o: JSONObject) = InvoiceEntity(
        id = o.getLong("id"), no = o.getString("no"), type = o.getString("type"),
        date = o.getString("date"), partyId = o.getLong("partyId"), partyName = o.getString("partyName"),
        currency = o.getString("currency"), rate = o.getLong("rate"), subtotal = o.getLong("subtotal"),
        discount = o.getLong("discount"), total = o.getLong("total"), cost = o.getLong("cost"),
        profit = o.getLong("profit"), paid = o.getLong("paid"), paidCurrency = o.optString("paidCurrency", "SYP"),
        paidForeign = o.optLong("paidForeign"), remaining = o.getLong("remaining"), change = o.optLong("change"),
        payMethod = o.optString("payMethod", "cash"), status = o.optString("status", "completed"),
        cancelReason = o.optString("cancelReason").ifBlank { null }, notes = o.optString("notes"),
        createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
    )

    private fun lineJson(l: InvoiceLineEntity) = JSONObject()
        .put("id", l.id).put("invoiceId", l.invoiceId).put("itemId", l.itemId).put("name", l.name)
        .put("unit", l.unit).put("qtyMilli", l.qtyMilli).put("unitPrice", l.unitPrice)
        .put("discount", l.discount).put("costPerBase", l.costPerBase).put("baseQty", l.baseQty)
        .put("isBasket", l.isBasket)

    private fun readLine(o: JSONObject) = InvoiceLineEntity(
        id = o.getLong("id"), invoiceId = o.getLong("invoiceId"), itemId = o.getLong("itemId"),
        name = o.getString("name"), unit = o.getString("unit"), qtyMilli = o.getLong("qtyMilli"),
        unitPrice = o.getLong("unitPrice"), discount = o.optLong("discount"),
        costPerBase = o.optLong("costPerBase"), baseQty = o.optLong("baseQty"), isBasket = o.optBoolean("isBasket"),
    )

    private fun paymentJson(p: PaymentEntity) = JSONObject()
        .put("id", p.id).put("partyType", p.partyType).put("partyId", p.partyId).put("date", p.date)
        .put("currency", p.currency).put("amount", p.amount).put("note", p.note)
        .put("createdBy", p.createdBy).put("createdAt", p.createdAt)

    private fun readPayment(o: JSONObject) = PaymentEntity(
        id = o.getLong("id"), partyType = o.getString("partyType"), partyId = o.getLong("partyId"),
        date = o.getString("date"), currency = o.getString("currency"), amount = o.getLong("amount"),
        note = o.optString("note"), createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"),
    )

    private fun withdrawalJson(w: WithdrawalEntity) = JSONObject()
        .put("id", w.id).put("date", w.date).put("amount", w.amount).put("currency", w.currency)
        .put("note", w.note).put("createdBy", w.createdBy).put("createdAt", w.createdAt)

    private fun readWithdrawal(o: JSONObject) = WithdrawalEntity(
        id = o.getLong("id"), date = o.getString("date"), amount = o.getLong("amount"),
        currency = o.getString("currency"), note = o.optString("note"),
        createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"),
    )

    private fun workerTxJson(t: WorkerTxEntity) = JSONObject()
        .put("id", t.id).put("workerId", t.workerId).put("date", t.date).put("type", t.type)
        .put("amount", t.amount).put("currency", t.currency).put("note", t.note).put("createdAt", t.createdAt)

    private fun readWorkerTx(o: JSONObject) = WorkerTxEntity(
        id = o.getLong("id"), workerId = o.getLong("workerId"), date = o.getString("date"),
        type = o.getString("type"), amount = o.getLong("amount"), currency = o.getString("currency"),
        note = o.optString("note"), createdAt = o.optLong("createdAt"),
    )

    private fun expenseJson(e: ExpenseEntity) = JSONObject()
        .put("id", e.id).put("date", e.date).put("category", e.category).put("amount", e.amount)
        .put("currency", e.currency).put("note", e.note).put("createdBy", e.createdBy).put("createdAt", e.createdAt)

    private fun readExpense(o: JSONObject) = ExpenseEntity(
        id = o.getLong("id"), date = o.getString("date"), category = o.getString("category"),
        amount = o.getLong("amount"), currency = o.getString("currency"), note = o.optString("note"),
        createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"),
    )

    private fun closingJson(c: ClosingEntity) = JSONObject()
        .put("id", c.id).put("date", c.date).put("currency", c.currency).put("opening", c.opening)
        .put("systemCash", c.systemCash).put("actualCash", c.actualCash).put("diff", c.diff)
        .put("note", c.note).put("createdBy", c.createdBy).put("createdAt", c.createdAt)

    private fun readClosing(o: JSONObject) = ClosingEntity(
        id = o.getLong("id"), date = o.getString("date"), currency = o.getString("currency"),
        opening = o.getLong("opening"), systemCash = o.getLong("systemCash"),
        actualCash = o.getLong("actualCash"), diff = o.getLong("diff"), note = o.optString("note"),
        createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"),
    )

    private fun preorderJson(p: PreorderEntity) = JSONObject()
        .put("id", p.id).put("no", p.no).put("customerName", p.customerName).put("phone", p.phone)
        .put("date", p.date).put("deliveryDate", p.deliveryDate).put("total", p.total)
        .put("deposit", p.deposit).put("currency", p.currency).put("notes", p.notes)
        .put("status", p.status).put("createdAt", p.createdAt)

    private fun readPreorder(o: JSONObject) = PreorderEntity(
        id = o.getLong("id"), no = o.getString("no"), customerName = o.getString("customerName"),
        phone = o.optString("phone"), date = o.getString("date"), deliveryDate = o.getString("deliveryDate"),
        total = o.getLong("total"), deposit = o.getLong("deposit"), currency = o.getString("currency"),
        notes = o.optString("notes"), status = o.optString("status", "pending"), createdAt = o.optLong("createdAt"),
    )

    private fun roastJson(r: RoastEntity) = JSONObject()
        .put("id", r.id).put("date", r.date).put("rawItemId", r.rawItemId)
        .put("roastedItemId", r.roastedItemId).put("rawQtyBase", r.rawQtyBase)
        .put("roastedQtyBase", r.roastedQtyBase).put("loss", r.loss).put("lossPct", r.lossPct)
        .put("rawCost", r.rawCost).put("actualCostPerKg", r.actualCostPerKg)
        .put("notes", r.notes).put("createdBy", r.createdBy).put("createdAt", r.createdAt)

    private fun readRoast(o: JSONObject) = RoastEntity(
        id = o.getLong("id"), date = o.getString("date"), rawItemId = o.getLong("rawItemId"),
        roastedItemId = o.getLong("roastedItemId"), rawQtyBase = o.getLong("rawQtyBase"),
        roastedQtyBase = o.getLong("roastedQtyBase"), loss = o.getLong("loss"),
        lossPct = o.optDouble("lossPct"), rawCost = o.getLong("rawCost"),
        actualCostPerKg = o.getLong("actualCostPerKg"), notes = o.optString("notes"),
        createdBy = o.optString("createdBy"), createdAt = o.optLong("createdAt"),
    )
}

data class InvoiceDraft(
    val type: String,
    val date: String,
    val partyId: Long,
    val partyName: String,
    val currency: String,
    val rate: Long,
    val lines: List<DraftLine>,
    val discountType: String = "none",
    val discountValue: Long = 0,
    val payMethod: String = "cash",
    val paidCurrency: String = "SYP",
    val paidAmount: Long = 0,
    val notes: String = "",
    val createdBy: String = "المدير",
)
