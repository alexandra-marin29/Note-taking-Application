package com.example.noteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.databinding.NoteLayoutBinding
import com.example.noteapp.fragments.HomeFragmentDirections
import com.example.noteapp.model.Note
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray


class NoteAdapter : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val itemBinding: NoteLayoutBinding): RecyclerView.ViewHolder(itemBinding.root)

    private val differCallback = object : DiffUtil.ItemCallback<Note>()
    {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.noteDesc == newItem.noteDesc &&
                    oldItem.noteTitle == newItem.noteTitle
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }

    }
    val differ = AsyncListDiffer(this, differCallback)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            NoteLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = differ.currentList[position]
        val binding = holder.itemBinding

        binding.noteTitle.text = currentNote.noteTitle

        binding.checklistContainerPreview.removeAllViews()
        binding.checklistContainerPreview.visibility = View.GONE
        binding.noteDesc.visibility = View.VISIBLE

        val noteDesc = currentNote.noteDesc

        try {
            val jsonArray = JSONArray(noteDesc)
            binding.checklistContainerPreview.visibility = View.VISIBLE
            binding.noteDesc.visibility = View.GONE

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemText = obj.getString("text")
                val itemChecked = obj.getBoolean("isChecked")

                val rowLayout = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val checkBox = CheckBox(holder.itemView.context).apply {
                    isClickable = false
                    isChecked = itemChecked
                }
                val textView = TextView(holder.itemView.context).apply {
                    text = itemText
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                rowLayout.addView(checkBox)
                rowLayout.addView(textView)
                binding.checklistContainerPreview.addView(rowLayout)
            }

        } catch (e: Exception) {
            binding.noteDesc.text = noteDesc
        }

        holder.itemView.setOnClickListener {
            val direction = HomeFragmentDirections.actionHomeFragmentToEditNoteFragment(currentNote)
            it.findNavController().navigate(direction)
        }
    }

}
