package com.example.kp_purchases_ravilova

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // находим view
        val balanceTv = findViewById<TextView>(R.id.balanceTv)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        balanceTv.text = getString(R.string.balance, "25 000")
        fab.setOnClickListener {
            startActivity(Intent(this, AddPurchaseActivity::class.java))
        }

        attachCommonBottomNav(findViewById(R.id.bottom_navigation), R.id.item_1)
    }
}