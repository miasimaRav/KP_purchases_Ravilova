package com.example.kp_purchases_ravilova


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Цепляет единую логику к BottomNavigationView.
 * currentItemId – id пункта, который СЕЙЧАС активен (чтобы не пересоздавать ту же Activity)
 */
fun AppCompatActivity.attachCommonBottomNav(
    bottomNav: BottomNavigationView,
    currentItemId: Int
) {
    bottomNav.selectedItemId = currentItemId
    bottomNav.setOnItemSelectedListener { item ->
        if (item.itemId == currentItemId) return@setOnItemSelectedListener true

        when (item.itemId) {
            R.id.item_1 -> startActivity(Intent(this, HomeActivity::class.java))
            R.id.item_2 -> startActivity(Intent(this, PurchasesActivity::class.java))
            R.id.item_3 -> startActivity(Intent(this, ProfileActivity::class.java))
        }
        finish()
        true
    }
}