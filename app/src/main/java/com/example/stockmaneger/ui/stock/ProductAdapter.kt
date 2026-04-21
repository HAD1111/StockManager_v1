package com.example.stockmaneger.ui.stock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.Product
import com.example.stockmaneger.databinding.ItemProductBinding

class ProductAdapter(
    private var products: MutableList<Product>,
    private val onClick: (Product) -> Unit,
    private val onLongClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            tvProductName.text   = product.name
            tvProductPrices.text = "Base: $${product.basePrice}  |  Sale: $${product.salePrice}"
            tvProductQty.text    = product.quantity.toString()

            // Color quantity red if low stock
            tvProductQty.setTextColor(
                if (product.quantity <= 3)
                    holder.itemView.context.getColor(com.example.stockmaneger.R.color.danger)
                else
                    holder.itemView.context.getColor(com.example.stockmaneger.R.color.primary)
            )

            root.setOnClickListener     { onClick(product) }
            root.setOnLongClickListener { onLongClick(product); true }
        }
    }

    override fun getItemCount() = products.size

    fun updateList(newList: List<Product>) {
        products.clear()
        products.addAll(newList)
        notifyDataSetChanged()
    }
}