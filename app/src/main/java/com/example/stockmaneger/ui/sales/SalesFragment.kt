package com.example.stockmaneger.ui.sales

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.FragmentSalesBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var saleDao: SaleDao
    private lateinit var adapter: SaleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao = SaleDao(requireContext())

        adapter = SaleAdapter(
            sales = mutableListOf(),
            onClick = { sale -> showSaleDetailsDialog(sale) },
            onLongClick = { sale -> showDeleteDialog(sale) }
        )

        binding.rvSales.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSales.adapter = adapter

        // Search as user types
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            val results = if (query.isEmpty()) {
                saleDao.getAll()
            } else {
                saleDao.searchSale(query)
            }
            adapter.updateList(results)
            toggleEmpty(results.isEmpty())
        }

        loadSales()

        // FAB navigates to the dedicated full-screen Cart page
        binding.fabAddSale.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSales()
    }

    private fun loadSales() {
        val list = saleDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())

        // Update summary bar with total agreed sales revenue and earned gross profit
        binding.tvTotalRevenue.text = "$${String.format(Locale.US, "%.2f", saleDao.getTotalRevenue())}"
        binding.tvTotalProfit.text = "$${String.format(Locale.US, "%.2f", saleDao.getTotalProfit())}"
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvSales.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    /**
     * Comprehensive Sale Details Dialog showing order line items, financial summary,
     * payment installments history, installment payment action, and deletion action.
     */
    private fun showSaleDetailsDialog(sale: Sale) {
        val items = saleDao.getItemsForSale(sale.id)
        val payments = saleDao.getPaymentsForSale(sale.id)

        val context = requireContext()
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        scroll.addView(layout)

        // Header
        val tvHeader = TextView(context).apply {
            text = "Order #${sale.id} • ${sale.date}"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.primary))
        }
        val tvCust = TextView(context).apply {
            text = "Customer: ${sale.customerName}${if (sale.customerPhone.isNotEmpty()) " (${sale.customerPhone})" else ""}"
            textSize = 14f
            setTextColor(context.getColor(R.color.text_primary))
            setPadding(0, 4, 0, 16)
        }
        layout.addView(tvHeader)
        layout.addView(tvCust)

        // Items Section Header
        val tvItemsHeader = TextView(context).apply {
            text = "ITEMS SOLD (${items.size})"
            textSize = 11f
            setTextColor(context.getColor(R.color.text_secondary))
            letterSpacing = 0.05f
            setPadding(0, 8, 0, 8)
        }
        layout.addView(tvItemsHeader)

        // List line items
        for (item in items) {
            val itemRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val left = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(context).apply {
                text = "${item.quantity}x ${item.productName}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(context.getColor(R.color.text_primary))
            }
            val tvUnit = TextView(context).apply {
                text = "@ $${String.format(Locale.US, "%.2f", item.unitPrice)} (Cost: $${String.format(Locale.US, "%.2f", item.unitCost)})"
                textSize = 12f
                setTextColor(context.getColor(R.color.text_secondary))
            }
            left.addView(tvName)
            left.addView(tvUnit)

            val tvSub = TextView(context).apply {
                text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(context.getColor(R.color.primary))
            }
            itemRow.addView(left)
            itemRow.addView(tvSub)
            layout.addView(itemRow)
        }

        // Financial Summary Block
        layout.addView(divider(context))
        val summaryLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }
        summaryLayout.addView(twoColumnRow(context, "Agreed Sale Value:", "$${String.format(Locale.US, "%.2f", sale.totalAmount)}", isBold = true))
        summaryLayout.addView(twoColumnRow(context, "Cost of Goods:", "$${String.format(Locale.US, "%.2f", sale.totalCost)}"))
        summaryLayout.addView(twoColumnRow(context, "Total Cash Paid:", "$${String.format(Locale.US, "%.2f", sale.paidAmount)}", isBold = true))
        summaryLayout.addView(twoColumnRow(context, "Recognized Revenue:", "$${String.format(Locale.US, "%.2f", sale.recognizedRevenue)}"))
        summaryLayout.addView(twoColumnRow(
            context,
            "Recognized Profit:",
            "$${String.format(Locale.US, "%.2f", sale.recognizedProfit)}",
            colorRes = if (sale.recognizedProfit > 0.005) R.color.success else R.color.text_primary,
            isBold = true
        ))
        summaryLayout.addView(twoColumnRow(
            context,
            "Outstanding Balance (Due):",
            "$${String.format(Locale.US, "%.2f", sale.remainingDebt)}",
            colorRes = if (sale.remainingDebt <= 0.005) R.color.success else R.color.danger,
            isBold = true
        ))
        layout.addView(summaryLayout)

        // Payments History Section
        layout.addView(divider(context))
        val tvPayHeader = TextView(context).apply {
            text = "PAYMENT HISTORY (${payments.size})"
            textSize = 11f
            setTextColor(context.getColor(R.color.text_secondary))
            letterSpacing = 0.05f
            setPadding(0, 8, 0, 8)
        }
        layout.addView(tvPayHeader)

        if (payments.isEmpty()) {
            val tvNoPay = TextView(context).apply {
                text = "No payments recorded (Unpaid sale)"
                textSize = 13f
                setTextColor(context.getColor(R.color.danger))
                setPadding(0, 4, 0, 8)
            }
            layout.addView(tvNoPay)
        } else {
            for (p in payments) {
                val pRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }
                val tvPDesc = TextView(context).apply {
                    text = "${p.paymentDate} • ${p.paymentMethod}${if (p.note.isNotEmpty()) " (${p.note})" else ""}"
                    textSize = 13f
                    setTextColor(context.getColor(R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvPAmount = TextView(context).apply {
                    text = "+$${String.format(Locale.US, "%.2f", p.amount)}"
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(context.getColor(R.color.success))
                }
                pRow.addView(tvPDesc)
                pRow.addView(tvPAmount)
                layout.addView(pRow)
            }
        }

        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle("Sale Details")
            .setView(scroll)
            .setPositiveButton("Close", null)

        // Allow adding an installment
        dialogBuilder.setNeutralButton("+ Add Payment") { _, _ ->
            showAddPaymentDialog(sale)
        }

        dialogBuilder.setNegativeButton("Delete Sale") { _, _ ->
            showDeleteDialog(sale)
        }

        dialogBuilder.show()
    }

    /**
     * Dialog to record a subsequent payment installment.
     */
    private fun showAddPaymentDialog(sale: Sale) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 0)
        }

        val tvPrompt = TextView(context).apply {
            text = if (sale.remainingDebt <= 0.005) {
                "Order #${sale.id} is already paid in full. Extra payment will be recorded as overpayment cash."
            } else {
                "Order #${sale.id} • Balance due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
            }
            textSize = 14f
            setTextColor(context.getColor(if (sale.remainingDebt <= 0.005) R.color.success else R.color.danger))
            setPadding(0, 0, 0, 12)
        }

        val etAmount = EditText(context).apply {
            hint = "Payment Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            // Pre-fill with remaining debt if any, else empty
            if (sale.remainingDebt > 0.005) {
                setText(String.format(Locale.US, "%.2f", sale.remainingDebt))
            }
        }

        val paymentMethods = arrayOf("Cash", "OMT", "Whish", "Crypto")
        val spinnerMethod = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        }

        val etNote = EditText(context).apply {
            hint = "Note (e.g. Installment #2)"
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var selectedDate = dateFormat.format(calendar.time)

        val btnDate = Button(context).apply {
            text = "📅 Date: $selectedDate"
            setOnClickListener {
                DatePickerDialog(context, { _, y, m, d ->
                    calendar.set(y, m, d)
                    selectedDate = dateFormat.format(calendar.time)
                    text = "📅 Date: $selectedDate"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        layout.addView(tvPrompt)
        layout.addView(etAmount)
        layout.addView(spinnerMethod)
        layout.addView(etNote)
        layout.addView(btnDate)

        AlertDialog.Builder(context)
            .setTitle("Record Payment")
            .setView(layout)
            .setPositiveButton("Save Payment") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val method = paymentMethods[spinnerMethod.selectedItemPosition]
                val note = etNote.text.toString().trim()

                if (amount == null || amount <= 0.0) {
                    Toast.makeText(context, "Please enter a valid payment amount > 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    saleDao.addPayment(
                        saleId = sale.id,
                        amount = amount,
                        paymentMethod = method,
                        date = selectedDate,
                        note = note
                    )
                    Toast.makeText(context, "Payment recorded successfully", Toast.LENGTH_SHORT).show()
                    loadSales()

                    // Re-open fresh details dialog with updated payments and balance
                    val freshSale = saleDao.getById(sale.id)
                    if (freshSale != null) {
                        showSaleDetailsDialog(freshSale)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(sale: Sale) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Sale #${sale.id}")
            .setMessage("Delete this sale? All inventory will be restored to stock, and actual recorded payments will be reversed from your balance.\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                saleDao.delete(sale.id)
                loadSales()
                Toast.makeText(requireContext(), "Sale deleted & stock restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun divider(context: android.content.Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 12, 0, 12)
            }
            setBackgroundColor(context.getColor(R.color.border))
        }
    }

    private fun twoColumnRow(
        context: android.content.Context,
        label: String,
        value: String,
        colorRes: Int = R.color.text_primary,
        isBold: Boolean = false
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)

            val tvLabel = TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(context.getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvVal = TextView(context).apply {
                text = value
                textSize = 14f
                setTextColor(context.getColor(colorRes))
                if (isBold) setTypeface(null, android.graphics.Typeface.BOLD)
            }
            addView(tvLabel)
            addView(tvVal)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
