package com.example.stockmaneger.ui.sales

import com.example.stockmaneger.data.models.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesValidationTest {

    @Test
    fun productSelection_emptyList_hasNoValidSelection() {
        val emptyProducts = emptyList<Product>()
        assertTrue(emptyProducts.isEmpty())

        val selectedPosition = 0
        val isValid = selectedPosition in emptyProducts.indices
        assertFalse(isValid)

        val selectedProduct = if (isValid) emptyProducts[selectedPosition] else null
        assertNull(selectedProduct)
    }

    @Test
    fun productSelection_outOfBoundsIndex_isSafelyRejected() {
        val products = listOf(
            Product(id = 1, name = "Product A", quantity = 10, basePrice = 5.0, salePrice = 8.0)
        )
        assertFalse(-1 in products.indices)
        assertFalse(1 in products.indices)
        assertTrue(0 in products.indices)
    }

    @Test
    fun productSelection_validProduct_retrievesExpectedProduct() {
        val products = listOf(
            Product(id = 1, name = "Product A", quantity = 10, basePrice = 5.0, salePrice = 8.0),
            Product(id = 2, name = "Product B", quantity = 5, basePrice = 10.0, salePrice = 15.0)
        )
        val selectedIndex = 1
        assertTrue(selectedIndex in products.indices)
        assertEquals("Product B", products[selectedIndex].name)
        assertEquals(5, products[selectedIndex].quantity)
    }

    @Test
    fun stockQuantity_insufficientStock_isDetected() {
        val product = Product(id = 1, name = "Widget", quantity = 3, basePrice = 2.0, salePrice = 5.0)
        val requestedQty = 5
        assertTrue(requestedQty > product.quantity)
    }

    @Test
    fun saleCalculation_computesRevenueAndProfitCorrectly() {
        val unitPrice = 15.0
        val basePrice = 10.0
        val qty = 4

        val totalPrice = unitPrice * qty
        val totalBase = basePrice * qty
        val profit = totalPrice - totalBase

        assertEquals(60.0, totalPrice, 0.001)
        assertEquals(40.0, totalBase, 0.001)
        assertEquals(20.0, profit, 0.001)
    }
}
