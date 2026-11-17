package com.matchtriple.pro

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.matchtriple.pro.databinding.ItemTileBinding

class TileAdapter(
    private val tiles: MutableList<Tile?>,
    private val cols: Int,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<TileAdapter.VH>() {

    class VH(val binding: ItemTileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = tiles.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tile = tiles[position]
        val tv: TextView = holder.binding.tileText
        if (tile == null) {
            tv.text = ""
            tv.background = null
            tv.setOnClickListener(null)
            tv.alpha = 0f
            return
        }
        tv.alpha = 1f
        if (tile.locked) {
            tv.text = "🔒"
            tv.background = holder.itemView.context.getDrawable(R.drawable.bg_tile_locked)
            tv.setTextColor(Color.parseColor("#708890"))
            tv.setOnClickListener(null)
        } else {
            tv.text = tile.emoji
            tv.background = holder.itemView.context.getDrawable(R.drawable.bg_tile)
            tv.setTextColor(Color.BLACK)
            tv.setOnClickListener { onClick(position) }
        }
    }
}