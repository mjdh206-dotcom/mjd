package com.alharamayn.mahamess.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ItemEntity::class, CustomerEntity::class, SupplierEntity::class, WorkerEntity::class,
        InvoiceEntity::class, InvoiceLineEntity::class, PaymentEntity::class, WithdrawalEntity::class,
        WorkerTxEntity::class, ExpenseEntity::class, ClosingEntity::class, PreorderEntity::class,
        StockMoveEntity::class, RoastEntity::class, StocktakeEntity::class, AuditEntity::class, UserEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mahamess_alharamayn.db",
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
