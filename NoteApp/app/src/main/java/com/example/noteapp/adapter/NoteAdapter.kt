package com.example.noteapp.adapter

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.R
import com.example.noteapp.databinding.NoteLayoutBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import com.squareup.picasso.Picasso
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onEditNoteClick: (Note) -> Unit,
    private val isHomePage: Boolean = false
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val itemBinding: NoteLayoutBinding) : RecyclerView.ViewHolder(itemBinding.root)

    private val differCallback = object : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
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

        val noteColorHex = currentNote.noteColor ?: "#FFFFFFFF"
        val parsedColor = Color.parseColor(noteColorHex)

        binding.noteCardView.setCardBackgroundColor(parsedColor)
        binding.noteTitle.setBackgroundColor(parsedColor)
        binding.noteDesc.setBackgroundColor(parsedColor)
        binding.checklistContainerPreview.setBackgroundColor(parsedColor)
        binding.imagesContainer.setBackgroundColor(parsedColor)
        binding.urlsContainer.setBackgroundColor(parsedColor)

        binding.noteTitle.text = currentNote.noteTitle

        binding.checklistContainerPreview.removeAllViews()
        binding.checklistContainerPreview.visibility = View.GONE
        binding.noteDesc.visibility = View.GONE

        binding.imagesContainer.removeAllViews()
        binding.imagesContainer.visibility = View.GONE

        binding.urlsContainer.removeAllViews()
        binding.urlsContainer.visibility = View.GONE

        val noteDesc = currentNote.noteDesc
        try {
            val jsonArray = JSONArray(noteDesc)
            binding.checklistContainerPreview.visibility = View.VISIBLE
            binding.noteDesc.visibility = View.GONE

            for (i in 0 until minOf(jsonArray.length(), 3)) {
                val obj = jsonArray.getJSONObject(i)
                val itemText = obj.getString("text")
                val itemChecked = obj.getBoolean("isChecked")

                val rowLayout = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundColor(parsedColor)
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
                    setBackgroundColor(parsedColor)
                }

                rowLayout.addView(checkBox)
                rowLayout.addView(textView)
                binding.checklistContainerPreview.addView(rowLayout)
            }

        } catch (e: Exception) {
            binding.noteDesc.visibility = View.VISIBLE
            binding.noteDesc.text = noteDesc
        }

        try {
            val imageUris = JSONArray(currentNote.imageUris)
            if (imageUris.length() > 0) {
                binding.imagesContainer.visibility = View.VISIBLE
                for (i in 0 until imageUris.length()) {
                    val uriString = imageUris.getString(i)
                    Log.d("NoteAdapter", "Loading image from URI: $uriString")
                    val imageSize = if (isHomePage) 50 else 500

                    val imageView = ImageView(holder.itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                            setMargins(8, 8, 8, 8)
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageResource(R.drawable.placeholder_image)
                        Picasso.get()
                            .load(Uri.parse(uriString))
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .resize(imageSize, imageSize)
                            .centerCrop()
                            .into(this)
                        tag = uriString
                    }

                    imageView.setOnLongClickListener {
                        AlertDialog.Builder(holder.itemView.context).apply {
                            setTitle("Delete Image")
                            setMessage("Are you sure you want to delete this image?")
                            setPositiveButton("Delete") { _, _ ->
                                binding.imagesContainer.removeView(imageView)
                                val activity = holder.itemView.context as? AppCompatActivity
                                if (activity != null) {
                                    val viewModel = ViewModelProvider(activity).get(NoteViewModel::class.java)
                                    val updatedUris = JSONArray(currentNote.imageUris)
                                    for (j in 0 until updatedUris.length()) {
                                        if (updatedUris.getString(j) == uriString) {
                                            updatedUris.remove(j)
                                            break
                                        }
                                    }
                                    val updatedNote = currentNote.copy(imageUris = updatedUris.toString())
                                    viewModel.updateNote(updatedNote)
                                } else {
                                    Toast.makeText(holder.itemView.context, "Error accessing ViewModel", Toast.LENGTH_SHORT).show()
                                }
                                val file = File(Uri.parse(uriString).path!!)
                                if (file.exists()) {
                                    file.delete()
                                }
                                Toast.makeText(holder.itemView.context, "Image deleted", Toast.LENGTH_SHORT).show()
                            }
                            setNegativeButton("Cancel", null)
                            create()
                            show()
                        }
                        true
                    }

                    binding.imagesContainer.addView(imageView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.imagesContainer.visibility = View.GONE
        }

        try {
            val urls = JSONArray(currentNote.urls)
            if (urls.length() > 0) {
                binding.urlsContainer.visibility = View.VISIBLE
                for (i in 0 until urls.length()) {
                    val url = urls.getString(i)
                    val urlTextView = TextView(holder.itemView.context).apply {
                        text = url
                        setTextColor(Color.BLUE)
                        textSize = 18f // Text size mai mare
                        setPadding(8, 8, 8, 8)
                        setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            holder.itemView.context.startActivity(intent)
                        }
                    }

                    urlTextView.setOnLongClickListener {
                        AlertDialog.Builder(holder.itemView.context).apply {
                            setTitle("Delete URL")
                            setMessage("Are you sure you want to delete this URL?")
                            setPositiveButton("Delete") { _, _ ->
                                binding.urlsContainer.removeView(urlTextView)
                                val activity = holder.itemView.context as? AppCompatActivity
                                if (activity != null) {
                                    val viewModel = ViewModelProvider(activity).get(NoteViewModel::class.java)
                                    val updatedUrls = JSONArray(currentNote.urls)
                                    for (j in 0 until updatedUrls.length()) {
                                        if (updatedUrls.getString(j) == url) {
                                            updatedUrls.remove(j)
                                            break
                                        }
                                    }
                                    val updatedNote = currentNote.copy(urls = updatedUrls.toString())
                                    viewModel.updateNote(updatedNote)
                                } else {
                                    Toast.makeText(holder.itemView.context, "Error accessing ViewModel", Toast.LENGTH_SHORT).show()
                                }
                                Toast.makeText(holder.itemView.context, "URL deleted", Toast.LENGTH_SHORT).show()
                            }
                            setNegativeButton("Cancel", null)
                            create()
                            show()
                        }
                        true
                    }

                    binding.urlsContainer.addView(urlTextView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.urlsContainer.visibility = View.GONE
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(currentNote.dateCreated))
        binding.noteDateTime.text = dateString

        if (currentNote.isPinned) {
            binding.pinIcon.visibility = View.VISIBLE
            binding.pinIcon.setImageResource(R.drawable.baseline_push_pin_blue_24)
        } else {
            binding.pinIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onEditNoteClick(currentNote)
        }
    }
}
