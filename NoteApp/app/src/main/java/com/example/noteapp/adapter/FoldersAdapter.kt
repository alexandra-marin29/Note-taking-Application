package com.example.noteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.databinding.FolderLayoutBinding
import com.example.noteapp.model.Folder

class FoldersAdapter(private val onFolderClick: (Folder) -> Unit) : RecyclerView.Adapter<FoldersAdapter.FolderViewHolder>() {

    class FolderViewHolder(val binding: FolderLayoutBinding): RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.id == newItem.id && oldItem.folderName == newItem.folderName
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }
    val differ = AsyncListDiffer(this, differCallback)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(
            FolderLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = differ.currentList[position]
        holder.binding.folderName.text = folder.folderName
        holder.itemView.setOnClickListener {
            onFolderClick(folder)
        }
    }

}
