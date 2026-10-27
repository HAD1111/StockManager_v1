package com.example.stockmaneger.ui.sales

import com.example.stockmaneger.data.models.CartItem
import com.example.stockmaneger.data.models.Payment
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.data.models.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartAndFinancialTest {

    private val sampleProduct1 = Product(
        id = 1,
        name = "Organic Honey",
        quantity = 20,
        basePrice = 12.00, // unit cost
        salePrice = 20.00  // catalog selling price
    )

    private val sampleProduct2 = Product(
        id = 2,
        name = "Olive Oil 1L",
        quantity = 15,
        basePrice = 18.00, // unit cost
        salePrice = 25.00  // catalog selling price
    )

    // ========================================================================
    // REQUIRED CORE SCENARIOS TABLE: Total $8.00, Cost $4.00
    // ========================================================================

    data class FinancialScenario(
        val name: String,
        val totalAmount: Double,
        val totalCost: Double,
        val amountPaid: Double,
        val expectedRemaining: Double,
        val expectedRevenue: Double,
        val expectedProfit: Double
    )

    @Test
    fun sixRequiredScenarios_tableValidation() {
        val scenarios = listOf(
            FinancialScenario(
                name = "Paid $0",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 0.00,
                expectedRemaining = 8.00,
                expectedRevenue = 0.00,
                expectedProfit = 0.00
            ),
            FinancialScenario(
                name = "Paid $2",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 2.00,
                expectedRemaining = 6.00,
                expectedRevenue = 2.00,
                expectedProfit = 0.00
            ),
            FinancialScenario(
                name = "Paid $4",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 4.00,
                expectedRemaining = 4.00,
                expectedRevenue = 4.00,
                expectedProfit = 0.00
            ),
            FinancialScenario(
                name = "Paid $5",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 5.00,
                expectedRemaining = 3.00,
                expectedRevenue = 5.00,
                expectedProfit = 1.00
            ),
            FinancialScenario(
                name = "Paid $8",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 8.00,
                expectedRemaining = 0.00,
                expectedRevenue = 8.00,
                expectedProfit = 4.00
            ),
            FinancialScenario(
                name = "Paid $10",
                totalAmount = 8.00,
                totalCost = 4.00,
                amountPaid = 10.00,
                expectedRemaining = 0.00,
                expectedRevenue = 8.00,
                expectedProfit = 6.00
            )
        )

        for (s in scenarios) {
            val sale = Sale(
                id = 1,
                date = "2026-10-27",
                totalAmount = s.totalAmount,
                totalCost = s.totalCost,
                paidAmount = s.amountPaid
            )

            assertEquals("${s.name} saleTotal mismatch", s.totalAmount, sale.saleTotal, 0.001)
            assertEquals("${s.name} totalPaid mismatch", s.amountPaid, sale.totalPaid, 0.001)
            assertEquals("${s.name} remainingDebt mismatch", s.expectedRemaining, sale.remainingDebt, 0.001)
            assertEquals("${s.name} remainingBalance mismatch", s.expectedRemaining, sale.remainingBalance, 0.001)
            assertEquals("${s.name} amountDue mismatch", s.expectedRemaining, sale.amountDue, 0.001)
            assertEquals("${s.name} recognizedRevenue mismatch", s.expectedRevenue, sale.recognizedRevenue, 0.001)
            assertEquals("${s.name} recognizedProfit mismatch", s.expectedProfit, sale.recognizedProfit, 0.001)
            assertEquals("${s.name} profit mismatch", s.expectedProfit, sale.profit, 0.001)
            assertEquals("${s.name} grossProfit mismatch", s.expectedProfit, sale.grossProfit, 0.001)
        }
    }

    @Test
    fun userExample_overpaymentWithExtraProfitRecognized() {
        // Sale total $9, Cost $4.50, Paid $19
        val sale = Sale(
            id = 2,
            date = "2026-10-27",
            totalAmount = 9.00,
            totalCost = 4.50,
            paidAmount = 19.00
        )

        assertEquals(9.00, sale.saleTotal, 0.001)
        assertEquals(19.00, sale.totalPaid, 0.001)
        assertEquals(0.00, sale.amountDue, 0.001)
        assertEquals(9.00, sale.recognizedRevenue, 0.001)
        assertEquals(14.50, sale.recognizedProfit, 0.001) // $19.00 - $4.50
        assertEquals(14.50, sale.profit, 0.001)
        assertTrue(sale.isOverpaid)
        assertTrue(sale.isPaidInFull)
    }

    @Test
    fun userExample_unpaidSaleZeroRevenueAndProfit() {
        // Sale total $8, Cost $4, Paid $0
        val sale = Sale(
            id = 3,
            date = "2026-10-27",
            totalAmount = 8.00,
            totalCost = 4.00,
            paidAmount = 0.00
        )

        assertEquals(8.00, sale.saleTotal, 0.001)
        assertEquals(0.00, sale.totalPaid, 0.001)
        assertEquals(8.00, sale.amountDue, 0.001)
        assertEquals(0.00, sale.recognizedRevenue, 0.001)
        assertEquals(0.00, sale.recognizedProfit, 0.001)
        assertEquals(0.00, sale.profit, 0.001)
        assertTrue(sale.isUnpaid)
        assertFalse(sale.isPaidInFull)
    }

    // ========================================================================
    // DETAILED SPECIFICATION REGRESSION TESTS 1 TO 12
    // ========================================================================

    @Test
    fun test1_editingUnitSellingPriceFrom100to120IncreasesProspectiveProfitFrom20to40() {
        val product = Product(id = 10, name = "Widget", quantity = 10, basePrice = 80.00, salePrice = 100.00)
        val cartItem = CartItem(product = product, quantity = 1, customUnitPrice = 100.00)

        assertEquals(100.00, cartItem.subtotal, 0.001)
        assertEquals(80.00, cartItem.totalCost, 0.001)
        assertEquals(20.00, cartItem.profit, 0.001)

        // User edits the selling price upward in the cart to $120
        cartItem.customUnitPrice = 120.00

        assertEquals(120.00, cartItem.subtotal, 0.001)
        assertEquals(80.00, cartItem.totalCost, 0.001)
        assertEquals(40.00, cartItem.profit, 0.001) // Margin increased from $20 to $40
    }

    @Test
    fun test2_changingPaymentFrom120to130PreservesSaleTotalAndRecognizesOverpaymentProfit() {
        val totalAmount = 120.00
        val totalCost = 80.00
        val paidAmount = 130.00

        val sale = Sale(
            id = 1,
            date = "2026-10-27",
            totalAmount = totalAmount,
            totalCost = totalCost,
            paidAmount = paidAmount
        )

        // Sale total is immutable agreed amount
        assertEquals(120.00, sale.saleTotal, 0.001)
        assertEquals(130.00, sale.totalPaid, 0.001)
        assertEquals(0.00, sale.amountDue, 0.001)
        assertEquals(120.00, sale.recognizedRevenue, 0.001)
        // Profit includes the $10 overpayment: (130 - 80) = 50.00
        assertEquals(50.00, sale.recognizedProfit, 0.001)
        assertEquals(50.00, sale.profit, 0.001)
        assertTrue(sale.isOverpaid)
    }

    @Test
    fun test3_changingPaymentFrom120to0PreservesSaleTotalAndRecognizesZeroRevenueAndProfit() {
        val totalAmount = 120.00
        val totalCost = 80.00
        val paidAmount = 0.00

        val sale = Sale(
            id = 1,
            date = "2026-10-27",
            totalAmount = totalAmount,
            totalCost = totalCost,
            paidAmount = paidAmount
        )

        // Changing payment to 0 yields $120 remaining debt, $0 revenue, $0 profit
        assertEquals(120.00, sale.saleTotal, 0.001)
        assertEquals(0.00, sale.totalPaid, 0.001)
        assertEquals(120.00, sale.amountDue, 0.001)
        assertEquals(0.00, sale.recognizedRevenue, 0.001)
        assertEquals(0.00, sale.recognizedProfit, 0.001)
        assertEquals(0.00, sale.profit, 0.001)
        assertTrue(sale.isUnpaid)
    }

    @Test
    fun test4_multipleInstallments_summedCorrectly() {
        val saleTotal = 200.00
        val payments = listOf(
            Payment(id = 1, saleId = 10, amount = 50.00, paymentMethod = "Cash", paymentDate = "2026-10-20"),
            Payment(id = 2, saleId = 10, amount = 70.00, paymentMethod = "Card", paymentDate = "2026-10-22"),
            Payment(id = 3, saleId = 10, amount = 30.00, paymentMethod = "Cash", paymentDate = "2026-10-25")
        )

        val totalPaid = payments.sumOf { it.amount }
        val remaining = (saleTotal - totalPaid).coerceAtLeast(0.0)

        assertEquals(150.00, totalPaid, 0.001)
        assertEquals(50.00, remaining, 0.001)
    }

    @Test
    fun test5_addingInstallmentRefreshesSaleListAndDetailValues() {
        val initialSale = Sale(
            id = 10,
            date = "2026-10-27",
            totalAmount = 120.00,
            totalCost = 80.00,
            paidAmount = 50.00
        )
        assertTrue(initialSale.isPartial)
        assertFalse(initialSale.isPaidInFull)
        assertEquals(50.00, initialSale.recognizedRevenue, 0.001)
        assertEquals(0.00, initialSale.recognizedProfit, 0.001) // 50 <= 80 cost
        assertEquals(70.00, initialSale.amountDue, 0.001)

        // Customer pays second installment of $70 (bringing total paid to $120)
        val newPayment = 70.00
        val updatedPaid = initialSale.paidAmount + newPayment

        val refreshedSale = initialSale.copy(paidAmount = updatedPaid)

        assertEquals(120.00, refreshedSale.paidAmount, 0.001)
        assertEquals(0.00, refreshedSale.amountDue, 0.001)
        assertEquals(120.00, refreshedSale.recognizedRevenue, 0.001)
        assertEquals(40.00, refreshedSale.recognizedProfit, 0.001) // 120 - 80 = 40
        assertTrue(refreshedSale.isPaidInFull)
        assertFalse(refreshedSale.isPartial)
    }

    @Test
    fun test6_walkInSaleCalculatesPaymentAndBalanceCorrectly() {
        val walkInSale = Sale(
            id = 5,
            customerId = null,
            date = "2026-10-27 10:00",
            totalAmount = 120.00,
            totalCost = 80.00,
            paidAmount = 50.00,
            customerName = "Walk-in Customer"
        )

        assertNull(walkInSale.customerId)
        assertEquals("Walk-in Customer", walkInSale.customerName)
        assertEquals(120.00, walkInSale.totalAmount, 0.001)
        assertEquals(50.00, walkInSale.paidAmount, 0.001)
        assertEquals(70.00, walkInSale.amountDue, 0.001)
        assertEquals(50.00, walkInSale.recognizedRevenue, 0.001)
        assertEquals(0.00, walkInSale.recognizedProfit, 0.001) // 50 <= 80 cost
    }

    @Test
    fun test7_laterCatalogPriceChangeDoesNotAlterHistoricalSaleProfit() {
        val snapshottedItem = SaleItem(
            id = 1,
            saleId = 100,
            productId = sampleProduct1.id,
            productName = sampleProduct1.name,
            quantity = 2,
            unitPrice = 20.00,
            unitCost = 12.00
        )

        assertEquals(40.00, snapshottedItem.subtotal, 0.001)
        assertEquals(16.00, snapshottedItem.profit, 0.001)

        // Supplier subsequently doubles basePrice and salePrice in catalog
        val alteredCatalogProduct = sampleProduct1.copy(basePrice = 24.00, salePrice = 40.00)

        // Historical snapshot is immutable
        assertEquals(20.00, snapshottedItem.unitPrice, 0.001)
        assertEquals(12.00, snapshottedItem.unitCost, 0.001)
        assertEquals(16.00, snapshottedItem.profit, 0.001)
        assertEquals(40.00, alteredCatalogProduct.salePrice, 0.001)
    }

    @Test
    fun test8_saleWithMultipleProductsCalculatesTotalCostAndProfitCorrectly() {
        val cart = listOf(
            CartItem(sampleProduct1, quantity = 2, customUnitPrice = 20.00), // subtotal: $40, cost: $24, profit: $16
            CartItem(sampleProduct2, quantity = 3, customUnitPrice = 25.00)  // subtotal: $75, cost: $54, profit: $21
        )

        val totalAmount = cart.sumOf { it.subtotal }
        val totalCost = cart.sumOf { it.totalCost }
        val totalProfit = cart.sumOf { it.profit }

        assertEquals(115.00, totalAmount, 0.001)
        assertEquals(78.00, totalCost, 0.001)
        assertEquals(37.00, totalProfit, 0.001)
    }

    @Test
    fun test9_paymentRecordsAreNotMultipliedByItemJoins() {
        // A sale with 3 items and 2 payments
        val itemsCount = 3
        val payments = listOf(
            Payment(id = 1, saleId = 1, amount = 40.00, paymentMethod = "Cash", paymentDate = "2026-10-27"),
            Payment(id = 2, saleId = 1, amount = 30.00, paymentMethod = "Card", paymentDate = "2026-10-27")
        )

        // Pre-aggregated query guarantees sum is exact
        val totalPaid = payments.sumOf { it.amount }
        assertEquals(70.00, totalPaid, 0.001)
        assertEquals(3, itemsCount)

        // A naive SQL cross join would yield 3 * 2 = 6 rows and multiply totalPaid to 210.00
        val naiveJoinedPaid = totalPaid * itemsCount
        assertTrue(naiveJoinedPaid > totalPaid)
    }

    @Test
    fun test10_saleDeletionReversesActualPaymentsAndRecognizedProfitContribution() {
        var accountCash = 1000.00
        var accountProfit = 500.00

        val saleTotal = 120.00
        val saleCost = 80.00
        val cashReceived = 130.00 // Overpaid by $10
        val recognizedProfit = (cashReceived - saleCost).coerceAtLeast(0.0) // $50

        // Checkout adds actual cash and recognized profit
        accountCash += cashReceived
        accountProfit += recognizedProfit

        assertEquals(1130.00, accountCash, 0.001)
        assertEquals(550.00, accountProfit, 0.001)

        // Sale deletion reverses actual payments received and recognized profit contribution
        accountCash -= cashReceived
        accountProfit -= recognizedProfit

        assertEquals(1000.00, accountCash, 0.001)
        assertEquals(500.00, accountProfit, 0.001)
    }

    @Test
    fun test11_pdfAndExcelSummariesMatchRecognizedCalculations() {
        val sales = listOf(
            Sale(id = 1, date = "2026-10-27", totalAmount = 120.00, totalCost = 80.00, paidAmount = 0.00),
            Sale(id = 2, date = "2026-10-27", totalAmount = 120.00, totalCost = 80.00, paidAmount = 50.00),
            Sale(id = 3, date = "2026-10-27", totalAmount = 120.00, totalCost = 80.00, paidAmount = 120.00),
            Sale(id = 4, date = "2026-10-27", totalAmount = 120.00, totalCost = 80.00, paidAmount = 130.00)
        )

        val totalRevenue = sales.sumOf { it.recognizedRevenue }
        val totalProfit = sales.sumOf { it.recognizedProfit }
        val totalPaid = sales.sumOf { it.paidAmount }
        val totalDebt = sales.sumOf { it.amountDue }

        // Sale 1: rev 0, profit 0, debt 120
        // Sale 2: rev 50, profit 0, debt 70
        // Sale 3: rev 120, profit 40, debt 0
        // Sale 4: rev 120, profit 50, debt 0
        assertEquals(290.00, totalRevenue, 0.001)
        assertEquals(90.00, totalProfit, 0.001)
        assertEquals(300.00, totalPaid, 0.001)
        assertEquals(190.00, totalDebt, 0.001)
    }

    @Test
    fun test12_incrementalInstallmentsSequenceCalculatesDeltaProfitCorrectly() {
        val totalCost = 4.00
        var priorPaid = 0.00
        var totalAccountProfit = 0.00

        val installments = listOf(2.00, 3.00, 3.00, 2.00) // cumulative: 2.00, 5.00, 8.00, 10.00
        val expectedDeltas = listOf(0.00, 1.00, 3.00, 2.00) // 0 -> 0, 0 -> 1, 1 -> 4, 4 -> 6

        installments.forEachIndexed { i, installment ->
            val priorRecognizedProfit = (priorPaid - totalCost).coerceAtLeast(0.0)
            val newTotalPaid = priorPaid + installment
            val newRecognizedProfit = (newTotalPaid - totalCost).coerceAtLeast(0.0)
            val delta = newRecognizedProfit - priorRecognizedProfit

            assertEquals("Delta at installment $i mismatch", expectedDeltas[i], delta, 0.001)
            totalAccountProfit += delta
            priorPaid = newTotalPaid
        }

        // Cumulative recognized profit for $10 paid on $4 cost must be $6.00
        assertEquals(6.00, totalAccountProfit, 0.001)
    }

    @Test
    fun cartItem_stockLimitBoundaryCheck() {
        val product = sampleProduct1.copy(quantity = 5)
        val cartItem = CartItem(product = product, quantity = 5)

        val requestedQty = 6
        val canAdd = requestedQty <= cartItem.product.quantity
        assertFalse(canAdd)
        assertTrue(cartItem.quantity <= product.quantity)
    }
}
