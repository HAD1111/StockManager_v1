package com.example.stockmaneger.data.models


data class Sale(
    val id: Int = 0,
    val customerId: Int,
    val productId: Int,
    val customerName: String = "",
    val customerPhone: String = "",
    val productName: String = "",
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val basePrice: Double,
    val profit: Double,
    val date: String,
    val paymentMethod: String
)