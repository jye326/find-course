package com.example.findcourse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter(
    private val items: MutableList<Address>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<AddressAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addressText: TextView = view.findViewById(R.id.addressText)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.addressText.text = items[position].text
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount() = items.size
}