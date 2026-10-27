// ui/customers/CustomersFragment.kt
package com.example.stockmaneger.ui.customers

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.CustomerDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.FragmentCustomersBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomersFragment : Fragment() {

    private var _binding: FragmentCustomersBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerDao: CustomerDao
    private lateinit var saleDao: SaleDao
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customerDao = CustomerDao(requireContext())
        saleDao = SaleDao(requireContext())

        // Setup RecyclerView
        adapter = CustomerAdapter(
            customers   = mutableListOf(),
            getDebt     = { id -> customerDao.getCustomerDebt(id) },
            onClick     = { customer -> showCustomerDetailsDialog(customer) },
            onLongClick = { customer -> showDeleteDialog(customer) }
        )
        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomers.adapter = adapter

        loadCustomers()

        // Search as user types
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            val results = if (query.isEmpty())
                customerDao.getAll()
            else
                customerDao.searchByName(query)
            adapter.updateList(results)
            toggleEmpty(results.isEmpty())
        }

        // FAB opens add dialog
        binding.fabAddCustomer.setOnClickListener {
            showAddDialog()
        }
    }

    private fun loadCustomers() {
        val list = customerDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvCustomers.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmpty.visibility     = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showCustomerDetailsDialog(customer: Customer) {
        val context = requireContext()
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 24)
        }
        scroll.addView(layout)

        val totalDebt = customerDao.getCustomerDebt(customer.id)
        val sales = saleDao.getByCustomer(customer.id)

        // Contact info
        val tvPhone = TextView(context).apply {
            text = "Phone: ${customer.phone.ifEmpty { "No phone recorded" }}"
            textSize = 14f
            setTextColor(context.getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 8)
        }
        layout.addView(tvPhone)

        // Outstanding Debt Banner
        val debtBanner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        val tvDebtLabel = TextView(context).apply {
            text = "Total Outstanding Debt: "
            textSize = 15f
            setTextColor(context.getColor(R.color.text_primary))
        }
        val tvDebtVal = TextView(context).apply {
            text = "$${String.format(Locale.US, "%.2f", totalDebt)}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(if (totalDebt > 0.005) R.color.danger else R.color.success))
        }
        debtBanner.addView(tvDebtLabel)
        debtBanner.addView(tvDebtVal)
        layout.addView(debtBanner)

        // Divider
        val divider1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { setMargins(0, 8, 0, 16) }
            setBackgroundColor(context.getColor(R.color.border))
        }
        layout.addView(divider1)

        // Sales section header
        val tvSalesHeader = TextView(context).apply {
            text = "SALES HISTORY (${sales.size})"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.text_secondary))
            letterSpacing = 0.05f
            setPadding(0, 0, 0, 12)
        }
        layout.addView(tvSalesHeader)

        var detailsDialog: AlertDialog? = null

        if (sales.isEmpty()) {
            val tvEmptySales = TextView(context).apply {
                text = "No sales recorded for this customer."
                textSize = 13f
                setTextColor(context.getColor(R.color.text_secondary))
                setPadding(0, 4, 0, 8)
            }
            layout.addView(tvEmptySales)
        } else {
            for (sale in sales) {
                val saleCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 12, 16, 12)
                    setBackgroundResource(R.drawable.box_back)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, 0, 12)
                    layoutParams = lp
                }

                // Row 1: Order # and Date
                val headerRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val tvOrder = TextView(context).apply {
                    text = "Order #${sale.id}"
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(context.getColor(R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvDate = TextView(context).apply {
                    text = sale.date
                    textSize = 12f
                    setTextColor(context.getColor(R.color.text_secondary))
                }
                headerRow.addView(tvOrder)
                headerRow.addView(tvDate)
                saleCard.addView(headerRow)

                // Row 2: Items and Financials
                val tvSummary = TextView(context).apply {
                    text = "${sale.itemCount} item(s) • Total: $${String.format(Locale.US, "%.2f", sale.totalAmount)} • Paid: $${String.format(Locale.US, "%.2f", sale.paidAmount)}"
                    textSize = 13f
                    setTextColor(context.getColor(R.color.text_secondary))
                    setPadding(0, 4, 0, 4)
                }
                saleCard.addView(tvSummary)

                // Row 3: Debt status + Pay button if debt > 0
                val bottomRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                val tvDebtStatus = TextView(context).apply {
                    when {
                        sale.paidAmount > sale.totalAmount + 0.005 -> {
                            text = "Paid (Overpaid • No Due)"
                            setTextColor(context.getColor(R.color.success))
                        }
                        sale.remainingDebt <= 0.005 -> {
                            text = "Paid in Full"
                            setTextColor(context.getColor(R.color.success))
                        }
                        else -> {
                            text = "Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                            setTextColor(context.getColor(R.color.danger))
                        }
                    }
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                bottomRow.addView(tvDebtStatus)

                if (sale.remainingDebt > 0.005) {
                    val btnPay = Button(context).apply {
                        text = "Pay"
                        textSize = 12f
                        setBackgroundColor(context.getColor(R.color.primary))
                        setTextColor(context.getColor(R.color.surface))
                        setOnClickListener {
                            detailsDialog?.dismiss()
                            showAddPaymentDialog(sale) {
                                loadCustomers()
                                showCustomerDetailsDialog(customer)
                            }
                        }
                    }
                    bottomRow.addView(btnPay)
                }

                saleCard.addView(bottomRow)
                layout.addView(saleCard)
            }
        }

        detailsDialog = AlertDialog.Builder(context)
            .setTitle(customer.name)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit Info") { _, _ ->
                showEditDialog(customer)
            }
            .show()
    }

    private fun showAddPaymentDialog(sale: Sale, onSuccess: (() -> Unit)? = null) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val tvInfo = TextView(context).apply {
            text = "Order #${sale.id} • Balance Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.text_primary))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(tvInfo)

        val etAmount = EditText(context).apply {
            hint = "Payment Amount ($)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", sale.remainingDebt))
        }
        layout.addView(etAmount)

        val spinnerMethod = Spinner(context).apply {
            val methods = arrayOf("Cash", "Card", "Bank Transfer", "Other")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, methods)
        }
        layout.addView(spinnerMethod)

        val etNote = EditText(context).apply {
            hint = "Note (e.g. installment, check #)"
        }
        layout.addView(etNote)

        AlertDialog.Builder(context)
            .setTitle("Record Installment Payment")
            .setView(layout)
            .setPositiveButton("Record") { _, _ ->
                val amountStr = etAmount.text.toString().trim()
                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    Toast.makeText(context, "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val method = spinnerMethod.selectedItem.toString()
                val note = etNote.text.toString().trim()
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

                try {
                    saleDao.addPayment(sale.id, amount, method, note, date)
                    Toast.makeText(context, "Payment of $${String.format(Locale.US, "%.2f", amount)} recorded", Toast.LENGTH_SHORT).show()
                    loadCustomers()
                    onSuccess?.invoke()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error saving payment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDialog() {
        val layout = buildFormLayout()
        val etName  = layout.getChildAt(0) as EditText
        val etPhone = layout.getChildAt(1) as EditText

        AlertDialog.Builder(requireContext())
            .setTitle("Add Customer")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty()) {
                    customerDao.insert(Customer(name = name, phone = phone))
                    loadCustomers()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(customer: Customer) {
        val layout = buildFormLayout(customer.name, customer.phone)
        val etName  = layout.getChildAt(0) as EditText
        val etPhone = layout.getChildAt(1) as EditText

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Customer")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty()) {
                    customerDao.update(Customer(customer.id, name, phone))
                    loadCustomers()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(customer: Customer) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Customer")
            .setMessage("Delete ${customer.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val result = customerDao.delete(customer.id)
                if (result == 0) {
                    Toast.makeText(
                        this.context,
                        "Customer has sales. Can't delete!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this.context,
                        "Customer deleted!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                loadCustomers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildFormLayout(name: String = "", phone: String = ""): LinearLayout {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }
        val etName = EditText(requireContext()).apply {
            hint = "Full name"
            setText(name)
        }
        val etPhone = EditText(requireContext()).apply {
            hint = "Phone number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(phone)
        }
        layout.addView(etName)
        layout.addView(etPhone)
        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
