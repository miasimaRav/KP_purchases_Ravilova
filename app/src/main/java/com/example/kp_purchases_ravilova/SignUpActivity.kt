package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.UserDto
import com.example.kp_purchases_ravilova.databinding.ActivitySignUpBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val emailPattern = Regex("^[a-z0-9]+@[a-z0-9]+\\.[a-z]{2,3}$")

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val userApi = Client.userApi

    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signUpBtn.setOnClickListener {
            signup()
        }

        binding.birthdayEt.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Дата рождения")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.birthdayEt.setText(sdf.format(Date(selection)))
            }

            picker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun signup() {
        val fio = binding.fioEt.text.toString()
        val email = binding.emailEt.text.toString()
        val password = binding.passwordEt.text.toString()
        val confirmPassword = binding.password2Et.text.toString()
        val birthday = binding.birthdayEt.text.toString()

        if (fio.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Validation failed: empty fields")
            return
        }

        if (!emailPattern.matches(email)) {
            binding.emailEt.error = "Неверный формат email"
            Toast.makeText(this, "Неверный формат email", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Validation failed: invalid email format: $email")
            return
        }
        if (password.length < 6) {
            binding.passwordEt.error = "Пароль должен содержать минимум 6 символов"
            Toast.makeText(this, "Пароль слишком короткий (минимум 6 символов)", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Validation failed: password too short: ${password.length}")
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Validation failed: passwords do not match")
            return
        }

        val parts = fio.trim().split(" ")
        val lastName = parts.firstOrNull() ?: ""
        val firstName = parts.drop(1).firstOrNull() ?: ""
        val patronymic = if (parts.size > 2) parts.last() else null

        val userDto = UserDto(
            lastName = lastName,
            firstName = firstName,
            patronymic = patronymic?.takeIf { it.isNotEmpty() },
            email = email,
            birthday = birthday,
            passwordHash = password
        )

        lifecycleScope.launch {
            try {
                binding.signUpBtn.isEnabled = false
                val response = userApi.registerUser(userDto)
                Log.d(TAG, "Response: ${response.success}, data=${response.data}")

                if (response.success != true || response.data !is UserDto) {
                    val errorMsg = response.data.toString()
                    Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                    binding.emailEt.error = errorMsg + ". Возможно email уже использовался"
                    return@launch
                }

                val userDto = response.data as UserDto
                Toast.makeText(this@SignUpActivity, "Регистрация успешна! ID: ${userDto.id}", Toast.LENGTH_LONG).show()
                AuthPreferences.saveUser(this@SignUpActivity, userDto.id ?: 0, userDto.email)

                startActivity(Intent(this@SignUpActivity, HomeActivity::class.java))
                finish()
            } catch (e: HttpException) {
                Toast.makeText(this@SignUpActivity, "Ошибка сервера: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@SignUpActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.signUpBtn.isEnabled = true
            }
        }

    }
}
