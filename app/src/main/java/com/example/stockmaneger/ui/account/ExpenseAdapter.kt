package com.example.stockmaneger.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.Expense
import com.example.stockmaneger.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private var expenses: MutableList<Expense>,
    private val onLongClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    private val icons = mapOf(
        "Rent"           to "🏠",
        "Electricity"    to "⚡",
        "Salary"         to "👤",
        "Transportation" to "🚚",
        "Other"          to "💸"
    )

    inner class ViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]
        with(holder.binding) {
            tvExpenseIcon.text   = icons[expense.type] ?: "💸"
            tvExpenseType.text   = expense.type
            tvExpenseNote.text   = expense.note.ifEmpty { "No note" }
            tvExpenseDate.text   = expense.date
            tvExpenseAmount.text = "-$${String.format("%.2f", expense.amount)}"

            root.setOnLongClickListener { onLongClick(expense); true }
        }
    }

    override fun getItemCount() = expenses.size

    fun updateList(newList: List<Expense>) {
        expenses.clear()
        expenses.addAll(newList)
        notifyDataSetChanged()
    }
}