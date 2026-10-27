package com.example.stockmaneger.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.CartItem
import com.example.stockmaneger.databinding.ItemCartProductBinding
import java.util.Locale

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onCartUpdated: () -> Unit,
    private val onItemRemoved: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCartProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cartItems[position]
        with(holder.binding) {
            tvProductName.text = item.productName
            tvStockInfo.text = "Stock: ${item.product.quantity} • Cost: $${String.format(Locale.US, "%.2f", item.unitCost)}"
            tvQuantity.text = item.quantity.toString()
            tvSubtotal.text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"

            // Temporary detach text watcher flag to avoid infinite loops during recycling
            var isBinding = true
            etUnitPrice.setText(String.format(Locale.US, "%.2f", item.customUnitPrice))
            isBinding = false

            etUnitPrice.doAfterTextChanged { editable ->
                if (isBinding) return@doAfterTextChanged
                val text = editable?.toString()?.trim() ?: ""
                val newPrice = text.toDoubleOrNull()
                if (newPrice != null && newPrice >= 0.0) {
                    item.customUnitPrice = newPrice
                    tvSubtotal.text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"
                    onCartUpdated()
                }
            }

            btnPlus.setOnClickListener {
                if (item.quantity < item.product.quantity) {
                    item.quantity++
                    tvQuantity.text = item.quantity.toString()
                    tvSubtotal.text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"
                    onCartUpdated()
                } else {
                    Toast.makeText(
                        root.context,
                        "Max available stock reached (${item.product.quantity})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    tvQuantity.text = item.quantity.toString()
                    tvSubtotal.text = "$${String.format(Locale.US, "%.2f", item.subtotal)}"
                    onCartUpdated()
                } else {
                    // Remove if decrementing below 1
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION && currentPos < cartItems.size) {
                        val removed = cartItems.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        onItemRemoved(removed)
                        onCartUpdated()
                    }
                }
            }

            btnRemove.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION && currentPos < cartItems.size) {
                    val removed = cartItems.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                    onItemRemoved(removed)
                    onCartUpdated()
                }
            }
        }
    }

    override fun getItemCount() = cartItems.size
}
