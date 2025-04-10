package com.example.findcourse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchResultAdapter(
    private val items: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val resultText: TextView = view.findViewById(R.id.resultText)

        init {
            view.setOnClickListener {
                val address = items[adapterPosition]
                onItemClick(address)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.resultText.text = items[position]
    }

    override fun getItemCount(): Int = items.size
}
