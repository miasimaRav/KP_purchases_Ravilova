package com.example.kp_purchases_ravilova

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.view.ViewContainer

// Класс для хранения вида дня в календаре
class DayViewContainer(view: View) : ViewContainer(view) {
    // TextView для номера дня
    val textView: TextView = view.findViewById(R.id.dayText)
}