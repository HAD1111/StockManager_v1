package com.example.stockmaneger.ui.account

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.ExpenseDao
import com.example.stockmaneger.data.dao.CapitalDao
import com.example.stockmaneger.data.models.Capital
import com.example.stockmaneger.data.models.Expense
import com.example.stockmaneger.databinding.FragmentAccountBinding
import java.text.SimpleDateFormat
import java.util.*

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountDao: AccountDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var capitalDao: CapitalDao

    private lateinit var prefs: SharedPreferences



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountDao = AccountDao(requireContext())
        capitalDao = CapitalDao(requireContext())
        expenseDao = ExpenseDao(requireContext())
        prefs      = requireActivity().getSharedPreferences("stockmaneger_prefs", android.content.Context.MODE_PRIVATE)

        loadSummary()
        loadRecentCapitals()
        loadRecentExpenses()

        binding.btnAddMoney.setOnClickListener { showAddCapitalDialog() }
        binding.btnAddExpense.setOnClickListener { showAddExpenseDialog() }
        binding.tvViewAllCapitals.setOnClickListener {
            findNavController().navigate(R.id.capitalsFragment)
        }
        binding.tvViewAllExpenses.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }
        binding.btnEditBusinessName.setOnClickListener {
            showEditBusinessNameDialog()
        }
        binding.tvBusinessName.text = accountDao.getBusinessName().toString()
    }

    override fun onResume() {
        super.onResume()
        loadSummary()
        loadRecentCapitals()
        loadRecentExpenses()
    }

    private fun loadSummary() {
        binding.tvTotalMoney.text    = "$${String.format("%.2f", accountDao.getTotalMoney())}"
        binding.tvTotalProfit.text   = "$${String.format("%.2f", accountDao.getTotalProfit())}"
        binding.tvTotalExpenses.text = "$${String.format("%.2f", expenseDao.getTotalExpenses())}"
    }

    private fun loadRecentCapitals() {
        binding.layoutCapitalHistory.removeAllViews()
        val capitals = capitalDao.getAll().take(5)

        if (capitals.isEmpty()) {
            binding.layoutCapitalHistory.addView(emptyRow("No capitals yet"))
            return
        }



        capitals.forEachIndexed { index, capital ->
            if (index > 0) binding.layoutCapitalHistory.addView(dividerView())

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(42, 24, 42, 24)
            }

            val tvIcon = TextView(requireContext()).apply {
                text = "\uD83D\uDCB0"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 28 }
            }

            val leftBlock = LinearLayout(requireContext()).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvType = TextView(requireContext()).apply {
                text     = capital.type
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvNote = TextView(requireContext()).apply {
                text       = if (capital.note.isNotEmpty()) capital.note else capital.date
                textSize   = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }

            leftBlock.addView(tvType)
            leftBlock.addView(tvNote)

            val tvAmount = TextView(requireContext()).apply {
                text     = "$${String.format("%.2f", capital.amount)}"
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.primary
                ))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(tvIcon)
            row.addView(leftBlock)
            row.addView(tvAmount)
            binding.layoutCapitalHistory.addView(row)
        }
    }

    private fun loadRecentExpenses() {
        binding.layoutRecentExpenses.removeAllViews()
        val expenses = expenseDao.getAll().take(5)

        if (expenses.isEmpty()) {
            binding.layoutRecentExpenses.addView(emptyRow("No expenses yet"))
            return
        }

        val icons = mapOf(
            "Rent"           to "🏠",
            "Electricity"    to "⚡",
            "Salary"         to "👤",
            "Transportation" to "🚚",
            "Other"          to "💸"
        )

        expenses.forEachIndexed { index, expense ->
            if (index > 0) binding.layoutRecentExpenses.addView(dividerView())

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(42, 24, 42, 24)
            }

            val tvIcon = TextView(requireContext()).apply {
                text = icons[expense.type] ?: "💸"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 28 }
            }

            val leftBlock = LinearLayout(requireContext()).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvType = TextView(requireContext()).apply {
                text     = expense.type
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvNote = TextView(requireContext()).apply {
                text       = if (expense.note.isNotEmpty()) expense.note else expense.date
                textSize   = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }

            leftBlock.addView(tvType)
            leftBlock.addView(tvNote)

            val tvAmount = TextView(requireContext()).apply {
                text     = "-$${String.format("%.2f", expense.amount)}"
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.danger))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(tvIcon)
            row.addView(leftBlock)
            row.addView(tvAmount)
            binding.layoutRecentExpenses.addView(row)
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────

    private fun showAddCapitalDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val etAmount = EditText(requireContext()).apply {
            hint      = "Amount"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }


        val tvTypeLabel = TextView(requireContext()).apply {
            text    = "Capital Type"
            setPadding(0, 16, 0, 4)
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val etType = EditText(requireContext()).apply {
            hint      = "Type"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val etNote = EditText(requireContext()).apply {
            hint      = "Note (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val calendar   = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        layout.addView(tvTypeLabel)
        layout.addView(etType)
        layout.addView(etNote)
        layout.addView(btnDate)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Capital")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val type   = etType.text.toString().trim()
                val note   = etNote.text.toString().trim()

                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                capitalDao.insert(
                    Capital(
                        amount = amount,
                        type = type,
                        note = note,
                        date = selectedDate
                    )
                )


                accountDao.addMoney(amount)

                loadSummary()
                loadRecentCapitals()
                loadRecentExpenses()
                Toast.makeText(requireContext(), "Capital saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        val tvTypeLabel = TextView(requireContext()).apply {
            text    = "Expense Type"
            setPadding(0, 16, 0, 4)
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val types         = arrayOf("Rent", "Electricity", "Salary", "Transportation", "Other")
        val spinnerType   = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        }

        val etNote = EditText(requireContext()).apply {
            hint      = "Note (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val calendar   = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        layout.addView(tvTypeLabel)
        layout.addView(spinnerType)
        layout.addView(etNote)
        layout.addView(btnDate)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val type   = types[spinnerType.selectedItemPosition]
                val note   = etNote.text.toString().trim()

                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                expenseDao.insert(Expense(
                    amount = amount,
                    type   = type,
                    note   = note,
                    date   = selectedDate
                ))

                // Deduct from balance
                accountDao.deductMoney(amount)

                loadSummary()
                loadRecentExpenses()
                Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun emptyRow(message: String): TextView {
        return TextView(requireContext()).apply {
            text     = message
            textSize = 13f
            gravity  = Gravity.CENTER
            setPadding(42, 48, 42, 48)
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    private fun dividerView(): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(42, 0, 42, 0) }
            setBackgroundColor(requireContext().getColor(R.color.border))
        }
    }

    private fun showEditBusinessNameDialog() {
        val editText = EditText(requireContext())
        editText.setText(binding.tvBusinessName.text)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Business Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->

                val newName = editText.text.toString().trim()

                if (newName.isNotEmpty()) {
                    accountDao.updateBusinessName(newName)

                    binding.tvBusinessName.text = newName
                    requireActivity().recreate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}