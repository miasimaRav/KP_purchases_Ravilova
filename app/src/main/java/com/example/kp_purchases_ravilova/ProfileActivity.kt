package com.example.kp_purchases_ravilova

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kp_purchases_ravilova.data.BalanceResponse
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.LimitDto
import com.example.kp_purchases_ravilova.data.LimitResponse
import com.example.kp_purchases_ravilova.data.UserDto
import com.example.kp_purchases_ravilova.databinding.ActivityProfileBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.util.Log

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var currentUserId = 0
    private var currentLimit: Pair<String, String>? = null // (limitRub, yearMonth)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = AuthPreferences.getUserId(this)
        if (currentUserId == 0) {
            finish()
            return
        }

        loadProfileData()
        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_3)

        binding.limitBtn.setOnClickListener {
            showSetLimitDialog()
        }

        binding.logoutBtn.setOnClickListener {
            AuthPreferences.logout(this)
            startActivity(Intent(this, SignInActivity::class.java))
            finishAffinity()
        }
    }

    private fun loadProfileData() {

        lifecycleScope.launch {
            try {
                // Загрузка профиля пользователя
                Log.d("ProfileActivity", "Запрос данных пользователя через API: getUserById($currentUserId)")
                val userResponse = Client.userApi.getUserById(currentUserId)
                Log.d("ProfileActivity", "Ответ API пользователя: success=${userResponse.success}, data=${userResponse.data}")

                if (userResponse.success == true && userResponse.data != null) {
                    val user = userResponse.data as UserDto
                    binding.fioTv.text = "${user.lastName} ${user.firstName}"
                    binding.emailTv.text = user.email
                    Log.d("ProfileActivity", "Профиль успешно загружен: ${user.lastName} ${user.firstName}, email: ${user.email}")
                } else {
                    Log.w("ProfileActivity", "API вернул ошибку или пустые данные, используется локальное сохранение")
                    val savedEmail = AuthPreferences.getEmail(this@ProfileActivity)
                    binding.fioTv.text = savedEmail ?: "Неизвестный пользователь"
                    binding.emailTv.text = savedEmail ?: ""
                }

                // Загрузка текущего лимита
                val currentMonth = getCurrentMonth()
                Log.d("ProfileActivity", "Запрос лимита для текущего месяца: getLimit($currentUserId, $currentMonth)")

                try {
                    val limitResponse = Client.budgetApi.getLimit(currentUserId, currentMonth)
                    Log.d("ProfileActivity", "Ответ API лимита: success=${limitResponse.success}, data=${limitResponse.data}")

                    if (limitResponse.success == true && limitResponse.data != null) {
                        val limit = limitResponse.data as LimitResponse
                        if (limit.limitRub != null && limit.limitRub.isNotEmpty()) {
                            binding.currentLimitTv.text = getString(
                                R.string.current_limit_format,
                                limit.limitRub,
                                limit.yearMonth
                            )
                            Log.d("ProfileActivity", "Лимит успешно загружен: ${limit.limitRub} руб. за ${limit.yearMonth}")
                        } else {
                            binding.currentLimitTv.text = getString(R.string.no_limit_set)
                            Log.i("ProfileActivity", "Лимит для месяца $currentMonth не установлен")
                        }
                    } else {
                        binding.currentLimitTv.text = getString(R.string.no_limit_set)
                        Log.w("ProfileActivity", "API лимита вернул success=false")
                    }
                } catch (httpEx: retrofit2.HttpException) {
                    if (httpEx.code() == 404) {
                        binding.currentLimitTv.text = getString(R.string.no_limit_set)
                        Log.i("ProfileActivity", "Лимит не найден (нормальный случай - 404)")
                    } else {
                        binding.currentLimitTv.text = getString(R.string.no_limit_set)
                        Log.e("ProfileActivity", "Неожиданный HTTP код: ${httpEx.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Неожиданная ошибка при загрузке лимита: ${e.message}", e)
                    binding.currentLimitTv.text = getString(R.string.no_limit_set)
                }

            } catch (httpEx: retrofit2.HttpException) {
                Log.e("ProfileActivity", "HTTP ошибка при загрузке профиля: ${httpEx.code()} - ${httpEx.message()}", httpEx)
                // Fallback для профиля
                val savedEmail = AuthPreferences.getEmail(this@ProfileActivity)
                binding.fioTv.text = savedEmail ?: "Неизвестный пользователь"
                binding.emailTv.text = savedEmail ?: ""
                binding.currentLimitTv.text = getString(R.string.no_limit_set)

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Критическая ошибка загрузки профиля: ${e.message}", e)
                val savedEmail = AuthPreferences.getEmail(this@ProfileActivity)
                binding.fioTv.text = savedEmail ?: "Неизвестный пользователь"
                binding.emailTv.text = savedEmail ?: ""
                binding.currentLimitTv.text = getString(R.string.no_limit_set)
            }
        }
    }



    private fun showSetLimitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_limit, null)
        val monthEt = dialogView.findViewById<TextInputEditText>(R.id.monthEt)
        val limitEt = dialogView.findViewById<TextInputEditText>(R.id.limitEt)

        // Текущий месяц или сохранённый лимит
        val defaultMonth = currentLimit?.second ?: getCurrentMonth()
        monthEt.setText(defaultMonth)

        // Подставляем текущий лимит если есть
        currentLimit?.first?.let { limit ->
            limitEt.setText(limit)
        }

        monthEt.setOnClickListener {
            showMonthPicker(monthEt)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_set_limit_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_save_limit) { _, _ ->
                val yearMonth = monthEt.text.toString().trim()
                val limitRub = limitEt.text.toString().trim()

                if (yearMonth.isNotBlank() && limitRub.isNotBlank()) {
                    lifecycleScope.launch {
                        saveLimit(yearMonth, limitRub)
                    }
                } else {
                    Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()

        dialog.show()
    }

    private fun showMonthPicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, _ ->
                val formattedMonth = String.format("%d-%02d", selectedYear, selectedMonth + 1)
                editText.setText(formattedMonth)
            },
            year,
            month,
            1
        )

        datePickerDialog.datePicker.spinnersShown = true
        datePickerDialog.datePicker.calendarViewShown = false
        datePickerDialog.show()
    }

    private fun getCurrentMonth(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    private suspend fun saveLimit(yearMonth: String, limitRub: String) {

        try {
            val limitDto = LimitDto(
                userId = currentUserId,
                yearMonth = yearMonth,
                limitRub = limitRub
            )

            val response = Client.budgetApi.setLimit(limitDto)
            Log.d("ProfileActivity", "Ответ сервера: success=${response.success}, message=${response.message}")

            if (response.success == true) {
                binding.currentLimitTv.text = getString(R.string.current_limit_format, limitRub, yearMonth)
                currentLimit = Pair(limitRub, yearMonth)
                Toast.makeText(this, "Лимит успешно сохранён", Toast.LENGTH_SHORT).show()
                Log.d("ProfileActivity", "Лимит успешно сохранён и UI обновлён")
            } else {
                Log.e("ProfileActivity", " Сервер вернул ошибку: ${response.message}")
                Toast.makeText(this, response.message ?: getString(R.string.error_generic), Toast.LENGTH_LONG).show()
            }
        } catch (httpEx: retrofit2.HttpException) {
            Log.e("ProfileActivity", " HTTP ошибка: ${httpEx.code()} - ${httpEx.message()}", httpEx)
            Toast.makeText(this, "Ошибка сервера: ${httpEx.message()}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Неожиданная ошибка: ${e.message}", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}
