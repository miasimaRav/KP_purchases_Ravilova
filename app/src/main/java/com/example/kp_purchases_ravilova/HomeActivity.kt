package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kp_purchases_ravilova.data.BalanceResponse
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.PurchaseViewDto
import com.example.kp_purchases_ravilova.data.SpendingByCategoryDto
import com.example.kp_purchases_ravilova.databinding.ActivityHomeBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var currentUserId = 0
    private var currentMonth = ""
    private lateinit var todayAdapter: TodayPurchasesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = AuthPreferences.getUserId(this)
        if (currentUserId == 0) {
            finish()
            return
        }
        currentMonth = getCurrentMonth()
        // Настройка RecyclerView
        binding.todayPurchasesRecycler.layoutManager = LinearLayoutManager(this)
        todayAdapter = TodayPurchasesAdapter(emptyList())
        binding.todayPurchasesRecycler.adapter = todayAdapter
        loadBalance()
        loadTodayPurchases()
        loadCategoryChart()
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddPurchaseActivity::class.java))
        }

        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_1)
    }

    private fun getCurrentMonth(): String {
        return LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    private fun loadBalance() {
        Log.d("HomeActivity", "Загрузка баланса для userId=$currentUserId, month=$currentMonth")
        lifecycleScope.launch {
            try {
                val response = Client.budgetApi.getBalance(currentUserId, currentMonth)
                Log.d("HomeActivity", "Response: success=${response.success}, data=${response.data}")
                if (response.success && response.data != null) {
                    Log.d("HomeActivity", "Баланс получен: ${response.data}")
                    val balance = response.data.balance.replace(",", ".").toDoubleOrNull() ?: 0.0
                    binding.balanceTv.text = String.format("%.2f ₽", balance)

                    if (balance < 0) {
                        binding.limitWarningTv.visibility = View.VISIBLE
                        binding.limitWarningTv.setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.error))
                        binding.limitWarningTv.text = getString(R.string.limit_exceeded)
                    } else {
                        binding.limitWarningTv.visibility = View.VISIBLE
                        binding.limitWarningTv.setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.today))
                        binding.limitWarningTv.text = getString(R.string.balance_remaining, String.format("%.2f", balance))
                    }

                } else {
                    Log.e("HomeActivity", "response.success=false или data=null")
                    binding.balanceTv.text = "0.00 ₽"
                    binding.limitWarningTv.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Ошибка: ${e.message}", e)
                binding.balanceTv.text = "0.00 ₽"
                binding.limitWarningTv.visibility = View.GONE
                Toast.makeText(this@HomeActivity, "Ошибка загрузки баланса", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTodayPurchases() {
        Log.d("HomeActivity", "Загрузка покупок за сегодня")
        val today = LocalDate.now()
        lifecycleScope.launch {
            try {
                val response = Client.purchaseApi.getUserPurchases(currentUserId)
                Log.d("HomeActivity", "Покупки: success=${response.success}, size=${response.data?.size}")

                if (response.success && response.data != null) {
                    val todayPurchases = response.data.filter { purchase ->
                        try {
                            // Парсим дату покупки
                            val purchaseDate = LocalDate.parse(
                                purchase.purchaseDate.substringBefore('T')
                            )
                            purchaseDate == today
                        } catch (e: Exception) {
                            Log.w("HomeActivity", "Неверный формат даты: ${purchase.purchaseDate}")
                            false  // Пропускаем некорректные даты
                        }
                    }
                    todayAdapter.updateData(todayPurchases)
                    Log.d("HomeActivity", "Найдено покупок за сегодня: ${todayPurchases.size}")
                } else {
                    todayAdapter.updateData(emptyList())
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Покупки ошибка: ${e.message}", e)
                todayAdapter.updateData(emptyList())
            }
        }
    }


    private fun loadCategoryChart() {
        // Настройка сообщения для пустой диаграммы
        binding.pieChart.setNoDataText("Нет покупок за $currentMonth")
        binding.pieChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        lifecycleScope.launch {
            try {
                val response = Client.analyticsApi.getSpendingByCategory(currentUserId, currentMonth)
                if (response.success && response.data != null) {
                    val data = response.data
                    if (data.isNotEmpty()) {
                        val entries = data.mapNotNull { category ->
                            val amount = category.totalSpent.replace(",", ".").toFloatOrNull()
                            if (amount != null && amount > 0) {
                                PieEntry(amount, category.categoryName)
                            } else null
                        }

                        if (entries.isNotEmpty()) {
                            val dataSet = PieDataSet(entries, "").apply {
                                colors = listOf(
                                    ContextCompat.getColor(this@HomeActivity, R.color.primary),
                                    ContextCompat.getColor(this@HomeActivity, R.color.primary_dark),
                                    ContextCompat.getColor(this@HomeActivity, R.color.secondary),
                                    ContextCompat.getColor(this@HomeActivity, R.color.today),
                                    ContextCompat.getColor(this@HomeActivity, R.color.error),
                                    ContextCompat.getColor(this@HomeActivity, R.color.text_primary),
                                    ContextCompat.getColor(this@HomeActivity, R.color.text_secondary),
                                    ContextCompat.getColor(this@HomeActivity, R.color.blue),
                                    ContextCompat.getColor(this@HomeActivity, R.color.purple_dark),
                                    ContextCompat.getColor(this@HomeActivity, R.color.beige),
                                    ContextCompat.getColor(this@HomeActivity, R.color.orange),
                                    ContextCompat.getColor(this@HomeActivity, R.color.bright_purple)
                                ).take(entries.size)
                                valueTextSize = 20f
                            }
                            val pieData = PieData(dataSet)
                            binding.pieChart.data = pieData
                            binding.pieChart.description.isEnabled = false
                            binding.pieChart.legend.isEnabled = true
                            binding.pieChart.invalidate()
                            return@launch
                        }
                    }
                }
                // Пустой график
                binding.pieChart.data = null
                binding.pieChart.invalidate()
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Нет данных для аналитики", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

