package com.example.stockmaneger.data.models

data class Sale(
    val id: Int = 0,
    val customerId: Int? = null,
    val date: String,
    val totalAmount: Double,
    val totalCost: Double,
    val notes: String = "",
    // Aggregated and display fields
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val itemCount: Int = 0,
    val paidAmount: Double = 0.0,
    val paymentMethod: String = "Cash"
) {
    // Backwards-compatible secondary constructor for callers supplying remainingDebt and profit
    constructor(
        id: Int = 0,
        customerId: Int? = null,
        date: String,
        totalAmount: Double,
        totalCost: Double,
        notes: String = "",
        customerName: String = "Walk-in Customer",
        customerPhone: String = "",
        itemCount: Int = 0,
        paidAmount: Double = 0.0,
        remainingDebt: Double,
        profit: Double,
        paymentMethod: String = "Cash"
    ) : this(
        id = id,
        customerId = customerId,
        date = date,
        totalAmount = totalAmount,
        totalCost = totalCost,
        notes = notes,
        customerName = customerName,
        customerPhone = customerPhone,
        itemCount = itemCount,
        paidAmount = paidAmount,
        paymentMethod = paymentMethod
    )

    val remainingDebt: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
    val profit: Double get() = (paidAmount - totalCost).coerceAtLeast(0.0)
    val saleTotal: Double get() = totalAmount
    val totalPaid: Double get() = paidAmount
    val remainingBalance: Double get() = remainingDebt
    val amountDue: Double get() = remainingDebt
    val recognizedRevenue: Double get() = minOf(totalAmount, paidAmount)
    val recognizedProfit: Double get() = profit
    val grossProfit: Double get() = profit
    val isPaidInFull: Boolean get() = remainingDebt <= 0.005
    val isOverpaid: Boolean get() = paidAmount > totalAmount + 0.005
    val isUnpaid: Boolean get() = paidAmount <= 0.005
    val isPartial: Boolean get() = paidAmount > 0.005 && remainingDebt > 0.005
}
