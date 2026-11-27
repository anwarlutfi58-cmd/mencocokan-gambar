package com.matchtriple.pro

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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
        val stack: TextView = holder.binding.stackBadge

        if (tile == null) {
            tv.text = ""
            tv.background = null
            tv.setOnClickListener(null)
            tv.alpha = 0f
            stack.visibility = View.GONE
            holder.itemView.translationX = 0f
            holder.itemView.translationY = 0f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            holder.itemView.elevation = 0f
            return
        }

        tv.alpha = if (tile.locked) 0.42f else 1f
        holder.itemView.translationX = tile.layer * 1.5f
        holder.itemView.translationY = -tile.layer * 1.5f
        holder.itemView.scaleX = if (tile.locked) 0.90f else 1f
        holder.itemView.scaleY = if (tile.locked) 0.90f else 1f
        holder.itemView.elevation = tile.layer * 1.5f

        tv.text = tile.emoji

        if (tile.locked) {
            tv.background = holder.itemView.context.getDrawable(R.drawable.bg_tile_locked)
            tv.setTextColor(Color.parseColor("#708890"))
            tv.setOnClickListener(null)
            stack.visibility = View.VISIBLE
            stack.text = "🔒"
        } else {
            tv.background = holder.itemView.context.getDrawable(R.drawable.bg_tile)
            tv.setTextColor(Color.BLACK)
            tv.setOnClickListener { onClick(position) }

            if (tile.layer > 0) {
                stack.visibility = View.VISIBLE
                stack.text = "+${tile.layer}"
            } else {
                stack.visibility = View.GONE
            }
        }
    }
}