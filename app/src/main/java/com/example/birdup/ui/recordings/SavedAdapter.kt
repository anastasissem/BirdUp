package com.example.birdup.ui.recordings

import android.util.Log
import com.example.birdup.ui.home.ShowDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.R

class SavedAdapter(private var stored_index: Int, private var stored_name: String?, private var stored_latin: String?, private var stored_percent: String?, private var stored_date: String?) :
    RecyclerView.Adapter<SavedAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val storedIndex: TextView = itemView.findViewById(R.id.saved_index)
        val storedName: TextView = itemView.findViewById(R.id.saved_name)
        val storedLatin: TextView = itemView.findViewById(R.id.saved_latin)
        val storedPercent: TextView  = itemView.findViewById(R.id.saved_confidence)
        val storedDate: TextView  = itemView.findViewById(R.id.saved_datetime)

        init {
            val delete: Button = itemView.findViewById(R.id.saved_trashButton)
            delete.setOnClickListener {
                Log.d("REC", "ADD DELETE METHOD")
                // add dialog delete window
            }
            val play: Button = itemView.findViewById(R.id.saved_playButton)
            play.setOnClickListener {
                Log.d("REC", "ADD PLAY METHOD")
            }

            itemView.setOnClickListener {

                val position: Int = adapterPosition
                Toast.makeText(itemView.context, "You clicked on item # ${position + 1}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.saved_items_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.storedIndex.text = stored_index.toString()
        holder.storedName.text = stored_name
        holder.storedLatin.text = stored_latin
        holder.storedPercent.text = stored_percent
        holder.storedDate.text = stored_date
    }

    override fun getItemCount(): Int {
        //fix that
        return stored_index
    }
}