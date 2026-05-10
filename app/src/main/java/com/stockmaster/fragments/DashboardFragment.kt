package com.stockmaster.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stockmaster.R
import com.stockmaster.activities.AddEditProductActivity
import com.stockmaster.activities.AlertsActivity
import com.stockmaster.activities.AnalyticsScreenActivity
import com.stockmaster.adapters.SaleAdapter
import com.stockmaster.viewmodels.DashboardViewModel

class DashboardFragment : Fragment() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var saleAdapter: SaleAdapter

    private lateinit var tvTotalSales: TextView
    private lateinit var tvSalesUpdated: TextView
    private lateinit var tvTodayProfit: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvCategories: TextView
    private lateinit var tvAlertCount: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var btnViewAlerts: TextView
    private lateinit var btnAddProduct: LinearLayout
    private lateinit var btnNewSale: LinearLayout
    private lateinit var btnPrintReports: LinearLayout

    private var userRole: String = "STAFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userRole = arguments?.getString("USER_ROLE") ?: "STAFF"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Bind views
        tvTotalSales = view.findViewById(R.id.tv_total_sales)
        tvSalesUpdated = view.findViewById(R.id.tv_sales_updated)
        tvTodayProfit = view.findViewById(R.id.tv_today_profit)
        tvTotalItems = view.findViewById(R.id.tv_total_items)
        tvCategories = view.findViewById(R.id.tv_categories)
        tvAlertCount = view.findViewById(R.id.tv_alert_count)
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions)
        btnViewAlerts = view.findViewById(R.id.btn_view_alerts)
        btnAddProduct = view.findViewById(R.id.btn_add_product)
        btnNewSale = view.findViewById(R.id.btn_new_sale)
        btnPrintReports = view.findViewById(R.id.btn_print_reports)

        // Setup RecyclerView
        saleAdapter = SaleAdapter(requireContext())
        rvRecentTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvRecentTransactions.adapter = saleAdapter

        // Observe LiveData
        viewModel.totalRevenue.observe(viewLifecycleOwner) { revenue ->
            tvTotalSales.text = "Rs. ${String.format("%,.0f", revenue ?: 0.0)}"
            tvSalesUpdated.text = "Updated just now"
        }

        viewModel.todayProfit.observe(viewLifecycleOwner) { profit ->
            tvTodayProfit.text = "Rs. ${String.format("%,.0f", profit ?: 0.0)}"
        }

        viewModel.totalProductCount.observe(viewLifecycleOwner) { count ->
            tvTotalItems.text = "${count ?: 0}"
        }

        viewModel.categoryCount.observe(viewLifecycleOwner) { count ->
            tvCategories.text = "Across ${count ?: 0} Categories"
        }

        viewModel.lowStockCount.observe(viewLifecycleOwner) { count ->
            tvAlertCount.text = "${count ?: 0} items require immediate restock"
        }

        viewModel.recentSales.observe(viewLifecycleOwner) { sales ->
            saleAdapter.setData(sales)
        }

        // Alert banner click
        btnViewAlerts.setOnClickListener {
            val intent = Intent(requireContext(), AlertsActivity::class.java)
            intent.putExtra("USER_ROLE", userRole)
            startActivity(intent)
        }

        // Quick Actions
        btnAddProduct.setOnClickListener {
            val intent = Intent(requireContext(), AddEditProductActivity::class.java)
            intent.putExtra("MODE", "ADD")
            startActivity(intent)
        }

        btnNewSale.setOnClickListener {
            // Navigate to POS tab
            val activity = requireActivity() as? com.stockmaster.activities.MainActivity
            activity?.navigateToTab(R.id.nav_pos)
        }

        btnPrintReports.setOnClickListener {
            startActivity(Intent(requireContext(), AnalyticsScreenActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // LiveData auto-refreshes
    }
}
