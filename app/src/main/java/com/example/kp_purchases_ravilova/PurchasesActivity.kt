package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.kp_purchases_ravilova.data.CategoryDto
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.PurchaseStatusUpdateDto
import com.example.kp_purchases_ravilova.data.PurchaseViewDto
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import com.kizitonwose.calendar.view.WeekScrollListener
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

class PurchasesActivity : AppCompatActivity() {
    private lateinit var calendarView: WeekCalendarView
    private lateinit var purchaseListLayout: LinearLayout
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthBtn: ImageButton
    private lateinit var nextMonthBtn: ImageButton
    private var selectedDate: LocalDate? = LocalDate.now()
    private var currentWeekStart: LocalDate? = null
    private val purchasesMap = mutableMapOf<LocalDate, List<PurchaseViewDto>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

        // Инициализация views
        calendarView = findViewById(R.id.weekCalendar)
        purchaseListLayout = findViewById(R.id.purchaseList)
        monthYearText = findViewById(R.id.monthYearText)
        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)
        val fab: FloatingActionButton = findViewById(R.id.fab)

        prevMonthBtn.setOnClickListener { navigateMonth(-1) }
        nextMonthBtn.setOnClickListener { navigateMonth(1) }

        setupCalendar()
        loadPurchases()
        updateMonthTitle()
        showPurchasesForDate(selectedDate)

        fab.setOnClickListener {
            startActivity(Intent(this, AddPurchaseActivity::class.java))
        }

        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_2)
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.minusYears(10).atStartOfMonth()
        val endDate = currentMonth.plusYears(10).atEndOfMonth()
        val firstDayOfWeek = WeekFields.of(Locale("ru")).getFirstDayOfWeek()

        calendarView.setup(startDate, endDate, firstDayOfWeek)
        calendarView.scrollToWeek(LocalDate.now())

        calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, weekDay: WeekDay) {
                val day = weekDay.date
                container.textView.text = day.dayOfMonth.toString()

                if (day == selectedDate) {
                    container.textView.setBackgroundResource(R.drawable.bg_selected)
                    container.textView.setTextColor(getColor(R.color.white))
                } else if (day == LocalDate.now()) {
                    container.textView.setBackgroundResource(R.drawable.bg_today)
                    container.textView.setTextColor(getColor(R.color.white))
                } else {
                    container.textView.background = null
                    container.textView.setTextColor(getColor(R.color.black))
                }

                container.view.setOnClickListener {
                    val oldDate = selectedDate
                    selectedDate = day
                    if (oldDate != null) calendarView.notifyDateChanged(oldDate)
                    calendarView.notifyDateChanged(day)
                    showPurchasesForDate(day)
                }
            }

        }

        calendarView.weekScrollListener = object : WeekScrollListener {
            override fun invoke(week: Week) {
                currentWeekStart = week.days.first().date
                updateMonthTitle()
            }
        }
    }


    private fun navigateMonth(direction: Int) {
        calendarView.findFirstVisibleWeek()?.let { week ->
            val targetWeek = if (direction > 0) {
                week.days.last().date.plusDays(31)
            } else {
                week.days.first().date.minusDays(31)
            }
            calendarView.scrollToWeek(targetWeek)
        }
    }


    private fun updateMonthTitle() {
        currentWeekStart?.let { weekStart ->
            val daysInWeek = calendarView.findFirstVisibleWeek()?.days ?: return
            val uniqueMonths = daysInWeek.map { it.date.month }.distinct()
            val monthName = if (uniqueMonths.size > 1) {
                "${uniqueMonths.first()} - ${uniqueMonths.last()}"
            } else {
                uniqueMonths.first().getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            }
            monthYearText.text = "$monthName ${weekStart.year}"
        } ?: run {
            monthYearText.text = getString(R.string.current_month)
        }
    }

    private fun loadPurchases() {
        Log.d("Purchases", "Загрузка покупок для userId=${AuthPreferences.getUserId(this)}")
        lifecycleScope.launch {
            try {
                val response = Client.purchaseApi.getUserPurchases(AuthPreferences.getUserId(this@PurchasesActivity))
                Log.d("Purchases", "Response: success=${response.success}, size=${response.data?.size}")

                if (response.success && response.data != null) {
                    val purchases = response.data
                    purchasesMap.clear()

                    purchases.forEach { p ->
                        try {
                            val date = try {
                                val dateStr = p.purchaseDate.substringBefore('T')
                                LocalDate.parse(dateStr)
                            } catch (e: Exception) {
                                Log.w("Purchases", "Неверный формат даты: ${p.purchaseDate}")
                                LocalDate.now()
                            }

                            val list = purchasesMap[date]?.toMutableList() ?: mutableListOf()
                            list.add(p)
                            purchasesMap[date] = list
                            Log.d("Purchases", "Добавлена покупка: ${p.productName} на $date")
                        } catch (e: Exception) {
                            Log.e("Purchases", "Ошибка обработки покупки: ${p.productName}", e)
                        }
                    }

                    Log.d("Purchases", "Загружено дат: ${purchasesMap.size}")
                    showPurchasesForDate(selectedDate)
                } else {
                    Log.w("Purchases", "Пустой ответ: ${response.message}")
                }
            } catch (e: HttpException) {
                Log.e("Purchases", "HTTP ${e.code()}: ${e.message()}")
            } catch (e: Exception) {
                Log.e("Purchases", "${e.message}", e)
            }
        }
    }

    // Функция для показа покупок за выбранную дату
    private fun showPurchasesForDate(date: LocalDate?) {
        purchaseListLayout.removeAllViews()

        val purchases = purchasesMap[date].orEmpty()

        if (purchases.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Нет покупок"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            purchaseListLayout.addView(tv)
            return
        }

        purchases.forEach { p ->
            val purchaseItemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
            }

            val checkBox = MaterialCheckBox(this).apply {
                text = "${p.productName} – ${p.cost} ₽ (${p.shopName})"
                textSize = 16f
                isChecked = p.isCompleted  //Устанавливаем текущий статус
                buttonTintList = ContextCompat.getColorStateList(this@PurchasesActivity, R.color.primary)

                setOnCheckedChangeListener { _, isChecked ->
                    Log.d("Purchases", "Изменение статуса покупки ${p.id}: $isChecked")
                    updatePurchaseStatus(p.id, isChecked)
                }
            }

            purchaseItemLayout.addView(checkBox)
            purchaseListLayout.addView(purchaseItemLayout)
        }
    }

    private fun updatePurchaseStatus(purchaseId: Int, isCompleted: Boolean) {
        Log.d("Purchases", "Обновление покупки ID=$purchaseId, completed=$isCompleted")

        lifecycleScope.launch {
            try {
                val statusDto = PurchaseStatusUpdateDto(isCompleted)
                val response = Client.purchaseApi.updatePurchaseStatus(purchaseId, statusDto)
                if (response.success == true) {
                    Log.d("Purchases", "Статус покупки $purchaseId обновлен")
                    Toast.makeText(this@PurchasesActivity, "Статус обновлен", Toast.LENGTH_SHORT).show()

                    loadPurchases()
                } else {
                    Log.e("Purchases", "Ошибка API: ${response.message}")
                    Toast.makeText(this@PurchasesActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Purchases", "Ошибка сети: ${e.message}", e)
                Toast.makeText(this@PurchasesActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
            }
        }
    }

}