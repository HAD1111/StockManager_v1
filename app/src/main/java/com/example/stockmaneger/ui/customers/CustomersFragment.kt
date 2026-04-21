// ui/customers/CustomersFragment.kt
package com.example.stockmaneger.ui.customers

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockmaneger.R
import com.example.stockmaneger.data.dao.CustomerDao
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.databinding.FragmentCustomersBinding

class CustomersFragment : Fragment() {

    private var _binding: FragmentCustomersBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerDao: CustomerDao
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

        // Setup RecyclerView
        adapter = CustomerAdapter(
            customers   = mutableListOf(),
            onClick     = { customer -> showEditDialog(customer) },
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
                customerDao.delete(customer.id)
                loadCustomers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Builds a simple name + phone form programmatically
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