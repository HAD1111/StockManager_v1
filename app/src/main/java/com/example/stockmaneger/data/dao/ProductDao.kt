package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Product

class ProductDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun insert(product: Product): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PROD_NAME, product.name)
            put(DatabaseHelper.COL_PROD_QTY,  product.quantity)
            put(DatabaseHelper.COL_PROD_BASE, product.basePrice)
            put(DatabaseHelper.COL_PROD_SALE, product.salePrice)
        }
        return db.insert(DatabaseHelper.TABLE_PRODUCTS, null, values)
    }

    fun update(product: Product): Int {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PROD_NAME, product.name)
            put(DatabaseHelper.COL_PROD_QTY,  product.quantity)
            put(DatabaseHelper.COL_PROD_BASE, product.basePrice)
            put(DatabaseHelper.COL_PROD_SALE, product.salePrice)
        }
        return db.update(
            DatabaseHelper.TABLE_PRODUCTS,
            values,
            "${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(product.id.toString())
        )
    }

    fun delete(id: Int): Int {
        return db.delete(
            DatabaseHelper.TABLE_PRODUCTS,
            "${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Product> {
        val list = mutableListOf<Product>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS} ORDER BY ${DatabaseHelper.COL_PROD_NAME} ASC",
            null
        )
        while (cursor.moveToNext()) {
            list.add(cursor.toProduct())
        }
        cursor.close()
        return list
    }

    fun getById(id: Int): Product? {
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(id.toString())
        )
        return if (cursor.moveToFirst())
            cursor.toProduct().also { cursor.close() }
        else
            null.also { cursor.close() }
    }

    fun searchByName(query: String): List<Product> {
        val list = mutableListOf<Product>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COL_PROD_NAME} LIKE ?",
            arrayOf("%$query%")
        )
        while (cursor.moveToNext()) {
            list.add(cursor.toProduct())
        }
        cursor.close()
        return list
    }

    fun decreaseQuantity(productId: Int, amount: Int) {
        db.execSQL("""
            UPDATE ${DatabaseHelper.TABLE_PRODUCTS}
            SET ${DatabaseHelper.COL_PROD_QTY} = ${DatabaseHelper.COL_PROD_QTY} - $amount
            WHERE ${DatabaseHelper.COL_PROD_ID} = $productId
        """)
    }

    // Extension to avoid repeating cursor mapping
    private fun android.database.Cursor.toProduct() = Product(
        id        = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_ID)),
        name      = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME)),
        quantity  = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_QTY)),
        basePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_BASE)),
        salePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_SALE))
    )
}