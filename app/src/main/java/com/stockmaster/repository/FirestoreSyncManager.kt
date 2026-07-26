package com.stockmaster.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.stockmaster.models.Product
import com.stockmaster.utils.Constants

object FirestoreSyncManager {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun upsertCurrentUser(fallbackEmail: String? = null, fallbackName: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDocRef = firestore.collection(Constants.FS_USERS_COLLECTION).document(currentUser.uid)

        userDocRef.get().addOnSuccessListener { snapshot ->
            val email = currentUser.email ?: fallbackEmail.orEmpty()
            val name = currentUser.displayName
                ?: fallbackName
                ?: email.substringBefore('@').ifBlank { "User" }

            val updateData = mutableMapOf<String, Any>(
                Constants.FS_FIELD_NAME to name,
                Constants.FS_FIELD_EMAIL to email
            )
            if (!snapshot.contains(Constants.FS_FIELD_CREATED_AT)) {
                updateData[Constants.FS_FIELD_CREATED_AT] = FieldValue.serverTimestamp()
            }

            userDocRef.set(updateData, SetOptions.merge())
        }
    }

    fun upsertProduct(product: Product, ownerId: String) {
        val productId = if (product.id > 0) {
            "${ownerId}_${product.id}"
        } else {
            firestore.collection(Constants.FS_PRODUCTS_COLLECTION).document().id
        }

        val data = hashMapOf<String, Any>(
            Constants.FS_FIELD_NAME to product.name,
            Constants.FS_FIELD_PRICE to product.sellingPrice,
            Constants.FS_FIELD_QUANTITY to product.stockQuantity,
            Constants.FS_FIELD_OWNER_ID to ownerId,
            Constants.FS_FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            Constants.FS_FIELD_CATEGORY to product.category,
            Constants.FS_FIELD_DESCRIPTION to product.description,
            Constants.FS_FIELD_LOW_STOCK_THRESHOLD to product.lowStockThreshold,
            Constants.FS_FIELD_LOCAL_ID to product.id
        )

        firestore.collection(Constants.FS_PRODUCTS_COLLECTION)
            .document(productId)
            .set(data, SetOptions.merge())
    }

    fun listenProductsByOwner(
        ownerId: String,
        onProductsChanged: (List<Product>) -> Unit,
        onError: () -> Unit
    ): ListenerRegistration {
        return firestore.collection(Constants.FS_PRODUCTS_COLLECTION)
            .whereEqualTo(Constants.FS_FIELD_OWNER_ID, ownerId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError()
                    return@addSnapshotListener
                }

                val products = snapshots?.documents.orEmpty().mapNotNull { document ->
                    mapFirestoreDocumentToProduct(document.data ?: emptyMap())
                }
                onProductsChanged(products)
            }
    }

    private fun mapFirestoreDocumentToProduct(data: Map<String, Any>): Product? {
        val name = data[Constants.FS_FIELD_NAME] as? String ?: return null
        val price = (data[Constants.FS_FIELD_PRICE] as? Number)?.toDouble() ?: 0.0
        val quantity = (data[Constants.FS_FIELD_QUANTITY] as? Number)?.toInt() ?: 0
        val category = data[Constants.FS_FIELD_CATEGORY] as? String ?: "Other"
        val description = data[Constants.FS_FIELD_DESCRIPTION] as? String ?: ""
        val threshold = (data[Constants.FS_FIELD_LOW_STOCK_THRESHOLD] as? Number)?.toInt() ?: 10
        val localId = (data[Constants.FS_FIELD_LOCAL_ID] as? Number)?.toInt() ?: 0
        val timestamp = data[Constants.FS_FIELD_TIMESTAMP] as? Timestamp
        val dateAdded = timestamp?.toDate()?.time ?: System.currentTimeMillis()

        return Product(
            id = localId,
            name = name,
            category = category,
            description = description,
            purchasePrice = price,
            sellingPrice = price,
            stockQuantity = quantity,
            lowStockThreshold = threshold,
            dateAdded = dateAdded
        )
    }
}

