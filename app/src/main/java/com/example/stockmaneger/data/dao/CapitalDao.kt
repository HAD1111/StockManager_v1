package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Capital

class CapitalDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun insert(capital: Capital): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CAP_AMOUNT, capital.amount)
            put(DatabaseHelper.COL_CAP_TYPE,   capital.type)
            put(DatabaseHelper.COL_CAP_NOTE,   capital.note)
            put(DatabaseHelper.COL_CAP_DATE,   capital.date)
        }
        return db.insert(DatabaseHelper.TABLE_CAPITALS, null, values)
    }

    fun delete(id: Int): Int {
        return db.delete(
            DatabaseHelper.TABLE_CAPITALS,
            "${DatabaseHelper.COL_CAP_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Capital> {
        val list = mutableListOf<Capital>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_CAPITALS} ORDER BY ${DatabaseHelper.COL_CAP_DATE} DESC",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                Capital(
                    id     = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAP_ID)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAP_AMOUNT)),
                    type   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAP_TYPE)),
                    note   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAP_NOTE)),
                    date   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAP_DATE))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getTotalCapitals(): Double {
        val cursor = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COL_CAP_AMOUNT}) FROM ${DatabaseHelper.TABLE_CAPITALS}",
            null
        )
        return if (cursor.moveToFirst())
            cursor.getDouble(0).also { cursor.close() }
        else
            0.0.also { cursor.close() }
    }
}