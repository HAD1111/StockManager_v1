// data/dao/AccountDao.kt
package com.example.stockmaneger.data.dao

import android.content.ContentValues
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

    fun getTotalProfit(): Double {
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ACC_PROFIT} FROM ${DatabaseHelper.TABLE_ACCOUNT}",
            null
        )
        return if (cursor.moveToFirst())
            cursor.getDouble(0).also { cursor.close() }
        else
            0.0.also { cursor.close() }
    }

    fun addMoney(amount: Double) {
        db.execSQL("""
            UPDATE ${DatabaseHelper.TABLE_ACCOUNT}
            SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} + $amount
        """)
    }

    fun deductMoney(amount: Double) {
        db.execSQL("""
            UPDATE ${DatabaseHelper.TABLE_ACCOUNT}
            SET ${DatabaseHelper.COL_ACC_MONEY} = ${DatabaseHelper.COL_ACC_MONEY} - $amount
        """)
    }

    fun addProfit(amount: Double) {
        db.execSQL("""
            UPDATE ${DatabaseHelper.TABLE_ACCOUNT}
            SET ${DatabaseHelper.COL_ACC_PROFIT} = ${DatabaseHelper.COL_ACC_PROFIT} + $amount
        """)
    }
}