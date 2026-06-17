package com.example.stockmaneger.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmaneger.data.models.Capital
import com.example.stockmaneger.databinding.ItemCapitalBinding
import com.example.stockmaneger.databinding.ItemExpenseBinding

class CapitalAdapter(
    private var capitals: MutableList<Capital>,
    private val onLongClick: (Capital) -> Unit
) : RecyclerView.Adapter<CapitalAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: ItemCapitalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCapitalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val capital = capitals[position]
        with(holder.binding) {
            tvCapitalIcon.text   = "\uD83D\uDCB0"
            tvCapitalType.text   = capital.type
            tvCapitalNote.text   = capital.note.ifEmpty { "No note" }
            tvCapitalDate.text   = capital.date
            tvCapitalAmount.text = "$${String.format("%.2f", capital.amount)}"

            root.setOnLongClickListener { onLongClick(capital); true }
        }
    }

    override fun getItemCount() = capitals.size

    fun updateList(newList: List<Capital>) {
        capitals.clear()
        capitals.addAll(newList)
        notifyDataSetChanged()
    }
}