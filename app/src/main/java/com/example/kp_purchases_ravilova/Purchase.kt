package com.example.kp_purchases_ravilova

import java.time.LocalDate

// временный (?) класс для хранения информации о покупке
data class Purchase(
    val id: String, // Уникальный ID покупки
    val name: String, // Название
    val amount: Double, // Сумма
    val date: LocalDate, // Дата покупки
    var isChecked: Boolean = false // Отмечена ли покупка
)