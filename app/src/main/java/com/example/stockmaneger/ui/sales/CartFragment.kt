package com.example.stockmaneger.ui.sales

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.CustomerDao
import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.data.models.CartItem
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.databinding.FragmentCartBinding
import com.example.stockmaneger.ui.scanner.BarcodeScannerHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var saleDao: SaleDao
    private lateinit var productDao: ProductDao
    private lateinit var customerDao: CustomerDao
    private lateinit var barcodeScanner: BarcodeScannerHelper
    private lateinit var cartAdapter: CartAdapter

    private val cartItems = mutableListOf<CartItem>()
    private var allProducts = listOf<Product>()
    private var allCustomers = listOf<Customer>()

    private var selectedCustomerId: Int? = null
    private var previousCustomerDisplay: String = "Walk-in Customer (No Account)"
    private var selectedDate: String = ""
    private var userHasCustomPayment = false
    private var isCheckingOut = false

    private val paymentMethods = arrayOf("Cash", "OMT", "Whish", "Crypto")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize barcode scanner in onCreate to safely register ActivityResultLauncher
        barcodeScanner = BarcodeScannerHelper(this, requireContext()) { scannedBarcode ->
            val barcode = scannedBarcode?.trim() ?: return@BarcodeScannerHelper
            handleScannedBarcode(barcode)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao = SaleDao(requireContext())
        productDao = ProductDao(requireContext())
        customerDao = CustomerDao(requireContext())

        // Default to today's date
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(calendar.time)
        binding.btnDate.text = selectedDate

        binding.btnDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = dateFormat.format(calendar.time)
                    binding.btnDate.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup Cart RecyclerView
        cartAdapter = CartAdapter(
            cartItems = cartItems,
            onCartUpdated = { updateCartSummary() },
            onItemRemoved = { updateCartSummary() }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter

        // Setup Payment Method Spinner
        val paymentAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            paymentMethods
        )
        binding.spinnerPaymentMethod.adapter = paymentAdapter

        // Load customers & products
        loadCustomers()
        loadProducts()

        // New customer button
        binding.btnNewCustomer.setOnClickListener {
            showAddCustomerDialog()
        }

        // Barcode button
        binding.btnScanBarcode.setOnClickListener {
            barcodeScanner.scan()
        }

        // Quick payment chips
        binding.btnChipFull.setOnClickListener {
            userHasCustomPayment = false
            val total = cartItems.sumOf { it.subtotal }
            binding.etAmountPaid.setText(String.format(Locale.US, "%.2f", total))
            updateDebtDisplay()
        }

        binding.btnChipUnpaid.setOnClickListener {
            userHasCustomPayment = true
            binding.etAmountPaid.setText("0.00")
            updateDebtDisplay()
        }

        binding.etAmountPaid.doAfterTextChanged {
            if (binding.etAmountPaid.hasFocus()) {
                userHasCustomPayment = true
            }
            updateDebtDisplay()
        }

        // Complete sale button
        binding.btnCompleteSale.setOnClickListener {
            attemptCheckout()
        }

        updateCartSummary()
    }

    private fun loadCustomers(selectCustomerId: Int? = selectedCustomerId) {
        allCustomers = customerDao.getAll()
        val customerDisplayList = mutableListOf("Walk-in Customer (No Account)")
        customerDisplayList.addAll(allCustomers.map { "${it.name} ${if (it.phone.isNotEmpty()) "(${it.phone})" else ""}" })
        customerDisplayList.add("+ New Customer")

        val customerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            customerDisplayList
        )
        binding.actvCustomer.setAdapter(customerAdapter)

        val targetIndex = if (selectCustomerId != null) {
            val idx = allCustomers.indexOfFirst { it.id == selectCustomerId }
            if (idx >= 0) idx + 1 else 0
        } else {
            0
        }

        binding.actvCustomer.setText(customerDisplayList[targetIndex], false)
        selectedCustomerId = if (targetIndex == 0) null else allCustomers[targetIndex - 1].id
        previousCustomerDisplay = customerDisplayList[targetIndex]

        binding.actvCustomer.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    selectedCustomerId = null
                    previousCustomerDisplay = customerDisplayList[0]
                }
                customerDisplayList.size - 1 -> {
                    // User selected "+ New Customer" from dropdown
                    binding.actvCustomer.setText(previousCustomerDisplay, false)
                    showAddCustomerDialog()
                }
                else -> {
                    val customer = allCustomers[position - 1]
                    selectedCustomerId = customer.id
                    previousCustomerDisplay = customerDisplayList[position]
                }
            }
        }
    }

    private fun showAddCustomerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_customer, null)
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilCustomerName)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCustomerName)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etCustomerPhone)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Customer")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        var isSaving = false
        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            val name = etName.text?.toString()?.trim() ?: ""
            val phone = etPhone.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                tilName.error = "Customer name is required"
                etName.requestFocus()
                return@setOnClickListener
            } else {
                tilName.error = null
            }

            isSaving = true
            saveButton.isEnabled = false

            try {
                val newId = customerDao.insert(Customer(name = name, phone = phone))
                if (newId > 0) {
                    dialog.dismiss()
                    loadCustomers(selectCustomerId = newId.toInt())
                    Toast.makeText(requireContext(), "Customer '$name' added", Toast.LENGTH_SHORT).show()
                } else {
                    isSaving = false
                    saveButton.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to save customer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isSaving = false
                saveButton.isEnabled = true
                Toast.makeText(requireContext(), "Error saving customer: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProducts() {
        allProducts = productDao.getAll().filter { it.quantity > 0 }
        val productDisplayList = allProducts.map {
            "${it.name} • Stock: ${it.quantity} • $${String.format(Locale.US, "%.2f", it.salePrice)}"
        }

        val productAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            productDisplayList
        )
        binding.actvProductSearch.setAdapter(productAdapter)

        binding.actvProductSearch.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = allProducts[position]
            addProductToCart(selectedProduct)
            binding.actvProductSearch.setText("", false)
        }
    }

    private fun handleScannedBarcode(barcode: String) {
        val product = productDao.getByBarcode(barcode)
        if (product == null) {
            Toast.makeText(requireContext(), "Product with barcode '$barcode' not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (product.quantity <= 0) {
            Toast.makeText(requireContext(), "'${product.name}' is out of stock", Toast.LENGTH_SHORT).show()
            return
        }

        addProductToCart(product)
        Toast.makeText(requireContext(), "Added '${product.name}'", Toast.LENGTH_SHORT).show()
    }

    private fun addProductToCart(product: Product) {
        val existingIndex = cartItems.indexOfFirst { it.productId == product.id }
        if (existingIndex >= 0) {
            val existingItem = cartItems[existingIndex]
            if (existingItem.quantity < product.quantity) {
                existingItem.quantity++
                cartAdapter.notifyItemChanged(existingIndex)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Max available stock reached (${product.quantity}) for '${product.name}'",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val newItem = CartItem(
                product = product,
                quantity = 1,
                customUnitPrice = product.salePrice
            )
            cartItems.add(newItem)
            cartAdapter.notifyItemInserted(cartItems.size - 1)
            binding.rvCartItems.smoothScrollToPosition(cartItems.size - 1)
        }
        updateCartSummary()
    }

    private fun updateCartSummary() {
        val totalItemsCount = cartItems.sumOf { it.quantity }
        val totalAmount = cartItems.sumOf { it.subtotal }
        val totalCost = cartItems.sumOf { it.totalCost }
        val grossProfit = totalAmount - totalCost

        val isEmpty = cartItems.isEmpty()
        binding.layoutEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.cardCheckoutPanel.visibility = if (isEmpty) View.GONE else View.VISIBLE

        binding.tvTotalItems.text = "$totalItemsCount item${if (totalItemsCount == 1) "" else "s"}"
        binding.tvSaleTotal.text = "Total: $${String.format(Locale.US, "%.2f", totalAmount)}"

        val profitPrefix = if (grossProfit >= 0) "+" else ""
        binding.tvGrossProfit.text = "Profit: $profitPrefix$${String.format(Locale.US, "%.2f", grossProfit)}"
        binding.tvGrossProfit.setTextColor(
            requireContext().getColor(if (grossProfit >= 0) R.color.success else R.color.danger)
        )

        // If user hasn't explicitly entered a custom payment amount, auto-fill full payment
        if (!userHasCustomPayment) {
            binding.etAmountPaid.setText(String.format(Locale.US, "%.2f", totalAmount))
        }

        updateDebtDisplay()
    }

    private fun updateDebtDisplay() {
        val totalAmount = cartItems.sumOf { it.subtotal }
        val text = binding.etAmountPaid.text.toString().trim()
        val amountPaid = text.toDoubleOrNull() ?: 0.0

        val remainingDebt = (totalAmount - amountPaid).coerceAtLeast(0.0)
        binding.tvRemainingDebt.text = "$${String.format(Locale.US, "%.2f", remainingDebt)}"

        if (remainingDebt <= 0.005) {
            binding.tvRemainingDebt.setTextColor(requireContext().getColor(R.color.success))
        } else {
            binding.tvRemainingDebt.setTextColor(requireContext().getColor(R.color.danger))
        }

        // Disable payment method dropdown if paid amount is 0
        binding.spinnerPaymentMethod.isEnabled = amountPaid > 0.005

        // Update checkout button text to reflect exact action
        binding.btnCompleteSale.text = when {
            amountPaid <= 0.005 -> {
                "Complete Sale (Unpaid • Due: $${String.format(Locale.US, "%.2f", totalAmount)})"
            }
            amountPaid > totalAmount + 0.005 -> {
                "Complete Sale (Overpaid: $${String.format(Locale.US, "%.2f", amountPaid)} • No Due)"
            }
            remainingDebt <= 0.005 -> {
                "Complete Sale (Paid: $${String.format(Locale.US, "%.2f", amountPaid)})"
            }
            else -> {
                "Complete Sale (Pay: $${String.format(Locale.US, "%.2f", amountPaid)} • Due: $${String.format(Locale.US, "%.2f", remainingDebt)})"
            }
        }
    }

    private fun attemptCheckout() {
        if (isCheckingOut) return

        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one product to the cart", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = cartItems.sumOf { it.subtotal }
        val text = binding.etAmountPaid.text.toString().trim()
        val amountPaid = text.toDoubleOrNull()

        if (amountPaid == null || amountPaid < 0.0) {
            Toast.makeText(requireContext(), "Please enter a valid amount paid (>= 0)", Toast.LENGTH_SHORT).show()
            return
        }

        val paymentMethod = if (amountPaid > 0.005) {
            paymentMethods[binding.spinnerPaymentMethod.selectedItemPosition]
        } else {
            "Unpaid"
        }

        isCheckingOut = true
        binding.btnCompleteSale.isEnabled = false

        try {
            saleDao.checkout(
                customerId = selectedCustomerId,
                date = selectedDate,
                cartItems = cartItems,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod
            )

            Toast.makeText(requireContext(), "Sale completed successfully!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } catch (e: Exception) {
            isCheckingOut = false
            binding.btnCompleteSale.isEnabled = true
            Toast.makeText(requireContext(), "Checkout error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
