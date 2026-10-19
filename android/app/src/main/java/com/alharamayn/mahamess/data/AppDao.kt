package com.alharamayn.mahamess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    /* الأصناف */
    @Query("SELECT * FROM items WHERE active = 1 ORDER BY name")
    fun items(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY name")
    fun allItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun item(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :code LIMIT 1")
    suspend fun itemByBarcode(code: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM items WHERE active = 1 AND minStock > 0 AND stock <= minStock")
    fun lowStock(): Flow<List<ItemEntity>>

    /* الزبائن والموردون */
    @Query("SELECT * FROM customers WHERE active = 1 ORDER BY name")
    fun customers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(c: CustomerEntity): Long

    @Query("SELECT * FROM suppliers WHERE active = 1 ORDER BY name")
    fun suppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(s: SupplierEntity): Long

    @Query("SELECT * FROM workers WHERE active = 1 ORDER BY name")
    fun workers(): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(w: WorkerEntity): Long

    /* الفواتير */
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun invoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY createdAt DESC")
    fun invoicesByType(type: String): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun invoiceWithLines(id: Long): InvoiceWithLines?

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun invoiceWithLinesFlow(id: Long): Flow<InvoiceWithLines?>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun allInvoicesWithLines(): Flow<List<InvoiceWithLines>>

    @Insert
    suspend fun insertInvoice(inv: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(inv: InvoiceEntity)

    @Insert
    suspend fun insertLines(lines: List<InvoiceLineEntity>)

    @Query("DELETE FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun deleteLines(invoiceId: Long)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)

    @Query("SELECT COUNT(*) FROM invoices WHERE type = :type")
    suspend fun countInvoices(type: String): Int

    /* دفعات ومسحوبات */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun payments(): Flow<List<PaymentEntity>>

    @Insert
    suspend fun insertPayment(p: PaymentEntity): Long

    @Query("SELECT * FROM withdrawals ORDER BY date DESC")
    fun withdrawals(): Flow<List<WithdrawalEntity>>

    @Insert
    suspend fun insertWithdrawal(w: WithdrawalEntity): Long

    @Query("SELECT * FROM worker_tx ORDER BY date DESC")
    fun workerTx(): Flow<List<WorkerTxEntity>>

    @Insert
    suspend fun insertWorkerTx(t: WorkerTxEntity): Long

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun expenses(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(e: ExpenseEntity): Long

    @Query("SELECT * FROM closings ORDER BY date DESC")
    fun closings(): Flow<List<ClosingEntity>>

    @Insert
    suspend fun insertClosing(c: ClosingEntity): Long

    @Query("SELECT * FROM preorders ORDER BY createdAt DESC")
    fun preorders(): Flow<List<PreorderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreorder(p: PreorderEntity): Long

    @Query("SELECT * FROM stock_moves ORDER BY at DESC LIMIT 300")
    fun stockMoves(): Flow<List<StockMoveEntity>>

    @Insert
    suspend fun insertStockMove(m: StockMoveEntity)

    @Query("SELECT * FROM roasts ORDER BY createdAt DESC")
    fun roasts(): Flow<List<RoastEntity>>

    @Insert
    suspend fun insertRoast(r: RoastEntity)

    @Query("SELECT * FROM stocktakes ORDER BY createdAt DESC")
    fun stocktakes(): Flow<List<StocktakeEntity>>

    @Insert
    suspend fun insertStocktake(s: StocktakeEntity)

    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT 500")
    fun audit(): Flow<List<AuditEntity>>

    @Insert
    suspend fun insertAudit(a: AuditEntity)

    @Query("SELECT * FROM users ORDER BY role, name")
    fun users(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE pin = :pin AND active = 1 LIMIT 1")
    suspend fun userByPin(pin: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(u: UserEntity): Long

    /* نسخ احتياطي كامل */
    @Query("SELECT * FROM items") suspend fun dumpItems(): List<ItemEntity>
    @Query("SELECT * FROM customers") suspend fun dumpCustomers(): List<CustomerEntity>
    @Query("SELECT * FROM suppliers") suspend fun dumpSuppliers(): List<SupplierEntity>
    @Query("SELECT * FROM workers") suspend fun dumpWorkers(): List<WorkerEntity>
    @Query("SELECT * FROM invoices") suspend fun dumpInvoices(): List<InvoiceEntity>
    @Query("SELECT * FROM invoice_lines") suspend fun dumpLines(): List<InvoiceLineEntity>
    @Query("SELECT * FROM payments") suspend fun dumpPayments(): List<PaymentEntity>
    @Query("SELECT * FROM withdrawals") suspend fun dumpWithdrawals(): List<WithdrawalEntity>
    @Query("SELECT * FROM worker_tx") suspend fun dumpWorkerTx(): List<WorkerTxEntity>
    @Query("SELECT * FROM expenses") suspend fun dumpExpenses(): List<ExpenseEntity>
    @Query("SELECT * FROM closings") suspend fun dumpClosings(): List<ClosingEntity>
    @Query("SELECT * FROM preorders") suspend fun dumpPreorders(): List<PreorderEntity>
    @Query("SELECT * FROM roasts") suspend fun dumpRoasts(): List<RoastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreItems(x: List<ItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCustomers(x: List<CustomerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSuppliers(x: List<SupplierEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWorkers(x: List<WorkerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreInvoices(x: List<InvoiceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreLines(x: List<InvoiceLineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePayments(x: List<PaymentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWithdrawals(x: List<WithdrawalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWorkerTx(x: List<WorkerTxEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreExpenses(x: List<ExpenseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreClosings(x: List<ClosingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePreorders(x: List<PreorderEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRoasts(x: List<RoastEntity>)

    @Query("DELETE FROM items") suspend fun wipeItems()
    @Query("DELETE FROM customers") suspend fun wipeCustomers()
    @Query("DELETE FROM suppliers") suspend fun wipeSuppliers()
    @Query("DELETE FROM workers") suspend fun wipeWorkers()
    @Query("DELETE FROM invoice_lines") suspend fun wipeLines()
    @Query("DELETE FROM invoices") suspend fun wipeInvoices()
    @Query("DELETE FROM payments") suspend fun wipePayments()
    @Query("DELETE FROM withdrawals") suspend fun wipeWithdrawals()
    @Query("DELETE FROM worker_tx") suspend fun wipeWorkerTx()
    @Query("DELETE FROM expenses") suspend fun wipeExpenses()
    @Query("DELETE FROM closings") suspend fun wipeClosings()
    @Query("DELETE FROM preorders") suspend fun wipePreorders()
    @Query("DELETE FROM roasts") suspend fun wipeRoasts()
}
