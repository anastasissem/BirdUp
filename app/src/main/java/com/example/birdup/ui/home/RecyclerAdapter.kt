package com.example.birdup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter (private var titles: List<String>, private var details: List<String>, private var percent: List<String>, private var images: List<Int>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemTitle: TextView = itemView.findViewById(R.id.bird_title)
        val itemDetails: TextView = itemView.findViewById(R.id.bird_description)
        val itemPercent: TextView = itemView.findViewById(R.id.prediction_percentage)
        val itemImage: ImageView  = itemView.findViewById(R.id.iv_image)

        init {
            itemView.setOnClickListener {
                // ADD POPUP WINDOW ASKING TO SAVE SAMPLE OR DISCARD
                val position: Int = adapterPosition
                Toast.makeText(itemView.context, "You clicked on item # ${position + 1}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTitle.text = titles[position]
        holder.itemDetails.text = details[position]
        holder.itemPercent.text = percent[position]
        holder.itemImage.setImageResource(images[position])
    }

    override fun getItemCount(): Int {
        return titles.size
    }
}