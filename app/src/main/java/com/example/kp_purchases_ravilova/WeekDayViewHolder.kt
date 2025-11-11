package com.example.kp_purchases_ravilova

import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.time.format.TextStyle
import androidx.core.content.ContextCompat
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate
import java.util.Locale

class WeekDayViewHolder(
    view: View,
    private val onClick: (LocalDate) -> Unit
) : ViewContainer(view) {

    private val dayText: TextView = view.findViewById(R.id.dayText)
        //private val dowText: TextView = view.findViewById(R.id.dowText)

    @RequiresApi(Build.VERSION_CODES.O)
    fun bind(day: WeekDay, isSelected: Boolean) {
        dayText.text = day.date.dayOfMonth.toString()
       // dowText.text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        view.setBackgroundColor(if (isSelected) ContextCompat.getColor(view.context, R.color.primary) else Color.TRANSPARENT)
        dayText.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
       // dowText.setTextColor(if (isSelected) Color.WHITE else Color.GRAY)

        view.setOnClickListener { onClick(day.date) }
    }
}