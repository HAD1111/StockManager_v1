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
import com.example.stockmaneger.data.dao.CapitalDao
import com.example.stockmaneger.data.models.Capital
import com.example.stockmaneger.databinding.FragmentCapitalBinding
import java.text.SimpleDateFormat
import java.util.*

class CapitalFragment : Fragment() {

    private var _binding: FragmentCapitalBinding? = null
    private val binding get() = _binding!!

    private lateinit var capitalDao: CapitalDao
    private lateinit var accountDao: AccountDao
    private lateinit var adapter: CapitalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCapitalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capitalDao = CapitalDao(requireContext())
        accountDao = AccountDao(requireContext())

        adapter = CapitalAdapter(
            capitals    = mutableListOf(),
            onLongClick = { capital -> showDeleteDialog(capital) }
        )

        binding.rvCapitals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCapitals.adapter       = adapter

        loadCapitals()

        binding.fabAddCapital.setOnClickListener { showAddCapitalDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadCapitals()
    }

    private fun loadCapitals() {
        val list = capitalDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())
        binding.tvTotalCapitals.text = "$${String.format("%.2f", capitalDao.getTotalCapitals())}"
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvCapitals.visibility = if (isEmpty) View.GONE   else View.VISIBLE
        binding.tvEmpty.visibility    = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showAddCapitalDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val etAmount = EditText(requireContext()).apply {
            hint      = "Amount"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val tvLabel = TextView(requireContext()).apply {
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

                capitalDao.insert(Capital(amount = amount, type = type, note = note, date = selectedDate))
                accountDao.addMoney(amount)
                loadCapitals()
                Toast.makeText(requireContext(), "Capital saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(capital: Capital) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Delete this ${capital.type} capital of $${capital.amount}?\nThe amount will be deducted from your balance.")
            .setPositiveButton("Delete") { _, _ ->
                capitalDao.delete(capital.id)
                accountDao.deductMoney(capital.amount)
                loadCapitals()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}