package com.example.findcourse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter(
    private val items: MutableList<AddressEntity>,
    private val onDeleteClick: (Int, () -> Unit) -> Unit // ✅ 콜백 추가
) : RecyclerView.Adapter<AddressAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeNameText: TextView = view.findViewById(R.id.placeNameText)
        val addressText: TextView = view.findViewById(R.id.addressText)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.placeNameText.text = item.placeName
        holder.addressText.text = item.address
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position){
                // 삭제 후 UI 갱신
                notifyItemRemoved(position)
            }
        }
    }


    override fun getItemCount() = items.size
}