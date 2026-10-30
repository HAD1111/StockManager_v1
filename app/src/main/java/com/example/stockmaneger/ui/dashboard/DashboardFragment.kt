package com.example.stockmaneger.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.ExpenseDao
import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var saleDao: SaleDao
    private lateinit var productDao: ProductDao
    private lateinit var accountDao: AccountDao
    private lateinit var expenseDao: ExpenseDao

    enum class ChartFilter {
        TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR
    }

    private var currentFilter = ChartFilter.THIS_WEEK

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao = SaleDao(requireContext())
        productDao = ProductDao(requireContext())
        accountDao = AccountDao(requireContext())
        expenseDao = ExpenseDao(requireContext())

        setupHeader()
        setupQuickActions()
        setupChartFilters()
        setupNavigationLinks()

        loadAllData()
    }

    override fun onResume() {
        super.onResume()
        setupHeader()
        loadAllData()
    }

    private fun setupHeader() {
        // Greeting based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }

        // Business Name
        val bizName = accountDao.getBusinessName()
        binding.tvBusinessName.text = if (bizName.isNotBlank()) bizName else "My Business"

        // Formatted natural date: e.g. "Friday, Oct 30, 2026"
        val datePattern = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        binding.tvCurrentDate.text = datePattern.format(Date())
    }

    private fun setupQuickActions() {
        // Add Sale (Cart)
        binding.btnQuickAddSale.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }

        // Add Product (Stock)
        binding.btnQuickAddProduct.setOnClickListener {
            findNavController().navigate(R.id.stockFragment)
        }

        // Add Expense
        binding.btnQuickAddExpense.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }
    }

    private fun setupNavigationLinks() {
        binding.tvViewAllSales.setOnClickListener {
            findNavController().navigate(R.id.salesFragment)
        }
        binding.tvViewAllStock.setOnClickListener {
            findNavController().navigate(R.id.stockFragment)
        }
        binding.tvViewAllExpenses.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }
    }

    private fun setupChartFilters() {
        binding.tabToday.setOnClickListener { setChartFilter(ChartFilter.TODAY) }
        binding.tabWeek.setOnClickListener { setChartFilter(ChartFilter.THIS_WEEK) }
        binding.tabMonth.setOnClickListener { setChartFilter(ChartFilter.THIS_MONTH) }
        binding.tabYear.setOnClickListener { setChartFilter(ChartFilter.THIS_YEAR) }
    }

    private fun setChartFilter(filter: ChartFilter) {
        if (currentFilter == filter) return
        currentFilter = filter
        updateChartFilterTabsUI()
        loadSalesPerformanceChart()
    }

    private fun updateChartFilterTabsUI() {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_unselected)
        val colorSelectedText = ContextCompat.getColor(requireContext(), R.color.surface)
        val colorUnselectedText = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        val tabs = listOf(
            ChartFilter.TODAY to binding.tabToday,
            ChartFilter.THIS_WEEK to binding.tabWeek,
            ChartFilter.THIS_MONTH to binding.tabMonth,
            ChartFilter.THIS_YEAR to binding.tabYear
        )

        for ((filter, textView) in tabs) {
            val isSelected = (filter == currentFilter)
            textView.background = if (isSelected) selectedBg else unselectedBg
            textView.setTextColor(if (isSelected) colorSelectedText else colorUnselectedText)
            textView.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun loadAllData() {
        try {
            loadFinancialOverview()
            loadSalesPerformanceChart()
            loadRecentSales()
            loadStockAlerts()
            loadRecentExpenses()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the 4 core financial metrics:
     * 1. Recognized Revenue: saleDao.getTotalRevenue()
     * 2. Recognized Profit: saleDao.getTotalProfit()
     * 3. Cash Balance: accountDao.getTotalMoney()
     * 4. Outstanding Debt: sum of all sales' remainingDebt
     */
    private fun loadFinancialOverview() {
        val revenue = saleDao.getTotalRevenue()
        binding.tvRevenue.text = "$${String.format(Locale.US, "%.2f", revenue)}"

        val profit = saleDao.getTotalProfit()
        binding.tvProfit.text = "$${String.format(Locale.US, "%.2f", profit)}"

        val balance = accountDao.getTotalMoney()
        binding.tvBalance.text = "$${String.format(Locale.US, "%.2f", balance)}"

        val allSales = saleDao.getAll()
        val totalDebt = allSales.sumOf { it.remainingDebt }
        binding.tvDebt.text = "$${String.format(Locale.US, "%.2f", totalDebt)}"

        if (totalDebt > 0.005) {
            binding.tvDebt.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
            binding.tvDebtCaption.text = "Unpaid by customers"
        } else {
            binding.tvDebt.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tvDebtCaption.text = "All accounts settled"
        }

        // Backward compatibility bindings for legacy layout elements
        val products = productDao.getAll()
        binding.tvProductCount.text = products.size.toString()
        val lowCount = products.count { it.quantity <= 3 }
        binding.tvLowStock.text = if (lowCount > 0) "⚠ $lowCount low stock" else ""
        binding.tvExpenses.text = "$${String.format(Locale.US, "%.2f", expenseDao.getTotalExpenses())}"
    }

    /**
     * Computes recognized revenue aggregated by payment transaction date.
     * For each sale, payments incrementally recognize revenue capped at the sale's total.
     */
    private fun getRecognizedRevenueByDate(): Map<String, Double> {
        val revenueByDate = mutableMapOf<String, Double>()
        val allSales = saleDao.getAll()

        for (sale in allSales) {
            val payments = saleDao.getPaymentsForSale(sale.id)
            var cumulativePaid = 0.0
            for (payment in payments) {
                val priorPaid = cumulativePaid
                val newPaid = priorPaid + payment.amount
                cumulativePaid = newPaid

                val priorRecognized = priorPaid.coerceAtMost(sale.totalAmount)
                val newRecognized = newPaid.coerceAtMost(sale.totalAmount)
                val deltaRevenue = (newRecognized - priorRecognized).coerceAtLeast(0.0)

                val dateKey = payment.paymentDate.trim().take(10)
                if (deltaRevenue > 0.0 && dateKey.isNotEmpty()) {
                    val current = revenueByDate[dateKey] ?: 0.0
                    revenueByDate[dateKey] = current + deltaRevenue
                }
            }
        }
        return revenueByDate
    }

    /**
     * Generates chart entries and period revenue for the selected time filter.
     */
    private fun loadSalesPerformanceChart() {
        val revenueByDate = getRecognizedRevenueByDate()
        val entries = mutableListOf<SalesPerformanceChartView.BarEntry>()
        var periodRevenue = 0.0

        when (currentFilter) {
            ChartFilter.TODAY -> {
                val cal = Calendar.getInstance()
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                val todayLabel = SimpleDateFormat("MMM d", Locale.US).format(cal.time)
                val todayRevenue = revenueByDate[todayKey] ?: 0.0

                entries.add(
                    SalesPerformanceChartView.BarEntry(
                        label = "Today ($todayLabel)",
                        value = todayRevenue,
                        isCurrent = true
                    )
                )
                periodRevenue = todayRevenue
            }

            ChartFilter.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

                for (i in 0..6) {
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                    val dayLabel = SimpleDateFormat("EEE", Locale.US).format(cal.time)
                    val rev = revenueByDate[dateKey] ?: 0.0

                    entries.add(
                        SalesPerformanceChartView.BarEntry(
                            label = dayLabel,
                            value = rev,
                            isCurrent = (dateKey == todayKey)
                        )
                    )
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                periodRevenue = entries.sumOf { it.value }
            }

            ChartFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH)
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)

                val intervals = mutableListOf(
                    1..7 to "1-7",
                    8..14 to "8-14",
                    15..21 to "15-21",
                    22..28 to "22-28"
                )
                if (maxDays > 28) {
                    intervals.add(29..maxDays to "29+")
                }

                for ((range, label) in intervals) {
                    var sum = 0.0
                    for (day in range) {
                        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth + 1, day)
                        sum += revenueByDate[dateKey] ?: 0.0
                    }
                    entries.add(
                        SalesPerformanceChartView.BarEntry(
                            label = label,
                            value = sum,
                            isCurrent = (currentDay in range)
                        )
                    )
                }
                periodRevenue = entries.sumOf { it.value }
            }

            ChartFilter.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH)
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                for (idx in monthNames.indices) {
                    val prefix = String.format(Locale.US, "%04d-%02d", currentYear, idx + 1)
                    val sum = revenueByDate.filter { it.key.startsWith(prefix) }.values.sum()
                    entries.add(
                        SalesPerformanceChartView.BarEntry(
                            label = monthNames[idx],
                            value = sum,
                            isCurrent = (idx == currentMonth)
                        )
                    )
                }
                periodRevenue = entries.sumOf { it.value }
            }
        }

        binding.salesChartView.setChartData(entries)
        binding.tvChartPeriodRevenue.text = "Revenue: $${String.format(Locale.US, "%.2f", periodRevenue)}"
    }

    /**
     * Loads the 5 most recent sales into a compact, professional transaction list.
     * Tapping any sale opens its comprehensive details dialog.
     */
    private fun loadRecentSales() {
        binding.llRecentSales.removeAllViews()
        val sales = saleDao.getAll().take(5)

        if (sales.isEmpty()) {
            binding.tvNoSales.visibility = View.VISIBLE
            return
        }

        binding.tvNoSales.visibility = View.GONE

        sales.forEachIndexed { index, sale ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(13), dpToPx(16), dpToPx(13))
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    showSaleDetailsDialog(sale)
                }
            }

            // Customer info & date summary
            val infoLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvCustName = TextView(requireContext()).apply {
                text = sale.customerName
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvMeta = TextView(requireContext()).apply {
                val itemsStr = "${sale.itemCount} ${if (sale.itemCount == 1) "item" else "items"}"
                text = "$itemsStr • ${sale.date}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setPadding(0, dpToPx(2), 0, 0)
            }

            infoLayout.addView(tvCustName)
            infoLayout.addView(tvMeta)

            // Right side: Total amount & payment status badge
            val rightLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            val tvTotal = TextView(requireContext()).apply {
                text = "$${String.format(Locale.US, "%.2f", sale.totalAmount)}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvStatusBadge = TextView(requireContext()).apply {
                textSize = 11f
                setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(3) }
                layoutParams = lp

                when {
                    sale.paidAmount > sale.totalAmount + 0.005 -> {
                        text = "Overpaid ($${String.format(Locale.US, "%.2f", sale.paidAmount)})"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                        setBackgroundResource(R.drawable.bg_badge_success)
                    }
                    sale.remainingDebt <= 0.005 -> {
                        text = "Paid in Full"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                        setBackgroundResource(R.drawable.bg_badge_success)
                    }
                    sale.paidAmount > 0.005 -> {
                        text = "Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                        setBackgroundResource(R.drawable.bg_badge_loss)
                    }
                    else -> {
                        text = "Unpaid • Due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                        setBackgroundResource(R.drawable.bg_badge_loss)
                    }
                }
            }

            rightLayout.addView(tvTotal)
            rightLayout.addView(tvStatusBadge)

            row.addView(infoLayout)
            row.addView(rightLayout)

            binding.llRecentSales.addView(row)

            // Divider between items (except last)
            if (index < sales.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                    ).also { it.setMargins(dpToPx(16), 0, dpToPx(16), 0) }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border))
                }
                binding.llRecentSales.addView(divider)
            }
        }
    }

    /**
     * Loads products requiring attention based on existing stock thresholds (qty <= 5).
     */
    private fun loadStockAlerts() {
        binding.llLowStock.removeAllViews()
        val allProducts = productDao.getAll()
        val lowProducts = allProducts.filter { it.quantity <= 5 }

        if (allProducts.isEmpty()) {
            binding.tvNoLowStock.visibility = View.VISIBLE
            binding.tvNoLowStockText.text = "No products recorded yet"
            return
        }

        if (lowProducts.isEmpty()) {
            binding.tvNoLowStock.visibility = View.VISIBLE
            binding.tvNoLowStockText.text = "All stock levels look good"
            return
        }

        binding.tvNoLowStock.visibility = View.GONE

        lowProducts.take(5).forEachIndexed { index, product ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    findNavController().navigate(R.id.stockFragment)
                }
            }

            // Avatar circle with initial letter
            val avatarFrame = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                    marginEnd = dpToPx(12)
                }
                setBackgroundResource(R.drawable.bg_avatar)
            }
            val tvInitial = TextView(requireContext()).apply {
                text = product.name.take(1).uppercase(Locale.getDefault())
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            avatarFrame.addView(tvInitial)

            // Product name and price
            val infoLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(requireContext()).apply {
                text = product.name
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvPrice = TextView(requireContext()).apply {
                text = "Sale: $${String.format(Locale.US, "%.2f", product.salePrice)} • Cost: $${String.format(Locale.US, "%.2f", product.basePrice)}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            infoLayout.addView(tvName)
            infoLayout.addView(tvPrice)

            // Stock status badge
            val tvQtyBadge = TextView(requireContext()).apply {
                textSize = 11f
                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))

                when {
                    product.quantity == 0 -> {
                        text = "Out of Stock"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                        setBackgroundResource(R.drawable.bg_badge_loss)
                    }
                    product.quantity <= 3 -> {
                        text = "${product.quantity} left (Critical)"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                        setBackgroundResource(R.drawable.bg_badge_loss)
                    }
                    else -> {
                        text = "${product.quantity} left (Low)"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                        setBackgroundResource(R.drawable.bg_badge_neutral)
                    }
                }
            }

            row.addView(avatarFrame)
            row.addView(infoLayout)
            row.addView(tvQtyBadge)

            binding.llLowStock.addView(row)

            if (index < lowProducts.take(5).size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                    ).also { it.setMargins(dpToPx(16), 0, dpToPx(16), 0) }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border))
                }
                binding.llLowStock.addView(divider)
            }
        }
    }

    /**
     * Loads the 4 most recent business expenses into a clean list.
     */
    private fun loadRecentExpenses() {
        binding.layoutRecentExpenses.removeAllViews()
        val expenses = expenseDao.getAll().take(4)

        if (expenses.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No expenses recorded yet"
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            binding.layoutRecentExpenses.addView(emptyTv)
            return
        }

        val expenseIcons = mapOf(
            "Rent" to "🏠",
            "Electricity" to "⚡",
            "Salary" to "👤",
            "Transportation" to "🚚",
            "Other" to "💸"
        )

        expenses.forEachIndexed { index, expense ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    findNavController().navigate(R.id.expensesFragment)
                }
            }

            val tvIcon = TextView(requireContext()).apply {
                text = expenseIcons[expense.type] ?: "💸"
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dpToPx(12) }
            }

            val infoLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvType = TextView(requireContext()).apply {
                text = expense.type
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvDate = TextView(requireContext()).apply {
                text = expense.date
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            infoLayout.addView(tvType)
            infoLayout.addView(tvDate)

            val tvAmount = TextView(requireContext()).apply {
                text = "-$${String.format(Locale.US, "%.2f", expense.amount)}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(tvIcon)
            row.addView(infoLayout)
            row.addView(tvAmount)

            binding.layoutRecentExpenses.addView(row)

            if (index < expenses.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                    ).also { it.setMargins(dpToPx(16), 0, dpToPx(16), 0) }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border))
                }
                binding.layoutRecentExpenses.addView(divider)
            }
        }
    }

    /**
     * Opens a comprehensive Sale Details Dialog showing order line items, financial summary,
     * payment installment history, and quick payment entry if balance remains.
     */
    private fun showSaleDetailsDialog(sale: Sale) {
        val items = saleDao.getItemsForSale(sale.id)
        val payments = saleDao.getPaymentsForSale(sale.id)
        val context = requireContext()

        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
        }
        scroll.addView(layout)

        // Header
        val tvHeader = TextView(context).apply {
            text = "Order #${sale.id} • ${sale.date}"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
        }
        val tvCust = TextView(context).apply {
            text = "Customer: ${sale.customerName}${if (sale.customerPhone.isNotEmpty()) " (${sale.customerPhone})" else ""}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        }
        layout.addView(tvHeader)
        layout.addView(tvCust)

        // Line Items Section Header
        val tvItemsHeader = TextView(context).apply {
            text = "ITEMS SOLD (${items.size})"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            letterSpacing = 0.08f
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        layout.addView(tvItemsHeader)

        for (item in items) {
            val itemRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(6), 0, dpToPx(6))
            }
            val left = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(context).apply {
                text = "${item.quantity}x ${item.productName}"
                textSize = 13.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            val tvUnit = TextView(context).apply {
                text = "@ $${String.format(Locale.US, "%.2f", item.unitPrice)} (Cost: $${String.format(Locale.US, "%.2f", item.unitCost)})"
                textSize = 11.5f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            left.addView(tvName)
            left.addView(tvUnit)

            val tvSub = TextView(context).apply {
                text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"
                textSize = 13.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            itemRow.addView(left)
            itemRow.addView(tvSub)
            layout.addView(itemRow)
        }

        // Financial Summary Box
        val summaryBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.box_back)
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dpToPx(14), 0, dpToPx(14)) }
            layoutParams = lp
        }

        fun addSummaryRow(title: String, value: String, colorRes: Int, isBold: Boolean = false) {
            val r = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(2), 0, dpToPx(2))
            }
            val t = TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val v = TextView(context).apply {
                text = value
                textSize = 13.5f
                setTextColor(ContextCompat.getColor(context, colorRes))
                if (isBold) setTypeface(null, android.graphics.Typeface.BOLD)
            }
            r.addView(t)
            r.addView(v)
            summaryBox.addView(r)
        }

        addSummaryRow("Total Payable:", "$${String.format(Locale.US, "%.2f", sale.totalAmount)}", R.color.text_primary, true)
        addSummaryRow("Total Paid to Date:", "$${String.format(Locale.US, "%.2f", sale.paidAmount)}", R.color.primary, true)
        val debtColor = if (sale.remainingDebt <= 0.005) R.color.success else R.color.danger
        addSummaryRow("Outstanding Due:", "$${String.format(Locale.US, "%.2f", sale.remainingDebt)}", debtColor, true)
        addSummaryRow("Recognized Gross Profit:", "$${String.format(Locale.US, "%.2f", sale.profit)}", R.color.success, true)

        layout.addView(summaryBox)

        // Payment History
        val tvPayHeader = TextView(context).apply {
            text = "PAYMENTS RECORDED (${payments.size})"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            letterSpacing = 0.08f
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        layout.addView(tvPayHeader)

        for (pay in payments) {
            val payRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            val pLeft = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val pMethod = TextView(context).apply {
                text = "${pay.paymentMethod} • ${pay.paymentDate}"
                textSize = 12.5f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            if (pay.note.isNotEmpty()) {
                val pNote = TextView(context).apply {
                    text = pay.note
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                pLeft.addView(pNote)
            }
            pLeft.addView(pMethod)

            val pAmount = TextView(context).apply {
                text = "$${String.format(Locale.US, "%.2f", pay.amount)}"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            payRow.addView(pLeft)
            payRow.addView(pAmount)
            layout.addView(payRow)
        }

        val builder = AlertDialog.Builder(context)
            .setTitle("Sale Details")
            .setView(scroll)
            .setPositiveButton("Close", null)

        if (sale.remainingDebt > 0.005) {
            builder.setNeutralButton("+ Add Payment") { _, _ ->
                showAddPaymentDialog(sale)
            }
        }

        builder.show()
    }

    /**
     * Dialog to record a subsequent payment installment directly from the Dashboard.
     */
    private fun showAddPaymentDialog(sale: Sale) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(8))
        }

        val tvPrompt = TextView(context).apply {
            text = "Order #${sale.id} • Balance due: $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.danger))
            setPadding(0, 0, 0, dpToPx(14))
        }
        layout.addView(tvPrompt)

        val etAmount = EditText(context).apply {
            hint = "Amount to Pay ($)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", sale.remainingDebt))
        }
        layout.addView(etAmount)

        val etNote = EditText(context).apply {
            hint = "Note (Optional, e.g. Cash installment)"
            setPadding(0, dpToPx(12), 0, 0)
        }
        layout.addView(etNote)

        AlertDialog.Builder(context)
            .setTitle("Record Payment")
            .setView(layout)
            .setPositiveButton("Save Payment") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val note = etNote.text.toString().trim()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                if (amount == null || amount <= 0.0) {
                    Toast.makeText(context, "Please enter a valid amount > 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    saleDao.addPayment(
                        saleId = sale.id,
                        amount = amount,
                        paymentMethod = "Cash",
                        date = todayStr,
                        note = note
                    )
                    Toast.makeText(context, "Payment recorded successfully", Toast.LENGTH_SHORT).show()
                    loadAllData()

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

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
