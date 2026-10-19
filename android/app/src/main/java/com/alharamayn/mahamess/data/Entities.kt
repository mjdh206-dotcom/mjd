package com.alharamayn.mahamess.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/* ============ الأصناف ============ */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,          // nuts | hospitality | candy | coffee
    val unit: String,              // kg | piece
    val retail: Long,              // سعر المفرق (لكل كغ أو قطعة) بالوحدة الصغرى
    val wholesale: Long,
    val cost: Long,
    val stock: Long,               // غرام للأوزان، قطعة للعدد
    val minStock: Long,
    val barcode: String = "",
    val expiry: String? = null,
    val isBasket: Boolean = false,
    val componentsJson: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: String = "credit",   // cash | credit
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val salary: Long,
    val currency: String = "SYP",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/* ============ الفواتير ============ */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val no: String,
    val type: String,              // sale | purchase
    val date: String,              // yyyy-MM-dd
    val partyId: Long,
    val partyName: String,
    val currency: String,          // SYP | USD
    val rate: Long,                // سعر الصرف ×100
    val subtotal: Long,
    val discount: Long,
    val total: Long,
    val cost: Long,
    val profit: Long,
    val paid: Long,
    val paidCurrency: String,
    val paidForeign: Long = 0,
    val remaining: Long,
    val change: Long = 0,
    val payMethod: String = "cash", // cash | debt | partial
    val status: String = "completed",
    val cancelReason: String? = null,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [ForeignKey(
        entity = InvoiceEntity::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("invoiceId"), Index("itemId")],
)
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val itemId: Long,
    val name: String,
    val unit: String,              // kg | g | oz | piece
    val qtyMilli: Long,            // الكمية ×1000
    val unitPrice: Long,           // سعر الوحدة المختارة
    val discount: Long = 0,
    val costPerBase: Long = 0,     // التكلفة لكل غرام/قطعة
    val baseQty: Long = 0,         // الكمية بالغرام/القطعة
    val isBasket: Boolean = false,
)

data class InvoiceWithLines(
    @androidx.room.Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val lines: List<InvoiceLineEntity>,
)

/* ============ الدفعات والمسحوبات ============ */
@Entity(tableName = "payments", indices = [Index("partyId")])
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyType: String,         // customer | supplier
    val partyId: Long,
    val date: String,
    val currency: String,
    val amount: Long,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amount: Long,
    val currency: String,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "worker_tx", indices = [Index("workerId")])
data class WorkerTxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val date: String,
    val type: String,              // advance | goods
    val amount: Long,
    val currency: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val amount: Long,
    val currency: String,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "closings")
data class ClosingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val currency: String,
    val opening: Long,
    val systemCash: Long,
    val actualCash: Long,
    val diff: Long,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "preorders")
data class PreorderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val no: String,
    val customerName: String,
    val phone: String = "",
    val date: String,
    val deliveryDate: String,
    val total: Long,
    val deposit: Long,
    val currency: String,
    val notes: String = "",
    val status: String = "pending", // pending | ready | delivered | cancelled
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "stock_moves")
data class StockMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val itemId: Long,
    val qtyBase: Long,
    val type: String,
    val note: String = "",
    val refId: String = "",
)

@Entity(tableName = "roasts")
data class RoastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val rawItemId: Long,
    val roastedItemId: Long,
    val rawQtyBase: Long,
    val roastedQtyBase: Long,
    val loss: Long,
    val lossPct: Double,
    val rawCost: Long,
    val actualCostPerKg: Long,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "stocktakes")
data class StocktakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val itemId: Long,
    val systemQty: Long,
    val countedQty: Long,
    val diff: Long,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val user: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String = "",
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,              // manager | worker
    val pin: String,
    val active: Boolean = true,
)
