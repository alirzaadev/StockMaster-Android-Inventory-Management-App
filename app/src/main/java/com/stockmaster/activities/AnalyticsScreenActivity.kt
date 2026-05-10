package com.stockmaster.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockmaster.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AnalyticsUiState(
    val totalProducts: Int = 0,
    val lowStockItems: Int = 0,
    val totalInventoryValue: Double = 0.0
)

class AnalyticsScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var uiState by remember { mutableStateOf(AnalyticsUiState()) }

            LaunchedEffect(Unit) {
                uiState = loadAnalyticsState()
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnalyticsScreen(
                        state = uiState,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private suspend fun loadAnalyticsState(): AnalyticsUiState = withContext(Dispatchers.IO) {
        runCatching {
            val products = AppDatabase.getDatabase(applicationContext)
                .productDao()
                .getAllProductsList()

            AnalyticsUiState(
                totalProducts = products.size,
                lowStockItems = products.count { it.stockQuantity in 1..it.lowStockThreshold },
                totalInventoryValue = products.sumOf { it.sellingPrice * it.stockQuantity }
            )
        }.getOrElse {
            AnalyticsUiState()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Inventory Analytics") },
                actions = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnalyticsCard(
                title = "Total Products",
                value = state.totalProducts.toString(),
                subtitle = "Items currently stored in inventory"
            )
            AnalyticsCard(
                title = "Low Stock Items",
                value = state.lowStockItems.toString(),
                subtitle = "Products that need restocking"
            )
            AnalyticsCard(
                title = "Total Inventory Value",
                value = "Rs. ${String.format(Locale.getDefault(), "%,.0f", state.totalInventoryValue)}",
                subtitle = "Estimated value of all available stock"
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

