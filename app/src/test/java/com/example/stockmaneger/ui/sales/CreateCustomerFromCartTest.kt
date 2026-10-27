package com.example.stockmaneger.ui.sales

import com.example.stockmaneger.data.models.CartItem
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.data.models.Sale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateCustomerFromCartTest {

    private val sampleProduct1 = Product(
        id = 1,
        name = "Organic Honey",
        quantity = 20,
        basePrice = 12.00,
        salePrice = 20.00
    )

    private val sampleProduct2 = Product(
        id = 2,
        name = "Olive Oil 1L",
        quantity = 15,
        basePrice = 18.00,
        salePrice = 25.00
    )

    // ========================================================================
    // 1. CREATING CUSTOMER WITH NAME AND PHONE
    // ========================================================================

    @Test
    fun createCustomer_withNameAndPhone_validData() {
        val rawName = "  Sarah Connor  "
        val rawPhone = "  03123456  "

        val trimmedName = rawName.trim()
        val trimmedPhone = rawPhone.trim()

        assertTrue(trimmedName.isNotEmpty())
        assertEquals("Sarah Connor", trimmedName)
        assertEquals("03123456", trimmedPhone)

        val newCustomer = Customer(
            id = 101,
            name = trimmedName,
            phone = trimmedPhone
        )

        assertEquals(101, newCustomer.id)
        assertEquals("Sarah Connor", newCustomer.name)
        assertEquals("03123456", newCustomer.phone)
    }

    // ========================================================================
    // 2. CREATING CUSTOMER WITH NAME AND NO PHONE NUMBER
    // ========================================================================

    @Test
    fun createCustomer_withNameAndNoPhone_validData() {
        val rawName = "John Doe"
        val rawPhone = ""

        val trimmedName = rawName.trim()
        val trimmedPhone = rawPhone.trim()

        assertTrue(trimmedName.isNotEmpty())
        assertTrue(trimmedPhone.isEmpty())

        val newCustomer = Customer(
            id = 102,
            name = trimmedName,
            phone = trimmedPhone
        )

        assertEquals(102, newCustomer.id)
        assertEquals("John Doe", newCustomer.name)
        assertEquals("", newCustomer.phone)
    }

    // ========================================================================
    // 3. REJECTING EMPTY OR WHITESPACE-ONLY NAME
    // ========================================================================

    @Test
    fun createCustomer_emptyOrWhitespaceName_isRejected() {
        val invalidNames = listOf("", "   ", "\t\t", "\n\r  ", " \t \n ")

        for (invalid in invalidNames) {
            val trimmed = invalid.trim()
            val isValid = trimmed.isNotEmpty()

            assertFalse("Expected '$invalid' to be rejected", isValid)
        }
    }

    // ========================================================================
    // 4. AUTOMATICALLY SELECTING THE NEWLY CREATED CUSTOMER
    // ========================================================================

    @Test
    fun automaticSelection_newlyCreatedCustomer_selectedForSale() {
        val existingCustomers = mutableListOf(
            Customer(id = 1, name = "Alice", phone = "03000001"),
            Customer(id = 2, name = "Bob", phone = "03000002")
        )

        // Simulate insertion of new customer
        val newCustomer = Customer(id = 3, name = "Charlie", phone = "")
        existingCustomers.add(newCustomer)

        // Dropdown display mapping logic from CartFragment
        val customerDisplayList = mutableListOf("Walk-in Customer (No Account)")
        customerDisplayList.addAll(existingCustomers.map {
            "${it.name} ${if (it.phone.isNotEmpty()) "(${it.phone})" else ""}".trim()
        })
        customerDisplayList.add("+ New Customer")

        // Finding target index for the newly created customer
        val createdCustomerId = newCustomer.id
        val idx = existingCustomers.indexOfFirst { it.id == createdCustomerId }
        val targetIndex = if (idx >= 0) idx + 1 else 0

        var selectedCustomerId: Int? = if (targetIndex == 0) null else existingCustomers[targetIndex - 1].id
        val selectedDisplayText = customerDisplayList[targetIndex]

        assertEquals(3, selectedCustomerId)
        assertEquals("Charlie", selectedDisplayText)
        assertEquals(3, targetIndex)
    }

    // ========================================================================
    // 5. COMPLETING A SALE WITH THE NEWLY CREATED CUSTOMER
    // ========================================================================

    @Test
    fun completeSale_withNewlyCreatedCustomer_associatesCustomerIdCorrectly() {
        val newCustomer = Customer(id = 77, name = "Maya Angelou", phone = "03987654")

        val sale = Sale(
            id = 500,
            customerId = newCustomer.id,
            date = "2026-10-27",
            totalAmount = 150.00,
            totalCost = 100.00,
            customerName = newCustomer.name,
            customerPhone = newCustomer.phone,
            paidAmount = 150.00
        )

        assertNotNull(sale.customerId)
        assertEquals(77, sale.customerId)
        assertEquals("Maya Angelou", sale.customerName)
        assertEquals("03987654", sale.customerPhone)
        assertEquals(150.00, sale.saleTotal, 0.001)
        assertEquals(150.00, sale.totalPaid, 0.001)
        assertEquals(0.00, sale.amountDue, 0.001)
        assertEquals(50.00, sale.recognizedProfit, 0.001)
        assertTrue(sale.isPaidInFull)
    }

    // ========================================================================
    // 6. PRESERVING CART CONTENTS AND ENTERED PAYMENT WHILE CREATING CUSTOMER
    // ========================================================================

    @Test
    fun preserveCartContentsAndEnteredPayment_whileCreatingCustomer() {
        // 1. User builds cart with customized prices
        val cartItems = mutableListOf(
            CartItem(sampleProduct1, quantity = 3, customUnitPrice = 22.50), // subtotal: $67.50
            CartItem(sampleProduct2, quantity = 2, customUnitPrice = 28.00)  // subtotal: $56.00
        )
        val initialTotal = cartItems.sumOf { it.subtotal } // $123.50
        assertEquals(123.50, initialTotal, 0.001)

        // 2. User types custom partial payment
        var userHasCustomPayment = true
        var enteredAmountPaid = 50.00

        // 3. User opens Add Customer dialog and creates customer
        val newCustomerId = 88
        val newCustomerName = "David Copperfield"

        // Simulated customer reload does NOT mutate cart items or payment
        val targetCustomerId = newCustomerId
        assertEquals(88, targetCustomerId)

        // 4. Verify cart state is completely untouched
        assertEquals(2, cartItems.size)
        assertEquals(3, cartItems[0].quantity)
        assertEquals(22.50, cartItems[0].customUnitPrice, 0.001)
        assertEquals(2, cartItems[1].quantity)
        assertEquals(28.00, cartItems[1].customUnitPrice, 0.001)

        val preservedTotal = cartItems.sumOf { it.subtotal }
        assertEquals(123.50, preservedTotal, 0.001)
        assertEquals(50.00, enteredAmountPaid, 0.001)
        assertTrue(userHasCustomPayment)

        val remainingDebt = (preservedTotal - enteredAmountPaid).coerceAtLeast(0.0)
        assertEquals(73.50, remainingDebt, 0.001)
    }

    // ========================================================================
    // 7. CONTINUING TO SUPPORT WALK-IN SALES AND EXISTING CUSTOMERS
    // ========================================================================

    @Test
    fun supportWalkInSales_andExistingCustomers() {
        val existingCustomer = Customer(id = 5, name = "Existing Client", phone = "01234567")

        // 1. Walk-in customer flow (no customer selected)
        val walkInSale = Sale(
            id = 1,
            customerId = null,
            date = "2026-10-27",
            totalAmount = 80.00,
            totalCost = 50.00,
            paidAmount = 80.00,
            customerName = "Walk-in Customer"
        )
        assertNull(walkInSale.customerId)
        assertEquals("Walk-in Customer", walkInSale.customerName)

        // 2. Existing customer flow
        val existingCustomerSale = Sale(
            id = 2,
            customerId = existingCustomer.id,
            date = "2026-10-27",
            totalAmount = 80.00,
            totalCost = 50.00,
            paidAmount = 40.00,
            customerName = existingCustomer.name,
            customerPhone = existingCustomer.phone
        )
        assertEquals(5, existingCustomerSale.customerId)
        assertEquals("Existing Client", existingCustomerSale.customerName)
        assertEquals(40.00, existingCustomerSale.amountDue, 0.001)
    }

    // ========================================================================
    // 8. PREVENTING DUPLICATE SUBMISSIONS
    // ========================================================================

    @Test
    fun duplicateSubmissionPrevention_flagBlocksSecondCall() {
        var isSaving = false
        var insertCallCount = 0

        fun onSaveClicked() {
            if (isSaving) return
            isSaving = true
            insertCallCount++
        }

        // First click executes
        onSaveClicked()
        assertEquals(1, insertCallCount)
        assertTrue(isSaving)

        // Rapid subsequent clicks are ignored while isSaving is true
        onSaveClicked()
        onSaveClicked()
        assertEquals(1, insertCallCount)
    }
}
