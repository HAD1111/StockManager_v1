package com.example.stockmaneger.ui.stock

import com.example.stockmaneger.ui.scanner.BarcodeScannerHelper
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.databinding.FragmentStockBinding

class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var productDao: ProductDao
    private lateinit var accountDao: AccountDao
    private lateinit var adapter: ProductAdapter
    private lateinit var barcodeScanner: BarcodeScannerHelper
    private var pendingBarcodeEditText: EditText? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productDao = ProductDao(requireContext())
        accountDao = AccountDao(requireContext())

        barcodeScanner = BarcodeScannerHelper(
            this,
            requireContext()
        ) { scannedBarcode ->

            pendingBarcodeEditText?.setText(
                scannedBarcode ?: ""
            )
        }

        adapter = ProductAdapter(
            products    = mutableListOf(),
            onClick     = { product -> showEditDialog(product) },
            onLongClick = { product -> showDeleteDialog(product) }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter       = adapter

        loadProducts()

        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            val results = if (query.isEmpty())
                productDao.getAll()
            else
                productDao.searchByName(query)
            adapter.updateList(results)
            toggleEmpty(results.isEmpty())
        }

        binding.fabAddProduct.setOnClickListener {
            showAddDialog()
        }
    }

    private fun loadProducts() {
        val list = productDao.getAll()
        adapter.updateList(list)
        toggleEmpty(list.isEmpty())
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.rvProducts.visibility = if (isEmpty) View.GONE  else View.VISIBLE
        binding.tvEmpty.visibility    = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val layout  = buildFormLayout()
        val etName  = layout.getChildAt(0) as EditText
        val etQty   = layout.getChildAt(1) as EditText
        val etBase  = layout.getChildAt(2) as EditText
        val etSale  = layout.getChildAt(3) as EditText
        val etBarcode = layout.getChildAt(4) as EditText

        AlertDialog.Builder(requireContext())
            .setTitle("Add Product")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name  = etName.text.toString().trim()
                val qty   = etQty.text.toString().toIntOrNull() ?: 0
                val base  = etBase.text.toString().toDoubleOrNull() ?: 0.0
                val sale  = etSale.text.toString().toDoubleOrNull() ?: 0.0
                val barcode = etBarcode.text.toString().trim()

                if (name.isNotEmpty()) {
                    // Business logic: deduct cost from total money
                    if (barcode.isNotBlank()) {
                        val existing = productDao.getByBarcode(barcode)

                        if (existing != null) {
                            Toast.makeText(
                                context,
                                "Barcode already exists",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setPositiveButton
                        }
                    }
                    val totalCost = base * qty
                    val available = accountDao.getTotalMoney()

                    if (totalCost > available) {
                        showWarningAndProceed(name, qty, base, sale,barcode,totalCost, available)
                    } else {
                        productDao.insert(Product(name = name, quantity = qty, basePrice = base, salePrice = sale, barcode = barcode))
                        accountDao.deductMoney(totalCost)
                        loadProducts()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWarningAndProceed(
        name: String, qty: Int, base: Double, sale: Double, barcode: String,
        totalCost: Double, available: Double
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Insufficient Balance")
            .setMessage(
                "Total cost: $$totalCost\n" +
                        "Available: $$available\n\n" +
                        "This will result in a negative balance. Proceed anyway?"
            )
            .setPositiveButton("Proceed") { _, _ ->
                productDao.insert(Product(name = name, quantity = qty, basePrice = base, salePrice = sale, barcode = barcode))
                accountDao.deductMoney(totalCost)
                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditWarningAndProceed(
        product: Product,
        name: String,
        qty: Int,
        base: Double,
        sale: Double,
        barcode: String,
        needed: Double,
        available: Double
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Insufficient Balance")
            .setMessage(
                "Additional amount needed: $$needed\n" +
                        "Available: $$available\n\n" +
                        "This will result in a negative balance. Proceed anyway?"
            )
            .setPositiveButton("Proceed") { _, _ ->

                val oldCost = product.quantity * product.basePrice
                val newCost = qty * base

                val difference = newCost - oldCost

                accountDao.addMoney(-difference)

                productDao.update(
                    Product(
                        product.id,
                        name,
                        qty,
                        base,
                        sale,
                        barcode
                    )
                )

                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showEditDialog(product: Product) {
        val layout = buildFormLayout(
            product.name,
            product.quantity,
            product.basePrice,
            product.salePrice,
            product.barcode
        )

        val etName = layout.getChildAt(0) as EditText
        val etQty  = layout.getChildAt(1) as EditText
        val etBase = layout.getChildAt(2) as EditText
        val etSale = layout.getChildAt(3) as EditText
        val etBarcode = layout.getChildAt(4) as EditText

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Product")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->

                val name = etName.text.toString().trim()
                val qty  = etQty.text.toString().toIntOrNull() ?: 0
                val base = etBase.text.toString().toDoubleOrNull() ?: 0.0
                val sale = etSale.text.toString().toDoubleOrNull() ?: 0.0
                val barcode = etBarcode.text.toString().trim()

                if (name.isEmpty()) return@setPositiveButton
                val existing = productDao.getByBarcode(barcode)

                if (existing != null && existing.id != product.id && existing.barcode!="") {
                    Toast.makeText(
                        requireContext(),
                        "Barcode already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                val oldCost = product.quantity * product.basePrice
                val newCost = qty * base

                val difference = newCost - oldCost

                if (difference > 0) {
                    val available = accountDao.getTotalMoney()

                    if (available < difference) {
                        showEditWarningAndProceed(
                            product,
                            name,
                            qty,
                            base,
                            sale,
                            barcode,
                            difference,
                            available
                        )
                        return@setPositiveButton
                    }
                }

                accountDao.addMoney(-difference)

                productDao.update(
                    Product(
                        product.id,
                        name,
                        qty,
                        base,
                        sale,
                        barcode
                    )
                )

                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Delete ${product.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val result = productDao.delete(product.id)
                if(result==0)
                    Toast.makeText(
                        this.context,
                        "Product has sales related to it!. Set Quantity to 0 instead",
                        Toast.LENGTH_SHORT
                    ).show()
                else{
                    Toast.makeText(
                        this.context,
                        "Product deleted!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildFormLayout(
        name: String = "", qty: Int = 0,
        base: Double = 0.0, sale: Double = 0.0,barcode: String = ""
    ): LinearLayout {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }
        val etName = EditText(requireContext()).apply {
            hint      = "Product name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(name)
        }
        val etQty = EditText(requireContext()).apply {
            hint      = "Quantity"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (qty == 0) "" else qty.toString())
        }
        val etBase = EditText(requireContext()).apply {
            hint      = "Base price (what you paid)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (base == 0.0) "" else base.toString())
        }
        val etSale = EditText(requireContext()).apply {
            hint      = "Sale price (what you charge)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (sale == 0.0) "" else sale.toString())
        }
        val etBarcode = EditText(requireContext()).apply {
            hint = "Barcode (optional)"
            setText(barcode)
            isFocusable = false
            isClickable = false
            isCursorVisible = false
        }

        pendingBarcodeEditText = etBarcode

        val btnScanBarcode = Button(requireContext()).apply {
            text = "Scan Barcode"

            setOnClickListener {
                barcodeScanner.scan()
            }
        }


        layout.addView(etName)
        layout.addView(etQty)
        layout.addView(etBase)
        layout.addView(etSale)
        layout.addView(etBarcode)
        layout.addView(btnScanBarcode)
        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}