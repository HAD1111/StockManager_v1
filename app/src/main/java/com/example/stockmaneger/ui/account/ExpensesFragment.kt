package com.example.stockmaneger.ui.account

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.ExpenseDao
import com.example.stockmaneger.data.models.Expense
import com.example.stockmaneger.databinding.FragmentExpensesBinding
import java.text.SimpleDateFormat
import java.util.*

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseDao: ExpenseDao
    private lateinit var accountDao: AccountDao
    private lateinit var adapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expenseDao = ExpenseDao(requireContext())
        accountDao = AccountDao(requireContext())

        adapter = ExpenseAdapter(
            expenses    = mutableListOf(),
            onLongClick = { expense -> showDeleteDialog(expense) }
        )

        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter       = adapter

        loadExpenses()

        binding.fabAddExpense.setOnClickListener { showAddExpenseDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun loadExpenses() {
        val list = expenseDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())
        binding.tvTotalExpenses.text = "-$${String.format("%.2f", expenseDao.getTotalExpenses())}"
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvExpenses.visibility = if (isEmpty) View.GONE   else View.VISIBLE
        binding.tvEmpty.visibility    = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showAddExpenseDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val etAmount = EditText(requireContext()).apply {
            hint      = "Amount"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val tvLabel = TextView(requireContext()).apply {
            text    = "Expense Type"
            setPadding(0, 16, 0, 4)
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val types       = arrayOf("Rent", "Electricity", "Salary", "Transportation", "Other")
        val spinner     = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        }

        val etNote = EditText(requireContext()).apply {
            hint      = "Note (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val calendar     = Calendar.getInstance()
        val dateFormat   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var selectedDate = dateFormat.format(calendar.time)

        val btnDate = Button(requireContext()).apply {
            text = "📅 Date: $selectedDate"
            setOnClickListener {
                DatePickerDialog(requireContext(), { _, y, m, d ->
                    calendar.set(y, m, d)
                    selectedDate = dateFormat.format(calendar.time)
                    text = "📅 Date: $selectedDate"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        layout.addView(etAmount)
        layout.addView(tvLabel)
        layout.addView(spinner)
        layout.addView(etNote)
        layout.addView(btnDate)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val type   = types[spinner.selectedItemPosition]
                val note   = etNote.text.toString().trim()

                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                expenseDao.insert(Expense(amount = amount, type = type, note = note, date = selectedDate))
                accountDao.deductMoney(amount)
                loadExpenses()
                Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Delete this ${expense.type} expense of $${expense.amount}?\nThe amount will be restored to your balance.")
            .setPositiveButton("Delete") { _, _ ->
                expenseDao.delete(expense.id)
                accountDao.addMoney(expense.amount)
                loadExpenses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}