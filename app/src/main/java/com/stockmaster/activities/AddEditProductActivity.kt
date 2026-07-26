package com.stockmaster.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stockmaster.R
import com.stockmaster.models.Product
import com.stockmaster.repository.FirestoreSyncManager
import com.stockmaster.utils.ProfitCalculator
import com.stockmaster.viewmodels.ProductViewModel

class AddEditProductActivity : AppCompatActivity() {

    private lateinit var productViewModel: ProductViewModel

    private lateinit var tvTitle: TextView
    private lateinit var tilName: TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var tilDescription: TextInputLayout
    private lateinit var tilPurchase: TextInputLayout
    private lateinit var tilSelling: TextInputLayout
    private lateinit var tilStock: TextInputLayout
    private lateinit var tilThreshold: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPurchase: TextInputEditText
    private lateinit var etSelling: TextInputEditText
    private lateinit var etStock: TextInputEditText
    private lateinit var etThreshold: TextInputEditText
    private lateinit var tvMarginPreview: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: com.google.android.material.appbar.MaterialToolbar

    private var editProduct: Product? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_product)

        productViewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        // Detect ADD or EDIT mode
        val mode = intent.getStringExtra("MODE") ?: "ADD"
        isEditMode = mode == "EDIT"
        if (isEditMode) {
            val b = intent.extras
            editProduct = b?.getSerializable("PRODUCT_OBJECT") as? Product
        }

        // Bind views
        tvTitle = findViewById(R.id.tv_add_edit_title)
        tilName = findViewById(R.id.til_name)
        tilCategory = findViewById(R.id.til_category)
        tilDescription = findViewById(R.id.til_description)
        tilPurchase = findViewById(R.id.til_purchase)
        tilSelling = findViewById(R.id.til_selling)
        tilStock = findViewById(R.id.til_stock)
        tilThreshold = findViewById(R.id.til_threshold)
        etName = findViewById(R.id.et_name)
        actCategory = findViewById(R.id.act_category)
        etDescription = findViewById(R.id.et_description)
        etPurchase = findViewById(R.id.et_purchase)
        etSelling = findViewById(R.id.et_selling)
        etStock = findViewById(R.id.et_stock)
        etThreshold = findViewById(R.id.et_threshold)
        tvMarginPreview = findViewById(R.id.tv_margin_preview)
        btnSave = findViewById(R.id.btn_save_product)
        btnBack = findViewById(R.id.toolbar_add_edit)

        tvTitle.text = if (isEditMode) "Edit Product" else "Add Product"
        btnSave.text = if (isEditMode) "Update Product" else "Save Product"

        btnBack.setNavigationOnClickListener { finish() }

        // Category dropdown
        val categories = listOf("Electronics", "Food", "Clothing", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        actCategory.setAdapter(categoryAdapter)

        // Fill fields if editing
        editProduct?.let { p ->
            etName.setText(p.name)
            actCategory.setText(p.category, false)
            etDescription.setText(p.description)
            etPurchase.setText(p.purchasePrice.toString())
            etSelling.setText(p.sellingPrice.toString())
            etStock.setText(p.stockQuantity.toString())
            etThreshold.setText(p.lowStockThreshold.toString())
            updateMarginPreview()
        }

        // Clear errors on focus
        setupErrorClearing(tilName, etName)
        setupErrorClearing(tilPurchase, etPurchase)
        setupErrorClearing(tilSelling, etSelling)
        setupErrorClearing(tilStock, etStock)
        setupErrorClearing(tilThreshold, etThreshold)

        // Live profit margin preview
        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateMarginPreview()
            }
        }
        etPurchase.addTextChangedListener(priceWatcher)
        etSelling.addTextChangedListener(priceWatcher)

        // Save button
        btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun setupErrorClearing(til: TextInputLayout, et: TextInputEditText) {
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { til.error = null }
        })
    }

    private fun updateMarginPreview() {
        val purchase = etPurchase.text.toString().trim().toDoubleOrNull()
        val selling = etSelling.text.toString().trim().toDoubleOrNull()

        if (purchase != null && selling != null && selling > 0) {
            val margin = ProfitCalculator.calculateMarginPercent(purchase, selling)
            val label = ProfitCalculator.getMarginLabel(margin)
            tvMarginPreview.text = "Profit Margin: ${String.format("%.1f", margin)}% ($label)"
            val colorRes = ProfitCalculator.getMarginColorRes(margin)
            tvMarginPreview.setTextColor(ContextCompat.getColor(this, colorRes))
            tvMarginPreview.visibility = android.view.View.VISIBLE
        } else {
            tvMarginPreview.text = "Enter prices to see margin"
            tvMarginPreview.setTextColor(ContextCompat.getColor(this, R.color.outline))
        }
    }

    private fun saveProduct() {
        // Validate
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            tilName.error = "Enter product name"
            return
        }

        val category = actCategory.text.toString().trim()
        if (category.isEmpty()) {
            tilCategory.error = "Select a category"
            return
        }

        val description = etDescription.text.toString().trim()

        val purchasePrice = etPurchase.text.toString().trim().toDoubleOrNull()
        if (purchasePrice == null) {
            tilPurchase.error = "Enter a valid price"
            return
        }

        val sellingPrice = etSelling.text.toString().trim().toDoubleOrNull()
        if (sellingPrice == null) {
            tilSelling.error = "Enter a valid price"
            return
        }

        val stockQty = etStock.text.toString().trim().toIntOrNull()
        if (stockQty == null) {
            tilStock.error = "Enter a valid number"
            return
        }

        val threshold = etThreshold.text.toString().trim().toIntOrNull()
        if (threshold == null) {
            tilThreshold.error = "Enter a valid number"
            return
        }

        if (isEditMode) {
            val existing = editProduct ?: run {
                Toast.makeText(this, "Error: product data missing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val updated = existing.copy(
                name = name,
                category = category,
                description = description,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQty,
                lowStockThreshold = threshold
            )
            productViewModel.update(updated) {
                syncProductToFirestore(updated)
                Toast.makeText(this, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val newProduct = Product(
                name = name,
                category = category,
                description = description,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQty,
                lowStockThreshold = threshold
            )
            productViewModel.insert(newProduct) {
                val localId = it.toInt()
                syncProductToFirestore(newProduct.copy(id = localId))
                Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun syncProductToFirestore(product: Product) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirestoreSyncManager.upsertProduct(product = product, ownerId = ownerId)
    }
}
