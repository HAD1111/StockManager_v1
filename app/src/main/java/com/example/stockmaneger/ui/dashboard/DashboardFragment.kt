package com.example.stockmaneger.ui.dashboard

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.ExpenseDao
import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.databinding.FragmentDashboardBinding
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var saleDao: SaleDao
    private lateinit var productDao: ProductDao
    private lateinit var accountDao: AccountDao
    private lateinit var expenseDao: ExpenseDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao    = SaleDao(requireContext())
        productDao = ProductDao(requireContext())
        accountDao = AccountDao(requireContext())
        expenseDao = ExpenseDao(requireContext())

        setGreeting()
        loadMetrics()
        loadRecentSales()
        loadLowStock()
        loadRecentExpenses()
        // "View all" links
        binding.tvViewAllSales.setOnClickListener {
            findNavController().navigate(R.id.salesFragment)
        }
        binding.tvViewAllStock.setOnClickListener {
            findNavController().navigate(R.id.stockFragment)
        }
        binding.tvViewAllExpenses.setOnClickListener {
            findNavController().navigate(R.id.accountFragment)
        }
        binding.tvBusinessName.text = accountDao.getBusinessName().toString()
    }

    override fun onResume() {
        super.onResume()
        loadMetrics()
        loadRecentSales()
        loadLowStock()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
    }

    private fun loadMetrics() {
        // Balance
        val balance = accountDao.getTotalMoney()
        binding.tvBalance.text = "$${String.format(Locale.US, "%.2f", balance)}"

        // Revenue
        val revenue = saleDao.getTotalRevenue()
        binding.tvRevenue.text = "$${String.format(Locale.US, "%.2f", revenue)}"

        // Profit
        val profit = saleDao.getTotalProfit()
        binding.tvProfit.text = "$${String.format(Locale.US, "%.2f", profit)}"

        // Products + low stock count
        val products = productDao.getAll()
        binding.tvProductCount.text = products.size.toString()
        val lowCount = products.count { it.quantity <= 3 }
        if (lowCount > 0) {
            binding.tvLowStock.text = "⚠ $lowCount low stock"
            binding.tvLowStock.visibility = View.VISIBLE
        } else {
            binding.tvLowStock.visibility = View.GONE
        }

        // Expenses
        val totalExpenses = expenseDao.getTotalExpenses()
        binding.tvExpenses.text = "$${String.format(Locale.US, "%.2f", totalExpenses)}"
    }

    private fun loadRecentSales() {
        binding.llRecentSales.removeAllViews()
        val sales = saleDao.getAll().take(5)

        if (sales.isEmpty()) {
            binding.tvNoSales.visibility = View.VISIBLE
            return
        }

        binding.tvNoSales.visibility = View.GONE

        sales.forEach { sale ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(48, 28, 48, 28)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    findNavController().navigate(R.id.salesFragment)
                }
            }

            // Customer + items summary
            val info = LinearLayout(requireContext()).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(requireContext()).apply {
                text      = sale.customerName
                textSize  = 14f
                setTextColor(requireContext().getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvProduct = TextView(requireContext()).apply {
                val itemsStr = "${sale.itemCount} ${if (sale.itemCount == 1) "item" else "items"}"
                text      = "$itemsStr • ${sale.date}"
                textSize  = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }
            info.addView(tvName)
            info.addView(tvProduct)

            // Total + status
            val right = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity     = Gravity.END
            }
            val tvTotal = TextView(requireContext()).apply {
                text      = "Total: $${String.format(Locale.US, "%.2f", sale.totalAmount)}"
                textSize  = 14f
                setTextColor(requireContext().getColor(R.color.primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvPayment = TextView(requireContext()).apply {
                when {
                    sale.paidAmount > sale.totalAmount + 0.005 -> {
                        text = "Paid: $${String.format(Locale.US, "%.2f", sale.paidAmount)} (Overpaid)"
                        setTextColor(requireContext().getColor(R.color.success))
                    }
                    sale.remainingDebt <= 0.005 -> {
                        text = "Paid in Full (${sale.paymentMethod})"
                        setTextColor(requireContext().getColor(R.color.success))
                    }
                    sale.paidAmount > 0.005 -> {
                        text = "Paid: $${String.format(Locale.US, "%.2f", sale.paidAmount)} • Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                        setTextColor(requireContext().getColor(R.color.danger))
                    }
                    else -> {
                        text = "Unpaid • Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                        setTextColor(requireContext().getColor(R.color.danger))
                    }
                }
                textSize  = 11f
            }
            right.addView(tvTotal)
            right.addView(tvPayment)

            row.addView(info)
            row.addView(right)

            // Divider between rows
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.setMargins(48, 0, 48, 0) }
                setBackgroundColor(requireContext().getColor(R.color.border))
            }

            binding.llRecentSales.addView(row)
            binding.llRecentSales.addView(divider)
        }
    }

    private fun loadLowStock() {
        binding.llLowStock.removeAllViews()
        val lowProducts = productDao.getAll().filter { it.quantity <= 5 }
        val products = productDao.getAll()

        if (products.isEmpty()){
            binding.tvNoLowStock.visibility = View.VISIBLE
            binding.tvNoLowStock.text = "No Products Recorded"
            return
        }

        if (lowProducts.isEmpty()) {
            binding.tvNoLowStock.visibility = View.VISIBLE
            binding.tvNoLowStock.text = "All products are well stocked ✓"
            return
        }

        binding.tvNoLowStock.visibility = View.GONE

        lowProducts.forEach { product ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(48, 28, 48, 28)
            }

            val tvName = TextView(requireContext()).apply {
                text         = product.name
                textSize     = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(requireContext().getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val qtyColor = when {
                product.quantity == 0  -> requireContext().getColor(R.color.danger)
                product.quantity <= 3  -> requireContext().getColor(R.color.danger)
                else                   -> requireContext().getColor(R.color.warning)
            }

            val tvQty = TextView(requireContext()).apply {
                text      = "${product.quantity} left"
                textSize  = 13f
                setTextColor(qtyColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(tvName)
            row.addView(tvQty)

            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.setMargins(48, 0, 48, 0) }
                setBackgroundColor(requireContext().getColor(R.color.border))
            }

            binding.llLowStock.addView(row)
            binding.llLowStock.addView(divider)
        }
    }

    private fun loadRecentExpenses() {
        binding.layoutRecentExpenses.removeAllViews()
        val expenses = expenseDao.getAll().take(4)

        if (expenses.isEmpty()) {
            binding.layoutRecentExpenses.addView(emptyRow("No expenses recorded yet"))
            return
        }

        val expenseIcons = mapOf(
            "Rent"           to "🏠",
            "Electricity"    to "⚡",
            "Salary"         to "👤",
            "Transportation" to "🚚",
            "Other"          to "💸"
        )

        expenses.forEach { expense ->
            if (binding.layoutRecentExpenses.childCount > 0) {
                binding.layoutRecentExpenses.addView(dividerView())
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(42, 24, 42, 24)
            }

            val tvIcon = TextView(requireContext()).apply {
                text        = expenseIcons[expense.type] ?: "💸"
                textSize    = 20f
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

            val tvDate = TextView(requireContext()).apply {
                text     = expense.date
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }

            leftBlock.addView(tvType)
            leftBlock.addView(tvDate)

            val tvAmount = TextView(requireContext()).apply {
                text     = "-$${String.format(Locale.US, "%.2f", expense.amount)}"
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

    private fun emptyRow(message: String): TextView {
        return TextView(requireContext()).apply {
            text      = message
            textSize  = 13f
            gravity   = Gravity.CENTER
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
