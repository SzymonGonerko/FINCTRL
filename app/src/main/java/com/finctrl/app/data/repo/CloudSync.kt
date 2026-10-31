package com.finctrl.app.data.repo

import com.finctrl.app.core.ValidationException
import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TxType
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudSync(private val firestore: FirebaseFirestore, private val auth: AuthRepository) {

    private fun userRoot() = firestore.collection("users").document(
        auth.uid ?: throw ValidationException("Kopia zapasowa wymaga zalogowanego konta.")
    )

    suspend fun backup(s: Snapshot) {
        val root = userRoot()
        replace(root.collection("categories"), s.categories.map { it.id.toString() to it.toMap() })
        replace(root.collection("transactions"), s.transactions.map { it.id.toString() to it.toMap() })
        replace(root.collection("budgets"), s.budgets.map { it.id.toString() to it.toMap() })
    }

    suspend fun restore(): Snapshot {
        val root = userRoot()
        val cats = root.collection("categories").get().await().documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            CategoryEntity(d.id.toLong(), m.str("name"), TxType.valueOf(m.str("type")), m.str("colorHex"))
        }
        val txs = root.collection("transactions").get().await().documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            TransactionEntity(
                d.id.toLong(), m.lng("amount"), TxType.valueOf(m.str("type")),
                (m["categoryId"] as? Number)?.toLong(), m.str("note"), m.lng("date"), m.str("currency"),
            )
        }
        val buds = root.collection("budgets").get().await().documents.mapNotNull { d ->
            val m = d.data ?: return@mapNotNull null
            BudgetEntity(d.id.toLong(), m.lng("categoryId"), m.str("month"), m.lng("limitAmount"))
        }
        return Snapshot(cats, txs, buds)
    }

    private suspend fun replace(col: CollectionReference, docs: List<Pair<String, Map<String, Any?>>>) {
        col.get().await().documents.chunked(400).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
        }
        docs.chunked(400).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { (id, data) -> set(col.document(id), data) } }.commit().await()
        }
    }

    private fun Map<String, Any?>.str(k: String) = this[k] as? String ?: ""
    private fun Map<String, Any?>.lng(k: String) = (this[k] as? Number)?.toLong() ?: 0L

    private fun CategoryEntity.toMap() = mapOf("name" to name, "type" to type.name, "colorHex" to colorHex)
    private fun TransactionEntity.toMap() = mapOf(
        "amount" to amount, "type" to type.name, "categoryId" to categoryId,
        "note" to note, "date" to date, "currency" to currency,
    )
    private fun BudgetEntity.toMap() = mapOf("categoryId" to categoryId, "month" to month, "limitAmount" to limitAmount)
}
