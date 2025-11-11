package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import java.time.LocalDate
import java.time.YearMonth

class PurchasesActivity : AppCompatActivity() {
    private lateinit var calendarView: WeekCalendarView // Календарь
    private lateinit var purchaseListLayout: LinearLayout // Контейнер для чекбоксов покупок
    private var selectedDate: LocalDate? = LocalDate.now() // Выбранная дата
    private val purchasesMap = mutableMapOf<LocalDate, List<Purchase>>() // Словарь: дата -> покупки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

        // Находим элементы интерфейса
        calendarView = findViewById(R.id.weekCalendar) // Календарь
        purchaseListLayout = findViewById(R.id.purchaseList) // Контейнер для покупок
        val fab: FloatingActionButton = findViewById(R.id.fab) // Кнопка добавления


        // Тестовые покупки (потом заменим на данные из базы)
        val today = LocalDate.now()
        purchasesMap[today] = listOf(
            Purchase("1", "Молоко", 50.0, today),
            Purchase("2", "Хлеб", 30.0, today)
        )
        purchasesMap[today.plusDays(2)] = listOf(
            Purchase("3", "Масло", 120.0, today.plusDays(2))
        )

        // Настраиваем календарь
        setupCalendar()

        // Показываем покупки для начальной даты
        showPurchasesForDate(selectedDate)

        // При клике на FAB открываем экран добавления покупки
        fab.setOnClickListener {
            val intent = Intent(this, AddPurchaseActivity::class.java)
            startActivity(intent)
        }

        // Устанавливаем текущий экран в навигации
        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_2)
    }

    // Функция настройки календаря (как в Example 5)
    private fun setupCalendar() {
        // Устанавливаем диапазон: 50 месяцев назад и вперед
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(50)
        val endMonth = currentMonth.plusMonths(50)
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Первый день недели

        // Настраиваем календарь
        val startDate = currentMonth.minusMonths(50).atDay(1) // Первый день начального месяца
        val endDate = currentMonth.plusMonths(50).atEndOfMonth() // Последний день конечного месяца
        calendarView.setup(startDate, endDate, firstDayOfWeek)

        // Устанавливаем, как выглядят дни
        calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            // Создаем контейнер для дня
            override fun create(view: View) = DayViewContainer(view)

            // Настраиваем вид дня
            override fun bind(container: DayViewContainer, weekDay: WeekDay) {
                val day = weekDay.date // Дата дня
                container.textView.text = day.dayOfMonth.toString() // Устанавливаем номер дня

                if (day == selectedDate) {
                    // Выбранный день
                    container.textView.setBackgroundResource(R.drawable.bg_selected)
                    container.textView.setTextColor(getColor(R.color.white))
                } else if (day == LocalDate.now()) {
                    // Сегодня
                    container.textView.setBackgroundResource(R.drawable.bg_today)
                    container.textView.setTextColor(getColor(R.color.white))
                } else {
                    // Обычный день: без фона, черный текст
                    container.textView.background = null
                    container.textView.setTextColor(getColor(R.color.black))
                }

                // При клике выбираем дату и обновляем покупки
                container.view.setOnClickListener {
                    val oldDate = selectedDate
                    selectedDate = day
                    // Обновляем календарь
                    if (oldDate != null) calendarView.notifyDateChanged(oldDate)
                    calendarView.notifyDateChanged(day)
                    // Показываем покупки для выбранной даты
                    showPurchasesForDate(day)
                }
            }
        }

    }

    // Функция для показа покупок за выбранную дату
    private fun showPurchasesForDate(date: LocalDate?) {
        // Очищаем старые чекбоксы
        purchaseListLayout.removeAllViews()

        // Получаем покупки за дату (или пустой список, если нет)
        val purchases = purchasesMap[date].orEmpty()

        // Если покупок нет, добавляем текст "Нет покупок"
        if (purchases.isEmpty()) {
            val textView = TextView(this).apply {
                text = "Нет покупок"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            purchaseListLayout.addView(textView)
            return
        }

        // Для каждой покупки создаем чекбокс
        for (purchase in purchases) {
            val checkBox = MaterialCheckBox(this).apply {
                // Устанавливаем текст: "Название: сумма ₽"
                text = "${purchase.name}: ${purchase.amount} ₽"
                // Устанавливаем состояние чекбокса
                isChecked = purchase.isChecked
                // При клике на чекбокс обновляем состояние покупки
                setOnCheckedChangeListener { _, isChecked ->
                    purchase.isChecked = isChecked
                }
                // Настраиваем внешний вид
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }
            // Добавляем чекбокс в LinearLayout
            purchaseListLayout.addView(checkBox)
        }
    }
}