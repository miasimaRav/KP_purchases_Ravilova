package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_3)
        findViewById<Button>(R.id.limitBtn).setOnClickListener {
            startActivity(Intent(this, SetLimitActivity::class.java))
            finish()
        }
    }
}