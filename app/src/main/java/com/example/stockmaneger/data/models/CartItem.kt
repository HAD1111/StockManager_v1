package com.example.stockmaneger.data.models

data class CartItem(
    val product: Product,
    var quantity: Int = 1,
    var customUnitPrice: Double = product.salePrice
) {
    val productId: Int get() = product.id
    val productName: String get() = product.name
    val unitCost: Double get() = product.basePrice
    val subtotal: Double get() = customUnitPrice * quantity
    val totalCost: Double get() = unitCost * quantity
    val profit: Double get() = subtotal - totalCost
}
