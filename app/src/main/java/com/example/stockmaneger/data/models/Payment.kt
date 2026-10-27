package com.example.stockmaneger.data.models

data class Payment(
    val id: Int = 0,
    val saleId: Int,
    val amount: Double,
    val paymentMethod: String,
    val paymentDate: String,
    val note: String = ""
)
