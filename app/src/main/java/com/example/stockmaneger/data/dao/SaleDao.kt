package com.example.stockmaneger.data.dao

import android.content.ContentValues
import android.content.Context
import com.example.stockmaneger.data.DatabaseHelper
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.data.models.Sale

class SaleDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase

    fun insert(sale: Sale): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SALE_CUST_ID, sale.customerId)
            put(DatabaseHelper.COL_SALE_PROD_ID, sale.productId)
            put(DatabaseHelper.COL_SALE_QTY, sale.quantity)
            put(DatabaseHelper.COL_SALE_UNIT, sale.unitPrice)
            put(DatabaseHelper.COL_SALE_TOTAL, sale.totalPrice)
            put(DatabaseHelper.COL_SALE_BASE, sale.basePrice)
            put(DatabaseHelper.COL_SALE_PROFIT, sale.profit)
            put(DatabaseHelper.COL_SALE_DATE, sale.date)
            put(DatabaseHelper.COL_SALE_PAYMENT, sale.paymentMethod)
        }
        return db.insert(DatabaseHelper.TABLE_SALES, null, values)
    }

    fun delete(id: Int) {

        val query = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_SALE_QTY}, ${DatabaseHelper.COL_SALE_PROD_ID} " +
                    "FROM ${DatabaseHelper.TABLE_SALES} " +
                    "WHERE ${DatabaseHelper.COL_SALE_ID} = ?",
            arrayOf(id.toString())
        )

        if (query.moveToFirst()) {

            val qty = query.getInt(0)
            val productId = query.getInt(1)

            db.execSQL(
                "UPDATE ${DatabaseHelper.TABLE_PRODUCTS} " +
                        "SET ${DatabaseHelper.COL_PROD_QTY} = ${DatabaseHelper.COL_PROD_QTY} + ? " +
                        "WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
                arrayOf(qty, productId)
            )
        }

        query.close()

        db.delete(
            DatabaseHelper.TABLE_SALES,
            "${DatabaseHelper.COL_SALE_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Sale> {
        val list = mutableListOf<Sale>()

        val cursor = db.rawQuery("""
            SELECT 
            s.*,
            c.${DatabaseHelper.COL_CUST_NAME}  AS customerName,
            c.${DatabaseHelper.COL_CUST_PHONE} AS customerPhone,
            p.${DatabaseHelper.COL_PROD_NAME}  AS productName
            
            FROM ${DatabaseHelper.TABLE_SALES} s
            
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
            ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            
            LEFT JOIN ${DatabaseHelper.TABLE_PRODUCTS} p
            ON s.${DatabaseHelper.COL_SALE_PROD_ID} = p.${DatabaseHelper.COL_PROD_ID}
            
            ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC
        """, null)

        while (cursor.moveToNext()) {
            list.add(cursor.toSale())
        }

        cursor.close()
        return list
    }

    fun getByCustomer(customerId: Int): List<Sale> {

        val list = mutableListOf<Sale>()

        val cursor = db.rawQuery("""
            SELECT 
            s.*,
            c.${DatabaseHelper.COL_CUST_NAME}  AS customerName,
            c.${DatabaseHelper.COL_CUST_PHONE} AS customerPhone,
            p.${DatabaseHelper.COL_PROD_NAME}  AS productName
            
            FROM ${DatabaseHelper.TABLE_SALES} s
            
            LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
            ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
            
            LEFT JOIN ${DatabaseHelper.TABLE_PRODUCTS} p
            ON s.${DatabaseHelper.COL_SALE_PROD_ID} = p.${DatabaseHelper.COL_PROD_ID}
            
            WHERE s.${DatabaseHelper.COL_SALE_CUST_ID} = ?
            
            ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC
        """, arrayOf(customerId.toString()))

        while (cursor.moveToNext()) {
            list.add(cursor.toSale())
        }

        cursor.close()
        return list
    }

    fun getTotalProfit(): Double {
        val cursor = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COL_SALE_PROFIT}) FROM ${DatabaseHelper.TABLE_SALES}",
            null
        )

        val result = if (cursor.moveToFirst())
            cursor.getDouble(0)
        else
            0.0

        cursor.close()
        return result
    }

    fun getTotalRevenue(): Double {
        val cursor = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COL_SALE_TOTAL}) FROM ${DatabaseHelper.TABLE_SALES}",
            null
        )

        val result = if (cursor.moveToFirst())
            cursor.getDouble(0)
        else
            0.0

        cursor.close()
        return result
    }

    private fun android.database.Cursor.toSale() = Sale(

        id = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_ID)),

        customerId = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_CUST_ID)),

        productId = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_PROD_ID)),

        customerName =
            getString(getColumnIndexOrThrow("customerName")) ?: "Unknown",

        customerPhone =
            getString(getColumnIndexOrThrow("customerPhone")) ?: "",

        productName =
            getString(getColumnIndexOrThrow("productName")) ?: "Unknown",

        quantity =
            getInt(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_QTY)),

        unitPrice =
            getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_UNIT)),

        totalPrice =
            getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_TOTAL)),

        basePrice =
            getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_BASE)),

        profit =
            getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_PROFIT)),

        date =
            getString(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_DATE)),

        paymentMethod =
            getString(getColumnIndexOrThrow(DatabaseHelper.COL_SALE_PAYMENT)) ?: "Cash"
    )

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

    fun isPaymentMethod(inp: String): Boolean{
            val input = inp.lowercase().trim()
        return input.contains("cash")or input.contains("omt")or
                input.contains("whish")or input.contains("crypto")
    }

    fun searchSale(query: String): List<Sale>{

        val tokens = query.trim().split(Regex("\\s+"))
        val where = StringBuilder("WHERE 1=1 ")

        val phone = tokens.filter { isLebaneseNumber(it) }
        val payment = tokens.filter { isPaymentMethod(it) }
        val others = tokens.filter {
            !isLebaneseNumber(it) && !isPaymentMethod(it)
        }

        for(p in phone){
            where.append(" AND c.${DatabaseHelper.COL_CUST_PHONE} LIKE '%$p%'")
        }

        for(pay in payment){
            where.append(" AND s.${DatabaseHelper.COL_SALE_PAYMENT} LIKE '%$pay%'")
        }

        // others (name or product)
        for(phrase in others){
            where.append("""
            AND (
                c.${DatabaseHelper.COL_CUST_NAME} LIKE '%$phrase%' OR
                p.${DatabaseHelper.COL_PROD_NAME} LIKE '%$phrase%'
            )
        """.trimIndent())
        }


        val list = mutableListOf<Sale>()

        val cursor = db.rawQuery("""
        SELECT 
        s.*,
        c.${DatabaseHelper.COL_CUST_NAME}  AS customerName,
        c.${DatabaseHelper.COL_CUST_PHONE} AS customerPhone,
        p.${DatabaseHelper.COL_PROD_NAME}  AS productName
        
        FROM ${DatabaseHelper.TABLE_SALES} s
        
        LEFT JOIN ${DatabaseHelper.TABLE_CUSTOMERS} c
        ON s.${DatabaseHelper.COL_SALE_CUST_ID} = c.${DatabaseHelper.COL_CUST_ID}
        
        LEFT JOIN ${DatabaseHelper.TABLE_PRODUCTS} p
        ON s.${DatabaseHelper.COL_SALE_PROD_ID} = p.${DatabaseHelper.COL_PROD_ID}
        
        $where
        
        ORDER BY s.${DatabaseHelper.COL_SALE_DATE} DESC
    """, null)


        if(cursor.moveToFirst()){
            do{
                list.add(cursor.toSale())
            } while(cursor.moveToNext())
        }

        cursor.close()

        return list
    }
}