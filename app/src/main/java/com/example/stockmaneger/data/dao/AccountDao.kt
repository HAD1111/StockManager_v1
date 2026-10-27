// data/dao/AccountDao.kt
package com.example.stockmaneger.data.dao

import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper

class AccountDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun getTotalMoney(): Double {
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ACC_MONEY} FROM ${DatabaseHelper.TABLE_ACCOUNT}",
            null
        )
        return if (cursor.moveToFirst())
            cursor.getDouble(0).also { cursor.close() }
        else
            0.0.also { cursor.close() }
    }

    /**
     * Computes the total recognized gross profit directly from sales and payments:
     * SUM(max(0, totalPaid - totalCost)).
     * This ensures the profit display is always accurate, immune to desynchronization,
     * and strictly reflects recognized cash-backed gross profit across all sales.
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

    fun addMoney(amount: Double) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} + ?",
            arrayOf(amount)
        )
    }

    fun deductMoney(amount: Double) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} - ?",
            arrayOf(amount)
        )
    }

    fun addProfit(amount: Double) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} + ?",
            arrayOf(amount)
        )
    }

    fun deductProfit(amount: Double) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} - ?",
            arrayOf(amount)
        )
    }

    fun getBusinessName(): String {
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ACC_BUSINESS_NAME} FROM ${DatabaseHelper.TABLE_ACCOUNT} LIMIT 1",
            null
        )
        var result = if (cursor.moveToFirst()) cursor.getString(0) else ""
        if (result.isEmpty()) result = "My Business"
        cursor.close()
        return result
    }

    fun updateBusinessName(name: String) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_ACCOUNT} SET ${DatabaseHelper.COL_ACC_BUSINESS_NAME} = ?",
            arrayOf(name)
        )
    }
}
