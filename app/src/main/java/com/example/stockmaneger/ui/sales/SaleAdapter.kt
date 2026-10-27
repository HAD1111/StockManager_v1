package com.example.stockmaneger.ui.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.R
import com.example.stockmaneger.data.models.Sale
import com.example.stockmaneger.databinding.ItemSaleBinding
import java.util.Locale

class SaleAdapter(
    private var sales: MutableList<Sale>,
    private val onClick: (Sale) -> Unit = {},
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
            tvCustomerName.text = sale.customerName
            tvCustomerPhone.text = sale.customerPhone
            tvCustomerPhone.visibility = if (sale.customerPhone.isNotEmpty()) View.VISIBLE else View.GONE
            tvOrderId.text = "Order #${sale.id}"
            tvDate.text = sale.date
            tvProductName.text = "${sale.itemCount} item${if (sale.itemCount == 1) "" else "s"}"

            // 1. Sale Total
            tvTotalPrice.text = "$${String.format(Locale.US, "%.2f", sale.totalAmount)}"

            // 2. Total Paid
            tvAmountPaid.text = "$${String.format(Locale.US, "%.2f", sale.paidAmount)}"

            // 3. Remaining Balance (Due)
            tvRemainingDebt.text = "$${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
            if (sale.remainingDebt <= 0.005) {
                tvRemainingDebt.setTextColor(root.context.getColor(R.color.success))
            } else {
                tvRemainingDebt.setTextColor(root.context.getColor(R.color.danger))
            }

            // 4. Gross Profit
            if (sale.profit > 0.005) {
                tvProfit.text = "+$${String.format(Locale.US, "%.2f", sale.profit)}"
                tvProfit.setTextColor(root.context.getColor(R.color.success))
            } else if (sale.profit < -0.005) {
                tvProfit.text = "-$${String.format(Locale.US, "%.2f", -sale.profit)}"
                tvProfit.setTextColor(root.context.getColor(R.color.danger))
            } else {
                tvProfit.text = "$${String.format(Locale.US, "%.2f", sale.profit)}"
                tvProfit.setTextColor(root.context.getColor(R.color.text_secondary))
            }

            // 5. Payment method badge
            tvPaymentMethod.text = if (sale.paidAmount > 0.005) sale.paymentMethod else "Unpaid"

            // 6. Status badge
            when {
                sale.paidAmount > sale.totalAmount + 0.005 -> {
                    tvDebtStatus.text = "Overpaid ($${String.format(Locale.US, "%.2f", sale.paidAmount)})"
                    tvDebtStatus.setTextColor(root.context.getColor(R.color.success))
                    tvDebtStatus.setBackgroundResource(R.drawable.bg_badge_success)
                }
                sale.remainingDebt <= 0.005 -> {
                    tvDebtStatus.text = "Paid in Full"
                    tvDebtStatus.setTextColor(root.context.getColor(R.color.success))
                    tvDebtStatus.setBackgroundResource(R.drawable.bg_badge_success)
                }
                sale.paidAmount > 0.005 -> {
                    tvDebtStatus.text = "Partial • Due $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                    tvDebtStatus.setTextColor(root.context.getColor(R.color.danger))
                    tvDebtStatus.setBackgroundResource(R.drawable.bg_badge_loss)
                }
                else -> {
                    tvDebtStatus.text = "Unpaid • Due $${String.format(Locale.US, "%.2f", sale.remainingDebt)}"
                    tvDebtStatus.setTextColor(root.context.getColor(R.color.danger))
                    tvDebtStatus.setBackgroundResource(R.drawable.bg_badge_loss)
                }
            }

            root.setOnClickListener { onClick(sale) }
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
