package com.example.birdup.ui.home
import android.R.attr.data
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.R


class RecyclerAdapter (private var titles: MutableList<String>, private var details: MutableList<String>, private var percent: MutableList<String>, private var images: MutableList<Int>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemTitle: TextView = itemView.findViewById(R.id.bird_title)
        val itemDetails: TextView = itemView.findViewById(R.id.bird_description)
        val itemPercent: TextView = itemView.findViewById(R.id.prediction_percentage)
        val itemImage: ImageView  = itemView.findViewById(R.id.iv_image)

        init {
            itemView.setOnClickListener {
                // ADD POPUP WINDOW ASKING TO SAVE SAMPLE OR DISCARD
                val dialog = CustomPopup()
//                val fragmentManager: FragmentManager =
//                dialog.show(fragmentManager, CustomPopup.TAG)
                val position: Int = adapterPosition
                Toast.makeText(itemView.context, "You clicked on item # ${position + 1}",
                    Toast.LENGTH_SHORT).show()
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

    fun clear() {
        val size: Int = titles.size
        if (size > 0) {
            for (i in 0 until size) {
                titles.removeAt(i)
                details.removeAt(i)
                percent.removeAt(i)
                images.removeAt(i)
            }
            notifyItemRangeRemoved(0, size)
        }
    }
    override fun getItemCount(): Int {
        return titles.size
    }
}