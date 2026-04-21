package com.example.stockmaneger.data.models

data class Expense(
    val id: Int = 0,
    val amount: Double,
    val type: String,   // Rent, Electricity, Salary, Transport, Other
    val note: String,
    val date: String
)