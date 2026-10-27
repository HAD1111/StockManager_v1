package com.example.stockmaneger.data.models

data class SaleItem(
    val id: Int = 0,
    val saleId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double = quantity * unitPrice,
    val profit: Double = subtotal - (unitCost * quantity)
)
