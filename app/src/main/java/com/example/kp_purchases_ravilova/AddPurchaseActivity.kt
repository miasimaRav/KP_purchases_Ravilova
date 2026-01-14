package com.example.kp_purchases_ravilova

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kp_purchases_ravilova.data.CategoryDto
import com.example.kp_purchases_ravilova.data.Client
import com.example.kp_purchases_ravilova.data.ProductDto
import com.example.kp_purchases_ravilova.data.PurchaseDto
import com.example.kp_purchases_ravilova.data.ShopDto
import com.example.kp_purchases_ravilova.databinding.ActivityAddPurchaseBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class AddPurchaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPurchaseBinding
    private var currentUserId = 0
    private var productsList = mutableListOf<ProductDto>()
    private var shopsList = mutableListOf<ShopDto>()
    private lateinit var productAdapter: ArrayAdapter<String>
    private lateinit var shopAdapter: ArrayAdapter<String>
    private var categoriesList = mutableListOf<CategoryDto>()
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = AuthPreferences.getUserId(this)
        setupUI()
        loadProducts()
        loadShops()
        loadCategories()
    }

    private fun setupUI() {
        setupDatePicker()
        setupButtons()
    }

    private fun setupDatePicker() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.dateEt.setText(today)

        binding.dateEt.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                    binding.dateEt.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                val response = Client.productApi.getAllProducts()
                if (response.success == true) {
                    productsList = response.data as? MutableList<ProductDto> ?: mutableListOf()
                    setupProductAutocomplete()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPurchaseActivity, "Ошибка загрузки продуктов", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadShops() {
        lifecycleScope.launch {
            try {
                val response = Client.shopApi.getAllShops()
                if (response.success == true && response.data != null) {
                    shopsList = response.data.toMutableList()
                    setupShopAutocomplete()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPurchaseActivity, "Ошибка загрузки магазинов: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun createShopAndRefresh(name: String) {
        lifecycleScope.launch {
            val newShop = ShopDto(name = name)
            val response = Client.shopApi.createShop(newShop)
            if (response.success == true) {
                shopsList.add(response.data as ShopDto)
                updateShopAdapter()
                binding.shopAutoComplete.setText(name, false)
            }
        }
    }

    private fun showAddShopDialog() {
        val input = EditText(this).apply {
            hint = "Название магазина"
            setPadding(48, 48, 48, 48)
        }

        AlertDialog.Builder(this)
            .setTitle("Новый магазин")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createShopAndRefresh(name)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }



    private fun setupProductAutocomplete() {
        val items = mutableListOf<String>()
        items.add("Добавить новый продукт")
        items.addAll(productsList.map { it.name })

        productAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.productAutoComplete.setAdapter(productAdapter)

//        binding.productAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//            if (position == 0) { // "Добавить новый продукт"
//                showAddProductDialog()
//            }
//        }

        // Фильтрация по вводу
        binding.productAutoComplete.setOnItemSelectedListener(null)
        binding.productAutoComplete.setThreshold(1)
    }

    private fun setupShopAutocomplete() {
        val items = mutableListOf<String>()
        items.add("Добавить новый магазин")
        items.addAll(shopsList.map { it.name })

        shopAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.shopAutoComplete.setAdapter(shopAdapter)

//        binding.shopAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//            if (position == 0) {
//                showAddShopDialog()
//            }
//        }
    }

    private fun showAddProductDialog() {
        val input = EditText(this).apply {
            hint = "Название продукта"
            setPadding(48, 48, 48, 48)
        }

        AlertDialog.Builder(this)
            .setTitle("Новый продукт")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createProductAndRefresh(name)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createProductAndRefresh(name: String) {
        lifecycleScope.launch {
            try {
                val newProduct = ProductDto(name = name, categoryId = 1)
                val response = Client.productApi.createProduct(newProduct)

                if (response.success == true) {
                    productsList.add(response.data as ProductDto)
                    updateProductAdapter()
                    binding.productAutoComplete.setText(name, false)
                    Toast.makeText(this@AddPurchaseActivity, "Продукт добавлен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPurchaseActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProductAdapter() {
        val items = mutableListOf<String>()
        items.add("Добавить новый продукт")
        items.addAll(productsList.map { it.name })
        productAdapter.clear()
        productAdapter.addAll(items)
        productAdapter.notifyDataSetChanged()
    }

    private fun updateShopAdapter() {
        val items = mutableListOf<String>()
        items.add("Добавить новый магазин")
        items.addAll(shopsList.map { it.name })
        shopAdapter.clear()
        shopAdapter.addAll(items)
        shopAdapter.notifyDataSetChanged()
    }

    private fun setupButtons() {
        binding.saveBtn.setOnClickListener {
            val productName = binding.productAutoComplete.text.toString().trim()
            val shopName = binding.shopAutoComplete.text.toString().trim()
            val cost = binding.priceEt.text.toString().trim()
            val date = binding.dateEt.text.toString().trim()
            val category= binding.categoryAutoComplete.text.toString().trim()

            if (productName.isEmpty() || shopName.isEmpty() || cost.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            savePurchase(productName, shopName, cost, date,category)
        }

        binding.backBtn.setOnClickListener { finish() }
    }

    private fun savePurchase(productName: String, shopName: String, cost: String, date: String,
                             category:String) {
        val purchaseDto = PurchaseDto(
            userId = currentUserId,
            productName = productName, // Используется для создания нового продукта
            categoryName = category,
            productId = null, // null = будет создан новый
            shopId = 1, // Сервер сам найдет/создаст магазин
            cost = cost,
            quantity = "1",
            purchaseDate = date,
            note = "",
            isCompleted = false
        )

        lifecycleScope.launch {
            try {
                val response = Client.purchaseApi.createPurchase(purchaseDto)
                if (response.success == true) {
                    Toast.makeText(this@AddPurchaseActivity, "Покупка сохранена", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddPurchaseActivity, response.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPurchaseActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response = Client.categoryApi.getAllCategories()
                if (response.success == true && response.data != null) {
                    categoriesList = response.data.toMutableList()
                    setupCategoryAutocomplete()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPurchaseActivity, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupCategoryAutocomplete() {
        val items = mutableListOf<String>()
        items.addAll(categoriesList.map { it.name })  // имена категорий

        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.categoryAutoComplete.setAdapter(categoryAdapter)
    }



}
