package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Expense

class ExpenseDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun insert(expense: Expense): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EXP_AMOUNT, expense.amount)
            put(DatabaseHelper.COL_EXP_TYPE,   expense.type)
            put(DatabaseHelper.COL_EXP_NOTE,   expense.note)
            put(DatabaseHelper.COL_EXP_DATE,   expense.date)
        }
        return db.insert(DatabaseHelper.TABLE_EXPENSES, null, values)
    }

    fun delete(id: Int): Int {
        return db.delete(
            DatabaseHelper.TABLE_EXPENSES,
            "${DatabaseHelper.COL_EXP_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Expense> {
        val list = mutableListOf<Expense>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_EXPENSES} ORDER BY ${DatabaseHelper.COL_EXP_DATE} DESC",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                Expense(
                    id     = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_ID)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_AMOUNT)),
                    type   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_TYPE)),
                    note   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_NOTE)),
                    date   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DATE))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getTotalExpenses(): Double {
        val cursor = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COL_EXP_AMOUNT}) FROM ${DatabaseHelper.TABLE_EXPENSES}",
            null
        )
        return if (cursor.moveToFirst())
            cursor.getDouble(0).also { cursor.close() }
        else
            0.0.also { cursor.close() }
    }
}