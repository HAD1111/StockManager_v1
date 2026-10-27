package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.CartItem
import com.example.stockmaneger.data.models.Payment
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.data.models.SaleItem

class SaleDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val db = dbHelper.writableDatabase

    /**
     * Executes atomic checkout for a multi-item cart.
     * Validates stock under lock, inserts parent sale, inserts sale items with historical snapshots,
     * deducts stock for every item, logs initial payment (if > 0), and updates account money & profit.
     * Rolls back completely if any step fails.
     */
    fun checkout(
        customerId: Int?,
        date: String,
        cartItems: List<CartItem>,
        amountPaid: Double,
        paymentMethod: String,
        notes: String = ""
    ): Long {
        require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
        require(amountPaid >= 0.0) { "Amount paid cannot be negative" }

        db.beginTransaction()
        try {
            // 1. Re-validate stock for every cart item under transaction lock
            for (item in cartItems) {
                val stockCursor = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_PROD_QTY} FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
                    arrayOf(item.productId.toString())
                )
                val currentStock = if (stockCursor.moveToFirst()) stockCursor.getInt(0) else 0
                stockCursor.close()

                if (currentStock < item.quantity) {
                    throw IllegalStateException("Insufficient stock for '${item.productName}'. Available: $currentStock, Requested: ${item.quantity}")
                }
            }

            // 2. Compute sale-level totals from items
            val totalAmount = cartItems.sumOf { it.subtotal }
            val totalCost   = cartItems.sumOf { it.totalCost }
            val initialRecognizedProfit = (amountPaid - totalCost).coerceAtLeast(0.0)

            // 3. Insert parent sale
            val saleValues = ContentValues().apply {
                if (customerId != null) {
                    put(DatabaseHelper.COL_SALE_CUST_ID, customerId)
                } else {
                    putNull(DatabaseHelper.COL_SALE_CUST_ID)
                }
                put(DatabaseHelper.COL_SALE_DATE, date)
                put(DatabaseHelper.COL_SALE_TOTAL, totalAmount)
                put(DatabaseHelper.COL_SALE_COST, totalCost)
                put(DatabaseHelper.COL_SALE_NOTES, notes)
            }
            val saleId = db.insertOrThrow(DatabaseHelper.TABLE_SALES, null, saleValues)

            // 4. Insert each sale item and deduct inventory
            for (item in cartItems) {
                val itemValues = ContentValues().apply {
                    put(DatabaseHelper.COL_ITEM_SALE_ID, saleId)
                    put(DatabaseHelper.COL_ITEM_PROD_ID, item.productId)
                    put(DatabaseHelper.COL_ITEM_PROD_NAME, item.productName)
                    put(DatabaseHelper.COL_ITEM_QTY, item.quantity)
                    put(DatabaseHelper.COL_ITEM_UNIT_PRICE, item.customUnitPrice)
                    put(DatabaseHelper.COL_ITEM_UNIT_COST, item.unitCost)
                    put(DatabaseHelper.COL_ITEM_SUBTOTAL, item.subtotal)
                    put(DatabaseHelper.COL_ITEM_PROFIT, item.profit)
                }
                db.insertOrThrow(DatabaseHelper.TABLE_SALE_ITEMS, null, itemValues)

                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_PRODUCTS} SET ${DatabaseHelper.COL_PROD_QTY} = ${DatabaseHelper.COL_PROD_QTY} - ? WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
                    arrayOf(item.quantity, item.productId)
                )
            }

            // 5. Insert initial payment if amount received > 0
            if (amountPaid > 0.0) {
                val payValues = ContentValues().apply {
                    put(DatabaseHelper.COL_PAY_SALE_ID, saleId)
                    put(DatabaseHelper.COL_PAY_AMOUNT, amountPaid)
                    put(DatabaseHelper.COL_PAY_METHOD, paymentMethod)
                    put(DatabaseHelper.COL_PAY_DATE, date)
                    put(DatabaseHelper.COL_PAY_NOTE, "Initial payment")
                }
                db.insertOrThrow(DatabaseHelper.TABLE_PAYMENTS, null, payValues)

                // Add actual cash received to account balance
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} + ?",
                    arrayOf(amountPaid)
                )
            }

            // 6. Record initial recognized profit to account totalProfit (if any)
            if (initialRecognizedProfit > 0.0) {
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} + ?",
                    arrayOf(initialRecognizedProfit)
                )
            }

            db.setTransactionSuccessful()
            return saleId
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Records a subsequent payment installment against an existing sale.
     * Transactionally adds the payment row, increments liquid account cash,
     * and increments account totalProfit by any newly recognized profit delta.
     */
    fun addPayment(
        saleId: Int,
        amount: Double,
        paymentMethod: String,
        date: String,
        note: String = ""
    ) {
        require(amount > 0.0) { "Payment amount must be greater than zero" }

        db.beginTransaction()
        try {
            // 1. Fetch sale total cost and prior total paid to calculate profit recognition delta
            val saleCursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_SALE_COST} FROM ${DatabaseHelper.TABLE_SALES} WHERE ${DatabaseHelper.COL_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )
            val totalCost = if (saleCursor.moveToFirst()) saleCursor.getDouble(0) else 0.0
            saleCursor.close()

            val payCursor = db.rawQuery(
                "SELECT COALESCE(SUM(${DatabaseHelper.COL_PAY_AMOUNT}), 0.0) FROM ${DatabaseHelper.TABLE_PAYMENTS} WHERE ${DatabaseHelper.COL_PAY_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )
            val priorPaid = if (payCursor.moveToFirst()) payCursor.getDouble(0) else 0.0
            payCursor.close()

            val priorRecognizedProfit = (priorPaid - totalCost).coerceAtLeast(0.0)
            val newTotalPaid = priorPaid + amount
            val newRecognizedProfit = (newTotalPaid - totalCost).coerceAtLeast(0.0)
            val profitDelta = newRecognizedProfit - priorRecognizedProfit

            // 2. Insert new payment installment
            val payValues = ContentValues().apply {
                put(DatabaseHelper.COL_PAY_SALE_ID, saleId)
                put(DatabaseHelper.COL_PAY_AMOUNT, amount)
                put(DatabaseHelper.COL_PAY_METHOD, paymentMethod)
                put(DatabaseHelper.COL_PAY_DATE, date)
                put(DatabaseHelper.COL_PAY_NOTE, note)
            }
            db.insertOrThrow(DatabaseHelper.TABLE_PAYMENTS, null, payValues)

            // 3. Add received cash to account balance
            db.execSQL(
                "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} + ?",
                arrayOf(amount)
            )

            // 4. Update account totalProfit by the incremental recognized profit
            if (profitDelta > 0.0) {
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} + ?",
                    arrayOf(profitDelta)
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Deletes a sale transactionally:
     * - Restores stock for all line items.
     * - Reverses the cash balance by the SUM of payments actually received for this sale.
     * - Reverses the gross profit from account.
     * - Deletes the sale (cascades to items and payments).
     */
    fun delete(saleId: Int) {
        db.beginTransaction()
        try {
            // 1. Restore stock for all items in this sale
            val itemsCursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ITEM_PROD_ID}, ${DatabaseHelper.COL_ITEM_QTY} FROM ${DatabaseHelper.TABLE_SALE_ITEMS} WHERE ${DatabaseHelper.COL_ITEM_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )
            while (itemsCursor.moveToNext()) {
                val productId = itemsCursor.getInt(0)
                val qty = itemsCursor.getInt(1)
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_PRODUCTS} SET ${DatabaseHelper.COL_PROD_QTY} = ${DatabaseHelper.COL_PROD_QTY} + ? WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
                    arrayOf(qty, productId)
                )
            }
            itemsCursor.close()

            // 2. Sum actual cash payments received for this sale to reverse from account
            val payCursor = db.rawQuery(
                "SELECT COALESCE(SUM(${DatabaseHelper.COL_PAY_AMOUNT}), 0.0) FROM ${DatabaseHelper.TABLE_PAYMENTS} WHERE ${DatabaseHelper.COL_PAY_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )
            val actualCashPaid = if (payCursor.moveToFirst()) payCursor.getDouble(0) else 0.0
            payCursor.close()

            if (actualCashPaid > 0.0) {
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} - ?",
                    arrayOf(actualCashPaid)
                )
            }

            // 3. Revert actual recognized profit contribution from account
            val saleCursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_SALE_COST} FROM ${DatabaseHelper.TABLE_SALES} WHERE ${DatabaseHelper.COL_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )
            val totalCost = if (saleCursor.moveToFirst()) saleCursor.getDouble(0) else 0.0
            saleCursor.close()

            val recognizedProfitToRevert = (actualCashPaid - totalCost).coerceAtLeast(0.0)
            if (recognizedProfitToRevert > 0.0) {
                db.execSQL(
                    "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} - ?",
                    arrayOf(recognizedProfitToRevert)
                )
            }

            // 4. Delete sale record (cascading deletes child items and payments)
            db.delete(
                DatabaseHelper.TABLE_SALES,
                "${DatabaseHelper.COL_SALE_ID} = ?",
                arrayOf(saleId.toString())
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Retrieves all sales with aggregated items count and payments without Cartesian multiplication.
     */
    fun getAll(): List<Sale> {
        val list = mutableListOf<Sale>()
        val cursor = db.rawQuery("""
            SELECT 
                s.${DatabaseHelper.COL_SALE_ID},
                s.${DatabaseHelper.COL_SALE_CUST_ID},
                s.${DatabaseHelper.COL_SALE_DATE},
                s.${DatabaseHelper.COL_SALE_TOTAL},
                s.${DatabaseHelper.COL_SALE_COST},
                s.${DatabaseHelper.COL_SALE_NOTES},
                COALESCE(c.${DatabaseHelper.COL_CUST_NAME}, 'Walk-in Customer') AS customerName,
                COALESCE(c.${DatabaseHelper.COL_CUST_PHONE}, '') AS customerPhone,
                COALESCE(items.itemCount, 0) AS itemCount,
                COALESCE(pays.totalPaid, 0.0) AS paidAmount,
                COALESCE(pays.initialMethod, 'Cash') AS paymentMethod
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
                ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_ITEM_SALE_ID}, COUNT(${DatabaseHelper.COL_ITEM_ID}) AS itemCount
                FROM ${DatabaseHelper.TABLE_SALE_ITEMS}
                GROUP BY ${DatabaseHelper.COL_ITEM_SALE_ID}
            ) items ON s.${DatabaseHelper.COL_SALE_ID} = items.${DatabaseHelper.COL_ITEM_SALE_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, 
                       SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS totalPaid, 
                       MIN(${DatabaseHelper.COL_PAY_METHOD}) AS initialMethod
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) pays ON s.${DatabaseHelper.COL_SALE_ID} = pays.${DatabaseHelper.COL_PAY_SALE_ID}
            ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC, s.${DatabaseHelper.COL_SALE_ID} DESC
        """, null)

        while (cursor.moveToNext()) {
            list.add(cursor.toSale())
        }
        cursor.close()
        return list
    }

    fun getById(id: Int): Sale? {
        val cursor = db.rawQuery("""
            SELECT 
                s.${DatabaseHelper.COL_SALE_ID},
                s.${DatabaseHelper.COL_SALE_CUST_ID},
                s.${DatabaseHelper.COL_SALE_DATE},
                s.${DatabaseHelper.COL_SALE_TOTAL},
                s.${DatabaseHelper.COL_SALE_COST},
                s.${DatabaseHelper.COL_SALE_NOTES},
                COALESCE(c.${DatabaseHelper.COL_CUST_NAME}, 'Walk-in Customer') AS customerName,
                COALESCE(c.${DatabaseHelper.COL_CUST_PHONE}, '') AS customerPhone,
                COALESCE(items.itemCount, 0) AS itemCount,
                COALESCE(pays.totalPaid, 0.0) AS paidAmount,
                COALESCE(pays.initialMethod, 'Cash') AS paymentMethod
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
                ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_ITEM_SALE_ID}, COUNT(${DatabaseHelper.COL_ITEM_ID}) AS itemCount
                FROM ${DatabaseHelper.TABLE_SALE_ITEMS}
                GROUP BY ${DatabaseHelper.COL_ITEM_SALE_ID}
            ) items ON s.${DatabaseHelper.COL_SALE_ID} = items.${DatabaseHelper.COL_ITEM_SALE_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, 
                       SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS totalPaid, 
                       MIN(${DatabaseHelper.COL_PAY_METHOD}) AS initialMethod
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) pays ON s.${DatabaseHelper.COL_SALE_ID} = pays.${DatabaseHelper.COL_PAY_SALE_ID}
            WHERE s.${DatabaseHelper.COL_SALE_ID} = ?
        """, arrayOf(id.toString()))

        val sale = if (cursor.moveToFirst()) cursor.toSale() else null
        cursor.close()
        return sale
    }

    fun getByCustomer(customerId: Int): List<Sale> {
        val list = mutableListOf<Sale>()
        val cursor = db.rawQuery("""
            SELECT 
                s.${DatabaseHelper.COL_SALE_ID},
                s.${DatabaseHelper.COL_SALE_CUST_ID},
                s.${DatabaseHelper.COL_SALE_DATE},
                s.${DatabaseHelper.COL_SALE_TOTAL},
                s.${DatabaseHelper.COL_SALE_COST},
                s.${DatabaseHelper.COL_SALE_NOTES},
                COALESCE(c.${DatabaseHelper.COL_CUST_NAME}, 'Walk-in Customer') AS customerName,
                COALESCE(c.${DatabaseHelper.COL_CUST_PHONE}, '') AS customerPhone,
                COALESCE(items.itemCount, 0) AS itemCount,
                COALESCE(pays.totalPaid, 0.0) AS paidAmount,
                COALESCE(pays.initialMethod, 'Cash') AS paymentMethod
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
                ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_ITEM_SALE_ID}, COUNT(${DatabaseHelper.COL_ITEM_ID}) AS itemCount
                FROM ${DatabaseHelper.TABLE_SALE_ITEMS}
                GROUP BY ${DatabaseHelper.COL_ITEM_SALE_ID}
            ) items ON s.${DatabaseHelper.COL_SALE_ID} = items.${DatabaseHelper.COL_ITEM_SALE_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, 
                       SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS totalPaid, 
                       MIN(${DatabaseHelper.COL_PAY_METHOD}) AS initialMethod
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) pays ON s.${DatabaseHelper.COL_SALE_ID} = pays.${DatabaseHelper.COL_PAY_SALE_ID}
            WHERE s.${DatabaseHelper.COL_SALE_CUST_ID} = ?
            ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC, s.${DatabaseHelper.COL_SALE_ID} DESC
        """, arrayOf(customerId.toString()))

        while (cursor.moveToNext()) {
            list.add(cursor.toSale())
        }
        cursor.close()
        return list
    }

    fun getItemsForSale(saleId: Int): List<SaleItem> {
        val list = mutableListOf<SaleItem>()
        val cursor = db.rawQuery("""
            SELECT 
                ${DatabaseHelper.COL_ITEM_ID},
                ${DatabaseHelper.COL_ITEM_SALE_ID},
                ${DatabaseHelper.COL_ITEM_PROD_ID},
                ${DatabaseHelper.COL_ITEM_PROD_NAME},
                ${DatabaseHelper.COL_ITEM_QTY},
                ${DatabaseHelper.COL_ITEM_UNIT_PRICE},
                ${DatabaseHelper.COL_ITEM_UNIT_COST},
                ${DatabaseHelper.COL_ITEM_SUBTOTAL},
                ${DatabaseHelper.COL_ITEM_PROFIT}
            FROM ${DatabaseHelper.TABLE_SALE_ITEMS}
            WHERE ${DatabaseHelper.COL_ITEM_SALE_ID} = ?
            ORDER BY ${DatabaseHelper.COL_ITEM_ID} ASC
        """, arrayOf(saleId.toString()))

        while (cursor.moveToNext()) {
            list.add(
                SaleItem(
                    id = cursor.getInt(0),
                    saleId = cursor.getInt(1),
                    productId = cursor.getInt(2),
                    productName = cursor.getString(3),
                    quantity = cursor.getInt(4),
                    unitPrice = cursor.getDouble(5),
                    unitCost = cursor.getDouble(6),
                    subtotal = cursor.getDouble(7),
                    profit = cursor.getDouble(8)
                )
            )
        }
        cursor.close()
        return list
    }

    fun getPaymentsForSale(saleId: Int): List<Payment> {
        val list = mutableListOf<Payment>()
        val cursor = db.rawQuery("""
            SELECT 
                ${DatabaseHelper.COL_PAY_ID},
                ${DatabaseHelper.COL_PAY_SALE_ID},
                ${DatabaseHelper.COL_PAY_AMOUNT},
                ${DatabaseHelper.COL_PAY_METHOD},
                ${DatabaseHelper.COL_PAY_DATE},
                ${DatabaseHelper.COL_PAY_NOTE}
            FROM ${DatabaseHelper.TABLE_PAYMENTS}
            WHERE ${DatabaseHelper.COL_PAY_SALE_ID} = ?
            ORDER BY ${DatabaseHelper.COL_PAY_ID} ASC
        """, arrayOf(saleId.toString()))

        while (cursor.moveToNext()) {
            list.add(
                Payment(
                    id = cursor.getInt(0),
                    saleId = cursor.getInt(1),
                    amount = cursor.getDouble(2),
                    paymentMethod = cursor.getString(3),
                    paymentDate = cursor.getString(4),
                    note = cursor.getString(5)
                )
            )
        }
        cursor.close()
        return list
    }

    /**
     * Computes the total recognized gross profit across all sales:
     * SUM(max(0, totalPaid - totalCost)).
     * Ensures unpaid and partially paid sales do not recognize premature profit,
     * and overpayments are recognized as profit.
     */
    fun getTotalProfit(): Double {
        val cursor = db.rawQuery("""
            SELECT COALESCE(SUM(
                CASE 
                    WHEN COALESCE(p.paid, 0.0) > s.${DatabaseHelper.COL_SALE_COST} 
                    THEN (COALESCE(p.paid, 0.0) - s.${DatabaseHelper.COL_SALE_COST}) 
                    ELSE 0.0 
                END
            ), 0.0)
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS paid
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) p ON s.${DatabaseHelper.COL_SALE_ID} = p.${DatabaseHelper.COL_PAY_SALE_ID}
        """.trimIndent(), null)
        val result = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        cursor.close()
        return result
    }

    /**
     * Computes total recognized revenue across all sales:
     * SUM(min(saleTotal, totalPaid)).
     * Unpaid orders recognize $0, partial payments recognize cash received up to sale total,
     * and overpayments cap recognized revenue at agreed sale total.
     */
    fun getTotalRevenue(): Double {
        val cursor = db.rawQuery("""
            SELECT COALESCE(SUM(
                CASE 
                    WHEN COALESCE(p.paid, 0.0) >= s.${DatabaseHelper.COL_SALE_TOTAL} 
                    THEN s.${DatabaseHelper.COL_SALE_TOTAL} 
                    ELSE COALESCE(p.paid, 0.0) 
                END
            ), 0.0)
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS paid
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) p ON s.${DatabaseHelper.COL_SALE_ID} = p.${DatabaseHelper.COL_PAY_SALE_ID}
        """.trimIndent(), null)
        val result = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        cursor.close()
        return result
    }

    fun getSalesCount(): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(${DatabaseHelper.COL_SALE_ID}) FROM ${DatabaseHelper.TABLE_SALES}",
            null
        )
        val result = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return result
    }

    fun searchSale(query: String): List<Sale> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAll()

        val list = mutableListOf<Sale>()
        val pattern = "%$trimmed%"
        val cursor = db.rawQuery("""
            SELECT DISTINCT
                s.${DatabaseHelper.COL_SALE_ID},
                s.${DatabaseHelper.COL_SALE_CUST_ID},
                s.${DatabaseHelper.COL_SALE_DATE},
                s.${DatabaseHelper.COL_SALE_TOTAL},
                s.${DatabaseHelper.COL_SALE_COST},
                s.${DatabaseHelper.COL_SALE_NOTES},
                COALESCE(c.${DatabaseHelper.COL_CUST_NAME}, 'Walk-in Customer') AS customerName,
                COALESCE(c.${DatabaseHelper.COL_CUST_PHONE}, '') AS customerPhone,
                COALESCE(items.itemCount, 0) AS itemCount,
                COALESCE(pays.totalPaid, 0.0) AS paidAmount,
                COALESCE(pays.initialMethod, 'Cash') AS paymentMethod
            FROM ${DatabaseHelper.TABLE_SALES} s
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
                ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_ITEM_SALE_ID}, COUNT(${DatabaseHelper.COL_ITEM_ID}) AS itemCount
                FROM ${DatabaseHelper.TABLE_SALE_ITEMS}
                GROUP BY ${DatabaseHelper.COL_ITEM_SALE_ID}
            ) items ON s.${DatabaseHelper.COL_SALE_ID} = items.${DatabaseHelper.COL_ITEM_SALE_ID}
            LEFT JOIN (
                SELECT ${DatabaseHelper.COL_PAY_SALE_ID}, 
                       SUM(${DatabaseHelper.COL_PAY_AMOUNT}) AS totalPaid, 
                       MIN(${DatabaseHelper.COL_PAY_METHOD}) AS initialMethod
                FROM ${DatabaseHelper.TABLE_PAYMENTS}
                GROUP BY ${DatabaseHelper.COL_PAY_SALE_ID}
            ) pays ON s.${DatabaseHelper.COL_SALE_ID} = pays.${DatabaseHelper.COL_PAY_SALE_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_SALE_ITEMS} si
                ON s.${DatabaseHelper.COL_SALE_ID} = si.${DatabaseHelper.COL_ITEM_SALE_ID}
            WHERE c.${DatabaseHelper.COL_CUST_NAME} LIKE ?
               OR c.${DatabaseHelper.COL_CUST_PHONE} LIKE ?
               OR si.${DatabaseHelper.COL_ITEM_PROD_NAME} LIKE ?
               OR s.${DatabaseHelper.COL_SALE_DATE} LIKE ?
               OR pays.initialMethod LIKE ?
            ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC, s.${DatabaseHelper.COL_SALE_ID} DESC
        """, arrayOf(pattern, pattern, pattern, pattern, pattern))

        while (cursor.moveToNext()) {
            list.add(cursor.toSale())
        }
        cursor.close()
        return list
    }

    private fun Cursor.toSale(): Sale {
        val total = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_TOTAL))
        val cost  = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_COST))
        val paid  = getDouble(getColumnIndexOrThrow("paidAmount"))
        val debt  = (total - paid).coerceAtLeast(0.0)
        val recognizedProfit = (paid - cost).coerceAtLeast(0.0)

        val custIdCol = getColumnIndexOrThrow(DatabaseHelper.COL_SALE_CUST_ID)
        val customerId = if (isNull(custIdCol)) null else getInt(custIdCol)

        return Sale(
            id = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_ID)),
            customerId = customerId,
            date = getString(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_DATE)),
            totalAmount = total,
            totalCost = cost,
            notes = getString(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_NOTES)) ?: "",
            customerName = getString(getColumnIndexOrThrow("customerName")) ?: "Walk-in Customer",
            customerPhone = getString(getColumnIndexOrThrow("customerPhone")) ?: "",
            itemCount = getInt(getColumnIndexOrThrow("itemCount")),
            paidAmount = paid,
            remainingDebt = debt,
            profit = recognizedProfit,
            paymentMethod = getString(getColumnIndexOrThrow("paymentMethod")) ?: "Cash"
        )
    }
}
