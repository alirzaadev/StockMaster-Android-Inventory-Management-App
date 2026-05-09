package com.stockmaster.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stockmaster.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvGoLogin: View
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        if (FirebaseAuthManager.currentUserEmail() != null) {
            openMainActivity(FirebaseAuthManager.currentUserEmail())
            return
        }

        rootView = findViewById(R.id.root_register)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        tvGoLogin = findViewById(R.id.tv_go_login)

        etEmail.doAfterTextChanged { tilEmail.error = null }
        etPassword.doAfterTextChanged { tilPassword.error = null }
        etConfirmPassword.doAfterTextChanged { tilConfirmPassword.error = null }

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.empty_email)
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.empty_password)
            return
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = getString(R.string.empty_confirm_password)
            return
        }
        if (password != confirmPassword) {
            tilConfirmPassword.error = getString(R.string.password_mismatch)
            return
        }

        FirebaseAuthManager.register(
            email = email,
            password = password,
            onSuccess = { openMainActivity(email) },
            onFailure = { message ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private fun openMainActivity(email: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_NAME", email ?: "User")
            putExtra("USER_ROLE", "ADMIN")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
