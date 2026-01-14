package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.LoginRequest
import com.example.kp_purchases_ravilova.data.LoginResponse
import com.example.kp_purchases_ravilova.databinding.ActivitySignInBinding
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInBtn.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Заполните email и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    binding.signInBtn.isEnabled = false
                    val response = Client.userApi.login(
                        LoginRequest(email = email, password = password)
                    )

                    if (response.success == true) {
                        val loginResp = response.data as LoginResponse
                        Toast.makeText(this@SignInActivity, "Вход успешен! ID: ${loginResp.userId}", Toast.LENGTH_LONG).show()

                        AuthPreferences.saveUser(this@SignInActivity, loginResp.userId, loginResp.email)
                        // Переход на главный экран
                        val intent = Intent(this@SignInActivity, HomeActivity::class.java).apply {
                            putExtra("userId", loginResp.userId)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@SignInActivity, response.message ?: "Ошибка входа", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SignInActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.signInBtn.isEnabled = true
                }
            }
        }
        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}