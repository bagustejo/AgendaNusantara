package com.sdm.agendanusantara.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sdm.agendanusantara.R
import com.sdm.agendanusantara.Task
import com.sdm.agendanusantara.db.DatabaseHelper

/**
 * RecyclerView Adapter untuk Daftar Tugas.
 *
 * Fitur:
 * - Menampilkan judul, tanggal jatuh tempo, badge kategori
 * - Checkbox custom (checked = teal, unchecked = border only)
 * - Anak panah merah (penting) / hijau (biasa)
 * - Teks di-strikethrough bila tugas selesai
 * - Klik item → toggle selesai/belum, update SQLite
 */
class TaskAdapter(
    private val tasks  : MutableList<Task>,
    private val db     : DatabaseHelper,
    private val userId : Int,
    private val onToggle: (Task) -> Unit   // callback untuk refresh stats Beranda (jika perlu)
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCheck    : ImageView = itemView.findViewById(R.id.ivCheck)
        val tvTitle    : TextView  = itemView.findViewById(R.id.tvTitle)
        val tvDate     : TextView  = itemView.findViewById(R.id.tvDate)
        val tvBadge    : TextView  = itemView.findViewById(R.id.tvBadge)
        val ivArrow    : ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task    = tasks[position]
        val ctx     = holder.itemView.context
        val isPenting = task.category == DatabaseHelper.CATEGORY_PENTING

        // ── Judul ─────────────────────────────────────────────────────
        holder.tvTitle.text = task.title
        if (task.isDone) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.6f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1f
        }

        // ── Tanggal (format tampilan dd MMM yyyy) ─────────────────────
        holder.tvDate.text = formatDate(task.dueDate)

        // ── Badge PENTING / BIASA ─────────────────────────────────────
        if (isPenting) {
            holder.tvBadge.text = "Penting"
            holder.tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_penting_text))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_penting)
        } else {
            holder.tvBadge.text = "Biasa"
            holder.tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_biasa_text))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_biasa_pill)
        }

        // ── Checkbox icon ─────────────────────────────────────────────
        if (task.isDone) {
            holder.ivCheck.setImageResource(R.drawable.ic_check_done)
            holder.ivCheck.setBackgroundResource(R.drawable.bg_check_done)
        } else {
            holder.ivCheck.setImageResource(0)          // kosong
            holder.ivCheck.setBackgroundResource(R.drawable.bg_check_empty)
        }

        // ── Anak panah: merah (penting) / hijau (biasa) ───────────────
        val arrowColor = if (isPenting)
            ContextCompat.getColor(ctx, R.color.color_penting)
        else
            ContextCompat.getColor(ctx, R.color.color_biasa_green)
        holder.ivArrow.setColorFilter(arrowColor)
        if (task.isDone) holder.ivArrow.alpha = 0.4f else holder.ivArrow.alpha = 1f

        // ── Toggle selesai saat item diklik ───────────────────────────
        holder.itemView.setOnClickListener {
            val newDone = !task.isDone
            db.toggleTaskDone(task.id, newDone)
            task.isDone = newDone
            notifyItemChanged(position)
            onToggle(task)
        }
    }

    override fun getItemCount() = tasks.size

    /** Ganti seluruh data (dipakai saat refresh) */
    fun updateData(newList: List<Task>) {
        tasks.clear()
        tasks.addAll(newList)
        notifyDataSetChanged()
    }

    /** Format "yyyy-MM-dd" → "05 Mei 2026" */
    private fun formatDate(raw: String): String {
        return try {
            val sdf  = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val show = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
            show.format(sdf.parse(raw)!!).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { raw }
    }
}
