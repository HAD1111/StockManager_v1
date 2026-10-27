package com.example.stockmaneger.ui.sales

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.CustomerDao

import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.FragmentSalesBinding
import com.example.stockmaneger.ui.scanner.BarcodeScannerHelper
import java.text.SimpleDateFormat
import java.util.*

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var barcodeScanner: BarcodeScannerHelper
    private lateinit var saleDao: SaleDao
    private lateinit var customerDao: CustomerDao
    private lateinit var productDao: ProductDao
    private lateinit var accountDao: AccountDao
    private lateinit var adapter: SaleAdapter
    private var currentProducts: List<Product> = emptyList()
    private var currentSpinner: Spinner? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao     = SaleDao(requireContext())
        customerDao = CustomerDao(requireContext())
        productDao  = ProductDao(requireContext())
        accountDao  = AccountDao(requireContext())

        barcodeScanner = BarcodeScannerHelper(
            this,
            requireContext()
        ) { scannedBarcode ->

            val barcode = scannedBarcode ?: return@BarcodeScannerHelper

            val product = productDao.getByBarcode(barcode)

            if (product == null) {
                Toast.makeText(
                    requireContext(),
                    "Product not found",
                    Toast.LENGTH_SHORT
                ).show()
                return@BarcodeScannerHelper
            }

            val index = currentProducts.indexOfFirst {
                it.id == product.id
            }

            if (index >= 0) {
                currentSpinner?.setSelection(index)
            }
        }
        adapter = SaleAdapter(
            sales       = mutableListOf(),
            onLongClick = { sale -> showDeleteDialog(sale) }
        )

        binding.rvSales.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSales.adapter       = adapter

        // Search as user types
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            val results = if (query.isEmpty())
                saleDao.getAll()
            else
                saleDao.searchSale(query)
            adapter.updateList(results)
            toggleEmpty(results.isEmpty())
        }

        loadSales()

        binding.fabAddSale.setOnClickListener {
            showAddSaleDialog()
        }
    }

    private fun loadSales() {
        val list = saleDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())

        // Update summary
        binding.tvTotalRevenue.text = "$${String.format("%.2f", saleDao.getTotalRevenue())}"
        binding.tvTotalProfit.text  = "$${String.format("%.2f", saleDao.getTotalProfit())}"
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvSales.visibility = if (isEmpty) View.GONE  else View.VISIBLE
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showAddSaleDialog() {
        val products = productDao.getAll()
        if (products.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No products in stock. Please add a product first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        // Customer autocomplete
        val customers   = customerDao.getAll()
        val customerNames = customers.map { it.name }.toTypedArray()
        val etCustomer  = AutoCompleteTextView(requireContext()).apply {
            hint      = "Customer name"
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, customerNames))
            threshold = 1
        }
        val etPhoneNumber = EditText(requireContext()).apply {
            hint        = "Phone Number"
            inputType   = InputType.TYPE_CLASS_NUMBER
        }
        // Product spinner
        currentProducts = products
        val productNames = products.map { "${it.name} (${it.quantity} left)" }.toTypedArray()
        val tvProductLabel = TextView(requireContext()).apply {
            text    = "Select Product"
            setPadding(0, 16, 0, 4)
            setTextColor(requireContext().getColor(com.example.stockmaneger.R.color.text_secondary))
        }
        val spinnerProduct = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, productNames)
        }
        currentSpinner = spinnerProduct
        val btnScanBarcode = Button(requireContext()).apply {
            text = "Scan Product Barcode"
        }

        btnScanBarcode.setOnClickListener {
            barcodeScanner.scan()
        }

        // Quantity
        val etQty = EditText(requireContext()).apply {
            hint      = "Quantity"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        // Unit price — auto fills when product selected
        val etUnitPrice = EditText(requireContext()).apply {
            hint      = "Unit sale price"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        // Auto fill price when product selected
        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (pos in products.indices) {
                    etUnitPrice.setText(products[pos].salePrice.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Payment method
        val tvPaymentLabel = TextView(requireContext()).apply {
            text    = "Payment Method"
            setPadding(0, 16, 0, 4)
            setTextColor(requireContext().getColor(com.example.stockmaneger.R.color.text_secondary))
        }
        val paymentMethods = arrayOf("Cash", "Omt", "Whish", "Crypto")
        val spinnerPayment = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        }

        // Date picker
        val calendar = Calendar.getInstance()
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

        layout.addView(etCustomer)
        layout.addView(etPhoneNumber)
        layout.addView(tvProductLabel)
        layout.addView(spinnerProduct)
        layout.addView(btnScanBarcode)
        layout.addView(etQty)
        layout.addView(etUnitPrice)
        layout.addView(tvPaymentLabel)
        layout.addView(spinnerPayment)
        layout.addView(btnDate)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Sale")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val customerName  = etCustomer.text.toString().trim()
                val customerPhoneNumber = etPhoneNumber.text.toString()
                val productPosition = spinnerProduct.selectedItemPosition
                if (productPosition !in products.indices) {
                    Toast.makeText(requireContext(), "Please select a valid product", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedProduct = products[productPosition]
                val qty           = etQty.text.toString().toIntOrNull() ?: 0
                val unitPrice     = etUnitPrice.text.toString().toDoubleOrNull() ?: selectedProduct.salePrice
                val paymentPosition = spinnerPayment.selectedItemPosition
                val paymentMethod = if (paymentPosition in paymentMethods.indices) {
                    paymentMethods[paymentPosition]
                } else {
                    "Cash"
                }

                if (customerName.isEmpty() || qty <= 0) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (qty > selectedProduct.quantity) {
                    Toast.makeText(requireContext(), "Not enough stock! Available: ${selectedProduct.quantity}", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Find or create customer
                var customer = customerDao.searchByName(customerName).firstOrNull()
                if (customer == null) {
                    val newId = customerDao.insert(com.example.stockmaneger.data.models.Customer(name = customerName, phone = customerPhoneNumber))
                    customer = com.example.stockmaneger.data.models.Customer(newId.toInt(), customerName, "")
                }

                // Calculate
                val totalPrice = unitPrice * qty
                val totalBase  = selectedProduct.basePrice * qty
                val profit     = totalPrice - totalBase

                // Save sale
                saleDao.insert(Sale(
                    customerId    = customer.id,
                    productId     = selectedProduct.id,
                    quantity      = qty,
                    unitPrice     = unitPrice,
                    totalPrice    = totalPrice,
                    basePrice     = totalBase,
                    profit        = profit,
                    date          = selectedDate,
                    paymentMethod = paymentMethod
                ))

                // Update stock and account
                productDao.decreaseQuantity(selectedProduct.id, qty)
                accountDao.addMoney(totalPrice)
                accountDao.addProfit(profit)

                loadSales()
                Toast.makeText(requireContext(), "Sale saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(sale: Sale) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Sale")
            .setMessage("Delete this sale? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                saleDao.delete(sale.id)
                loadSales()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
