package com.example.kp_purchases_ravilova

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class PurchasesActivity : AppCompatActivity() {
    private lateinit var dateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

//        dateText = findViewById(R.id.dateText)
//        val currentDate = Calendar.getInstance()
//
//        // normal date picker
//        val datePicker: MaterialDatePicker<Long> =
//            MaterialDatePicker.Builder
//                .datePicker()
//                .setTitleText("Choose a date")
//                .build()
//        datePicker.minDate = currentDate.setWeekDate()
//        datePicker.maxDate = maxDate
//
//
//        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
//
//
//        datePicker.addOnPositiveButtonClickListener {
//            dateText.text = datePicker.headerText
//        }



    }
}