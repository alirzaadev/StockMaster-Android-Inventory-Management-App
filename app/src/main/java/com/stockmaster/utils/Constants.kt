package com.stockmaster.utils

object Constants {
    const val PREF_NAME = "stockmaster_prefs"
    const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    const val DEFAULT_CURRENCY = "Rs."

    // Intent keys
    const val KEY_USER_NAME = "USER_NAME"
    const val KEY_USER_ROLE = "USER_ROLE"
    const val KEY_USER_ID = "USER_ID"
    const val KEY_PRODUCT_OBJECT = "PRODUCT_OBJECT"
    const val KEY_SALE_OBJECT = "SALE_OBJECT"
    const val KEY_MODE = "MODE"

    // Roles
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_STAFF = "STAFF"

    // Categories
    val CATEGORIES = listOf("Electronics", "Food", "Clothing", "Other")

    // Firestore
    const val FS_USERS_COLLECTION = "users"
    const val FS_PRODUCTS_COLLECTION = "products"
    const val FS_FIELD_NAME = "name"
    const val FS_FIELD_EMAIL = "email"
    const val FS_FIELD_CREATED_AT = "createdAt"
    const val FS_FIELD_PRICE = "price"
    const val FS_FIELD_QUANTITY = "quantity"
    const val FS_FIELD_OWNER_ID = "ownerId"
    const val FS_FIELD_TIMESTAMP = "timestamp"
    const val FS_FIELD_CATEGORY = "category"
    const val FS_FIELD_DESCRIPTION = "description"
    const val FS_FIELD_LOW_STOCK_THRESHOLD = "lowStockThreshold"
    const val FS_FIELD_LOCAL_ID = "localId"
}
