package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.WeekCalendarView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale


class PurchasesActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var purchasesTv: TextView

    private var selectedDate: LocalDate = LocalDate.now()
    private val eventsMap = mutableMapOf<LocalDate, List<CalendarEvent>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

        calendarView = findViewById(R.id.weekCalendar)
        purchasesTv = findViewById(R.id.purchasesTv)
        val fab: FloatingActionButton = findViewById(R.id.fab)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)


        val today = LocalDate.now()
        eventsMap[today] = listOf(
            CalendarEvent("1", "Молоко 50 ₽", today),
            CalendarEvent("2", "Хлеб 30 ₽", today)
        )
        eventsMap[today.plusDays(2)] = listOf(
            CalendarEvent("3", "Масло 120 ₽", today.plusDays(2))
        )

        setupCalendar()

        fab.setOnClickListener {
            startActivity(Intent(this, AddPurchaseActivity::class.java))
        }


        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_2)
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(50)
        val lastMonth = currentMonth.plusMonths(50)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        calendarView.dayBinder = object : MonthDayBinder<CalendarDayViewHolder, CalendarDay> {
            override fun create(view: View): CalendarDayViewHolder =
                CalendarDayViewHolder(view) { date ->
                    selectedDate = date
                    calendarView.notifyDateChanged(date)
                    showPurchasesFor(date)
                }

            override fun bind(container: CalendarDayViewHolder, data: CalendarDay) {
                container.bind(data, eventsMap[data.date].orEmpty())
            }
        }

        showPurchasesFor(selectedDate)
    }

    private fun showPurchasesFor(date: LocalDate) {
        val list = eventsMap[date].orEmpty()
        purchasesTv.text = if (list.isEmpty()) "Нет покупок" else list.joinToString("\n") { it.title }
    }
}