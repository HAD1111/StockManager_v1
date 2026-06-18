// data/DatabaseHelper.kt
package com.example.stockmaneger.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "stockmaneger.db"
        const val DATABASE_VERSION = 8



        // ── Customers ──────────────────────────────
        const val TABLE_CUSTOMERS   = "customers"
        const val COL_CUST_ID       = "id"
        const val COL_CUST_NAME     = "name"
        const val COL_CUST_PHONE    = "phone"

        // ── Products ───────────────────────────────
        const val TABLE_PRODUCTS    = "products"
        const val COL_PROD_ID       = "id"
        const val COL_PROD_NAME     = "name"
        const val COL_PROD_QTY      = "quantity"
        const val COL_PROD_BASE     = "basePrice"
        const val COL_PROD_SALE     = "salePrice"
        const val COL_PROD_BARCODE  = "barcode"

        // ── Sales ──────────────────────────────────
        const val TABLE_SALES       = "sales"
        const val COL_SALE_ID       = "id"
        const val COL_SALE_CUST_ID  = "customerId"
        const val COL_SALE_PROD_ID  = "productId"
        const val COL_SALE_QTY      = "quantity"
        const val COL_SALE_UNIT     = "unitPrice"
        const val COL_SALE_TOTAL    = "totalPrice"
        const val COL_SALE_BASE     = "basePrice"
        const val COL_SALE_PROFIT   = "profit"
        const val COL_SALE_DATE     = "date"

        const val COL_SALE_PAYMENT  = "paymentMethod"

        // ── Expenses ───────────────────────────────
        const val TABLE_EXPENSES    = "expenses"
        const val COL_EXP_ID        = "id"
        const val COL_EXP_AMOUNT    = "amount"
        const val COL_EXP_TYPE      = "type"
        const val COL_EXP_NOTE      = "note"
        const val COL_EXP_DATE      = "date"

        // ── Capitals ───────────────────────────────
        const val TABLE_CAPITALS    = "capitals"
        const val COL_CAP_ID        = "id"
        const val COL_CAP_AMOUNT    = "amount"
        const val COL_CAP_TYPE      = "type"
        const val COL_CAP_NOTE      = "note"
        const val COL_CAP_DATE      = "date"

        // ── Account ────────────────────────────────
        const val TABLE_ACCOUNT     = "account"
        const val COL_ACC_MONEY     = "totalMoney"
        const val COL_ACC_PROFIT    = "totalProfit"
        const val COL_ACC_BUSINESS_NAME = "businessName"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CUSTOMERS (
                $COL_CUST_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CUST_NAME  TEXT NOT NULL,
                $COL_CUST_PHONE TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_PROD_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PROD_NAME  TEXT NOT NULL,
                $COL_PROD_QTY   INTEGER DEFAULT 0,
                $COL_PROD_BASE  REAL DEFAULT 0.0,
                $COL_PROD_SALE  REAL DEFAULT 0.0,
                $COL_PROD_BARCODE TEXT Default ''
            )
        """)

        db.execSQL("""
    CREATE TABLE $TABLE_SALES (
        $COL_SALE_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
        $COL_SALE_CUST_ID INTEGER,
        $COL_SALE_PROD_ID INTEGER,
        $COL_SALE_QTY     INTEGER,
        $COL_SALE_UNIT    REAL,
        $COL_SALE_TOTAL   REAL,
        $COL_SALE_BASE    REAL,
        $COL_SALE_PROFIT  REAL,
        $COL_SALE_DATE    TEXT,
        $COL_SALE_PAYMENT TEXT DEFAULT 'Cash',
        FOREIGN KEY($COL_SALE_CUST_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUST_ID),
        FOREIGN KEY($COL_SALE_PROD_ID) REFERENCES $TABLE_PRODUCTS($COL_PROD_ID)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSES (
                $COL_EXP_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EXP_AMOUNT REAL,
                $COL_EXP_TYPE   TEXT,
                $COL_EXP_NOTE   TEXT,
                $COL_EXP_DATE   TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_CAPITALS (
                $COL_CAP_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAP_AMOUNT REAL,
                $COL_CAP_TYPE   TEXT,
                $COL_CAP_NOTE   TEXT,
                $COL_CAP_DATE   TEXT
            )
        """)

        // Account has only one row — created once here
        db.execSQL("""
            CREATE TABLE $TABLE_ACCOUNT (
                $COL_ACC_MONEY  REAL DEFAULT 0.0,
                $COL_ACC_PROFIT REAL DEFAULT 0.0,
                $COL_ACC_BUSINESS_NAME TEXT
            )
        """)

        // Insert that one row
        db.execSQL("INSERT INTO $TABLE_ACCOUNT VALUES (0.0, 0.0,'')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop everything and recreate when you bump DATABASE_VERSION
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SALES")

        db.execSQL("DROP TABLE IF EXISTS $TABLE_CUSTOMERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")

        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CAPITALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACCOUNT")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}