package com.example.stockmaneger.ui.sales

import android.graphics.Color
import com.example.stockmaneger.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.ItemSaleBinding

class SaleAdapter(
    private var sales: MutableList<Sale>,
    private val onLongClick: (Sale) -> Unit
) : RecyclerView.Adapter<SaleAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSaleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sale = sales[position]
        with(holder.binding) {
            tvCustomerName.text  = sale.customerName
            tvCustomerPhone.text = sale.customerPhone
            tvProductName.text   = "${sale.productName}  ×${sale.quantity}"
            tvTotalPrice.text    = "$${String.format("%.2f", sale.totalPrice)}"
            if(sale.profit>0){
                tvProfit.text = "+$${String.format("%.2f", sale.profit)}"
            }
            else if (sale.profit<0){
                tvProfit.text = "-$${String.format("%.2f", sale.profit*(-1))}"
                tvProfit.setTextColor(Color.RED)
                tvProfit.setBackgroundResource(R.drawable.bg_badge_loss)
            }
            else{
                tvProfit.text = "$${String.format("%.2f", sale.profit)}"
                tvProfit.setTextColor(Color.DKGRAY)
                tvProfit.setBackgroundResource(R.drawable.bg_badge_neutral)
            }

            tvDate.text          = sale.date
            tvPaymentMethod.text = sale.paymentMethod

            root.setOnLongClickListener { onLongClick(sale); true }
        }
    }

    override fun getItemCount() = sales.size

    fun updateList(newList: List<Sale>) {
        sales.clear()
        sales.addAll(newList)
        notifyDataSetChanged()
    }
}