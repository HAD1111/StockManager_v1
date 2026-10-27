// ui/customers/CustomerAdapter.kt
package com.example.stockmaneger.ui.customers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.R
import com.example.stockmaneger.data.models.Customer
import com.example.stockmaneger.databinding.ItemCustomerBinding
import java.util.Locale

class CustomerAdapter(
    private var customers: MutableList<Customer>,
    private val getDebt: ((Int) -> Double)? = null,
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
            tvCustomerName.text = customer.name
            tvCustomerPhone.text = customer.phone.ifEmpty { "No phone" }
            tvAvatar.text = getAvatarInitial(customer.name)

            if (getDebt != null) {
                val debt = getDebt.invoke(customer.id)
                tvCustomerDebt.visibility = View.VISIBLE
                if (debt > 0.005) {
                    tvCustomerDebt.text = "Due: $" + String.format(Locale.US, "%.2f", debt)
                    tvCustomerDebt.setTextColor(root.context.getColor(R.color.danger))
                } else {
                    tvCustomerDebt.text = "No Debt"
                    tvCustomerDebt.setTextColor(root.context.getColor(R.color.success))
                }
            } else {
                tvCustomerDebt.visibility = View.GONE
            }

            root.setOnClickListener { onClick(customer) }
            root.setOnLongClickListener { onLongClick(customer); true }
        }
    }

    override fun getItemCount() = customers.size

    fun updateList(newList: List<Customer>) {
        customers.clear()
        customers.addAll(newList)
        notifyDataSetChanged()
    }

    companion object {
        fun getAvatarInitial(name: String): String {
            return name.firstOrNull { !it.isWhitespace() }?.uppercaseChar()?.toString() ?: "?"
        }
    }
}
