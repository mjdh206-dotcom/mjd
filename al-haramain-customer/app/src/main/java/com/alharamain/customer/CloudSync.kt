package com.alharamain.customer

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

internal object CloudSync {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun ensureSignedIn(onReady: (String?) -> Unit) {
        auth.currentUser?.let { onReady(it.uid); return }
        auth.signInAnonymously().addOnCompleteListener { task -> onReady(if (task.isSuccessful) task.result.user?.uid else null) }
    }

    fun watchProducts(onValue: (List<Map<String, Any>>) -> Unit): ListenerRegistration =
        db.collection("products").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onValue(snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) })
        }

    fun watchOffer(onValue: (Map<String, Any>?) -> Unit): ListenerRegistration =
        db.collection("offers").document("current").addSnapshotListener { snap, _ -> onValue(snap?.takeIf { it.exists() }?.data) }

    fun submitOrder(uid: String, order: Map<String, Any>, done: (Boolean) -> Unit) {
        db.collection("orders").add(order + mapOf("userId" to uid, "status" to "جديد", "createdAt" to System.currentTimeMillis()))
            .addOnCompleteListener { done(it.isSuccessful) }
    }

    fun watchOrders(uid: String, onValue: (List<Map<String, Any>>) -> Unit): ListenerRegistration =
        db.collection("orders").whereEqualTo("userId", uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onValue(snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L })
        }
}
