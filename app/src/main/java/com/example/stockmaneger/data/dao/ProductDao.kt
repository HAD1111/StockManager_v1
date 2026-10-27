package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Product

class ProductDao(context: Context) {
    private val accountDao = AccountDao(context)
    private val db = DatabaseHelper(context).writableDatabase

    fun insert(product: Product): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PROD_NAME, product.name)
            put(DatabaseHelper.COL_PROD_QTY,  product.quantity)
            put(DatabaseHelper.COL_PROD_BASE, product.basePrice)
            put(DatabaseHelper.COL_PROD_SALE, product.salePrice)
            put(DatabaseHelper.COL_PROD_BARCODE, product.barcode)
        }
        return db.insert(DatabaseHelper.TABLE_PRODUCTS, null, values)
    }

    fun update(product: Product): Int {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PROD_NAME, product.name)
            put(DatabaseHelper.COL_PROD_QTY,  product.quantity)
            put(DatabaseHelper.COL_PROD_BASE, product.basePrice)
            put(DatabaseHelper.COL_PROD_SALE, product.salePrice)
            put(DatabaseHelper.COL_PROD_BARCODE, product.barcode)
        }
        return db.update(
            DatabaseHelper.TABLE_PRODUCTS,
            values,
            "${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(product.id.toString())
        )
    }

    fun delete(id: Int): Int {
        val salesCursor = db.rawQuery(
            "SELECT 1 FROM ${DatabaseHelper.TABLE_SALE_ITEMS} WHERE ${DatabaseHelper.COL_ITEM_PROD_ID}=? LIMIT 1",
            arrayOf(id.toString())
        )

        val hasSales = salesCursor.moveToFirst()
        salesCursor.close()

        if (hasSales) return 0

        var inventoryValue = 0.0

        val productCursor = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_PROD_QTY},
                   ${DatabaseHelper.COL_PROD_BASE}
            FROM ${DatabaseHelper.TABLE_PRODUCTS}
            WHERE ${DatabaseHelper.COL_PROD_ID}=?
            """.trimIndent(),
            arrayOf(id.toString())
        )

        if (productCursor.moveToFirst()) {
            inventoryValue = productCursor.getInt(0) * productCursor.getDouble(1)
        }
        productCursor.close()

        val deletedRows = db.delete(
            DatabaseHelper.TABLE_PRODUCTS,
            "${DatabaseHelper.COL_PROD_ID}=?",
            arrayOf(id.toString())
        )

        if (deletedRows > 0) {
            accountDao.addMoney(inventoryValue)
        }

        return deletedRows
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
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_PRODUCTS} SET ${DatabaseHelper.COL_PROD_QTY} = ${DatabaseHelper.COL_PROD_QTY} - ? WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(amount, productId)
        )
    }

    fun getByBarcode(barcode: String): Product? {
        val cursor = db.rawQuery(
            """
            SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS}
            WHERE ${DatabaseHelper.COL_PROD_BARCODE} = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(barcode)
        )

        val product = if (cursor.moveToFirst()) cursor.toProduct() else null
        cursor.close()
        return product
    }

    fun getTotalProductCount(): Int {
        val cursor = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COL_PROD_QTY}) FROM ${DatabaseHelper.TABLE_PRODUCTS}",
            null
        )
        val result = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return result
    }

    fun getTotalCategories(): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_PRODUCTS}",
            null
        )
        val result = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return result
    }

    private fun android.database.Cursor.toProduct() = Product(
        id = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_ID)),
        name = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME)),
        quantity = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_QTY)),
        basePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_BASE)),
        salePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_SALE)),
        barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PROD_BARCODE)) ?: ""
    )
}
