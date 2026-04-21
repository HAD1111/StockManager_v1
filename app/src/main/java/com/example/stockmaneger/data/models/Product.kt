package com.example.stockmaneger.data.models

data class Product(
    val id: Int = 0,
    val name: String,
    val quantity: Int,
    val basePrice: Double,
    val salePrice: Double
)