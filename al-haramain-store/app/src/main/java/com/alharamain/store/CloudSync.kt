package com.alharamain.store

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

internal object CloudSync {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun signIn(email: String, password: String, done: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            val uid = if (task.isSuccessful) task.result.user?.uid else null
            if (uid == null) done(null)
            else db.collection("config").document("access").get().addOnCompleteListener { config ->
                val authorized = config.isSuccessful && config.result?.getString("adminUid") == uid
                if (authorized) done(uid) else { auth.signOut(); done(null) }
            }
        }
    }
    fun signOut() = auth.signOut()
    fun currentUid(): String? = auth.currentUser?.uid

    fun watchProducts(onValue: (List<Map<String, Any>>) -> Unit): ListenerRegistration =
        db.collection("products").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onValue(snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) })
        }
    fun watchOrders(onValue: (List<Map<String, Any>>) -> Unit): ListenerRegistration =
        db.collection("orders").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onValue(snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L })
        }
    fun saveProduct(product: Map<String, Any>, done: (Boolean) -> Unit) {
        val id = product["id"]?.toString().orEmpty()
        val data = product - "id" - "image"
        val ref = if (id.isBlank()) db.collection("products").document() else db.collection("products").document(id)
        ref.set(data).addOnCompleteListener { done(it.isSuccessful) }
    }
    fun publishOffer(offer: Map<String, Any>, done: (Boolean) -> Unit) {
        db.collection("offers").document("current").set(offer).addOnCompleteListener { done(it.isSuccessful) }
    }
    fun updateOrderStatus(id: String, status: String, done: (Boolean) -> Unit) {
        db.collection("orders").document(id).update("status", status).addOnCompleteListener { done(it.isSuccessful) }
    }
}
