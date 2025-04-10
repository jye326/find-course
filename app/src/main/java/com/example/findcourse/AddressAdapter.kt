package com.example.findcourse

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdressAdapter(private val items: List<Address>):
    RecyclerView.Adapter<AdressAdapter.ViewHolder>() {

    inner class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context)
        textView.setPadding(16, 16, 16, 16)
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.text = items[position].text
    }

    override fun getItemCount() = items.size
}