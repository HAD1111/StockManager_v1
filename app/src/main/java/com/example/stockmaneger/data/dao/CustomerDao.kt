// data/dao/CustomerDao.kt
package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Customer

class CustomerDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun insert(customer: Customer): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CUST_NAME, customer.name)
            put(DatabaseHelper.COL_CUST_PHONE, customer.phone)
        }
        return db.insert(DatabaseHelper.TABLE_CUSTOMERS, null, values)
    }

    fun update(customer: Customer): Int {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CUST_NAME, customer.name)
            put(DatabaseHelper.COL_CUST_PHONE, customer.phone)
        }
        return db.update(
            DatabaseHelper.TABLE_CUSTOMERS,
            values,
            "${DatabaseHelper.COL_CUST_ID} = ?",
            arrayOf(customer.id.toString())
        )
    }

    fun delete(id: Int): Int {
        return db.delete(
            DatabaseHelper.TABLE_CUSTOMERS,
            "${DatabaseHelper.COL_CUST_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Customer> {
        val list = mutableListOf<Customer>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_CUSTOMERS} ORDER BY ${DatabaseHelper.COL_CUST_NAME} ASC",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                Customer(
                    id    = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_ID)),
                    name  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_NAME)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_PHONE))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getById(id: Int): Customer? {
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_CUSTOMERS} WHERE ${DatabaseHelper.COL_CUST_ID} = ?",
            arrayOf(id.toString())
        )
        return if (cursor.moveToFirst()) {
            Customer(
                id    = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_ID)),
                name  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_PHONE))
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun isLebaneseNumber(input: String): Boolean {
        val number = input.trim().replace(" ", "")

        val cleaned = number.removePrefix("+").removePrefix("00")

        if (cleaned.startsWith("961")) {
            return Regex("""^961(3|70|71|76|78|79|81|82|83|84|85|86|87|88|89)\d{6}$""")
                .matches(cleaned)
        }

        return Regex("""^(3|70|71|76|78|79|81|82|83|84|85|86|87|88|89)\d{6}$""")
            .matches(cleaned)
    }
    // Used in the sales form — search by name as user types
    fun searchByName(query: String): List<Customer> {
        val tokens = query.split(" ")
        val phone = tokens.filter { isLebaneseNumber(it) }
        val name = tokens.filter { !isLebaneseNumber(it)}
        val where = StringBuilder("WHERE 1=1 ")

        for(p in phone) where.append(" AND ${DatabaseHelper.COL_CUST_PHONE} LIKE '%$p%' ")
        for(n in name) where.append(" AND ${DatabaseHelper.COL_CUST_NAME} LIKE '%$n%' ")

        val list = mutableListOf<Customer>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_CUSTOMERS} $where",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                Customer(
                    id    = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_ID)),
                    name  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_NAME)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CUST_PHONE))
                )
            )
        }
        cursor.close()
        return list
    }
}