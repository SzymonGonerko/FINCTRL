package com.finctrl.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val colorHex: String = "#607D8B",
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("categoryId"), Index("date")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: TxType,
    val categoryId: Long?,
    val note: String = "",
    val date: Long,
    val currency: String = "PLN",
)

@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["categoryId", "month"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val month: String,
    val limitAmount: Long,
)

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    val categoryName: String?,
    val categoryColor: String?,
)

data class CategorySum(val categoryId: Long?, val total: Long)
data class TypeSum(val type: TxType, val total: Long)
