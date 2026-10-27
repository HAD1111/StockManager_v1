// ui/customers/CustomerAdapter.kt
package com.example.stockmaneger.ui.customers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.databinding.ItemCustomerBinding

class CustomerAdapter(
    private var customers: MutableList<Customer>,
    private val onClick: (Customer) -> Unit,
    private val onLongClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = customers[position]
        with(holder.binding) {
            tvCustomerName.text  = customer.name
            tvCustomerPhone.text = customer.phone.ifEmpty { "No phone" }
            // First non-whitespace letter as avatar, safe fallback for blank names
            tvAvatar.text = getAvatarInitial(customer.name)

            root.setOnClickListener     { onClick(customer) }
            root.setOnLongClickListener { onLongClick(customer); true }
        }
    }

    override fun getItemCount() = customers.size

    // Call this to refresh the list after add/edit/delete
    fun updateList(newList: List<Customer>) {
        customers.clear()
        customers.addAll(newList)
        notifyDataSetChanged()
    }

    companion object {
        /**
         * Returns the first non-whitespace character in uppercase,
         * or "?" if the name is empty or contains only whitespace.
         */
        fun getAvatarInitial(name: String): String {
            return name.firstOrNull { !it.isWhitespace() }?.uppercaseChar()?.toString() ?: "?"
        }
    }
}
