package com.stockmaster.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.stockmaster.R
import com.stockmaster.database.AppDatabase
import com.stockmaster.fragments.AnalyticsFragment
import com.stockmaster.fragments.DashboardFragment
import com.stockmaster.fragments.InventoryFragment
import com.stockmaster.fragments.SalesFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // F1 — Read from Intent extras (NOT companion, NOT static)
    private val userName: String by lazy { intent.getStringExtra("USER_NAME") ?: "User" }
    private val userRole: String by lazy { intent.getStringExtra("USER_ROLE") ?: "STAFF" }

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fab: FloatingActionButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnLogout: ImageButton
    private var currentTabId: Int = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        fab = findViewById(R.id.fab_add)
        btnNotifications = findViewById(R.id.btn_notifications)
        btnLogout = findViewById(R.id.btn_logout)

        // Set app bar title
        val tvAppTitle = findViewById<TextView>(R.id.tv_app_title)
        tvAppTitle.text = "StockMaster"

        // ADMIN vs STAFF: hide Analytics tab for staff
        if (userRole == "STAFF") {
            bottomNav.menu.removeItem(R.id.nav_analytics)
        }

        // F4 — Fragment transactions via BottomNav
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment(), "dashboard")
                    fab.visibility = View.GONE
                    currentTabId = R.id.nav_dashboard
                }
                R.id.nav_inventory -> {
                    loadFragment(InventoryFragment(), "inventory")
                    fab.visibility = View.VISIBLE
                    currentTabId = R.id.nav_inventory
                }
                R.id.nav_pos -> {
                    loadFragment(SalesFragment(), "sales")
                    fab.visibility = View.VISIBLE
                    currentTabId = R.id.nav_pos
                }
                R.id.nav_analytics -> {
                    loadFragment(AnalyticsFragment(), "analytics")
                    fab.visibility = View.GONE
                    currentTabId = R.id.nav_analytics
                }
            }
            true
        }

        // FAB action
        fab.setOnClickListener {
            when (currentTabId) {
                R.id.nav_inventory -> {
                    val intent = Intent(this, AddEditProductActivity::class.java)
                    intent.putExtra("MODE", "ADD")
                    startActivity(intent)
                }
                R.id.nav_pos -> {
                    val fragment = supportFragmentManager.findFragmentByTag("sales")
                    if (fragment is SalesFragment) {
                        fragment.showAddSaleBottomSheet()
                    }
                }
            }
        }

        // Notification bell
        btnNotifications.setOnClickListener {
            val intent = Intent(this, AlertsActivity::class.java)
            intent.putExtra("USER_ROLE", userRole)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }

        // Load unread alert badge count
        updateAlertBadge()

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), "dashboard")
            bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    override fun onResume() {
        super.onResume()
        updateAlertBadge()
    }

    // F4 — Manual FragmentManager transactions
    private fun loadFragment(fragment: Fragment, tag: String) {
        val args = Bundle().apply { putString("USER_ROLE", userRole) }
        fragment.arguments = args
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    // Public method for fragments to navigate tabs
    fun navigateToTab(tabId: Int) {
        bottomNav.selectedItemId = tabId
    }

    private fun updateAlertBadge() {
        val db = AppDatabase.getDatabase(applicationContext)
        db.stockAlertDao().getUnreadCount().observe(this) { count ->
            val badge = bottomNav.getOrCreateBadge(R.id.nav_dashboard)
            if (count != null && count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }
    }
}
