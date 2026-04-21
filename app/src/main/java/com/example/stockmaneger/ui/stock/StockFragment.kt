package com.example.stockmaneger.ui.stock

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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

        AlertDialog.Builder(requireContext())
            .setTitle("Add Product")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name  = etName.text.toString().trim()
                val qty   = etQty.text.toString().toIntOrNull() ?: 0
                val base  = etBase.text.toString().toDoubleOrNull() ?: 0.0
                val sale  = etSale.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    // Business logic: deduct cost from total money
                    val totalCost = base * qty
                    val available = accountDao.getTotalMoney()

                    if (totalCost > available) {
                        showWarningAndProceed(name, qty, base, sale, totalCost, available)
                    } else {
                        productDao.insert(Product(name = name, quantity = qty, basePrice = base, salePrice = sale))
                        accountDao.deductMoney(totalCost)
                        loadProducts()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWarningAndProceed(
        name: String, qty: Int, base: Double, sale: Double,
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
                productDao.insert(Product(name = name, quantity = qty, basePrice = base, salePrice = sale))
                accountDao.deductMoney(totalCost)
                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(product: Product) {
        val layout  = buildFormLayout(product.name, product.quantity, product.basePrice, product.salePrice)
        val etName  = layout.getChildAt(0) as EditText
        val etQty   = layout.getChildAt(1) as EditText
        val etBase  = layout.getChildAt(2) as EditText
        val etSale  = layout.getChildAt(3) as EditText

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Product")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val name = etName.text.toString().trim()
                val qty  = etQty.text.toString().toIntOrNull() ?: 0
                val base = etBase.text.toString().toDoubleOrNull() ?: 0.0
                val sale = etSale.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    productDao.update(Product(product.id, name, qty, base, sale))
                    loadProducts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Delete ${product.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                productDao.delete(product.id)
                loadProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildFormLayout(
        name: String = "", qty: Int = 0,
        base: Double = 0.0, sale: Double = 0.0
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
        layout.addView(etName)
        layout.addView(etQty)
        layout.addView(etBase)
        layout.addView(etSale)
        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}