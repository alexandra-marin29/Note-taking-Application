package com.example.noteapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentEditNoteBinding
import com.example.noteapp.model.Note
import com.example.noteapp.util.createColorBorderDrawable
import com.example.noteapp.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONObject

class EditNoteFragment : Fragment(R.layout.fragment_edit_note), MenuProvider {

    private var editNoteBinding: FragmentEditNoteBinding? = null
    private val binding get() = editNoteBinding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var currentNote: Note
    private val args: EditNoteFragmentArgs by navArgs()

    private lateinit var editChecklistContainer: LinearLayout
    private lateinit var editNoteDescEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        editNoteBinding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        notesViewModel = (activity as MainActivity).noteViewModel

        currentNote = args.note!!
        binding.editNoteTitle.setText(currentNote.noteTitle)
        editChecklistContainer = binding.editChecklistContainer
        editNoteDescEditText = binding.editNoteDesc

        if (tryLoadChecklist(currentNote.noteDesc)) {
            editNoteDescEditText.visibility = View.GONE
            editChecklistContainer.visibility = View.VISIBLE
        } else {
            editChecklistContainer.visibility = View.GONE
            editNoteDescEditText.visibility = View.VISIBLE
            editNoteDescEditText.setText(currentNote.noteDesc)
        }

        val storedColor = currentNote.noteColor ?: "#FFFFFFFF"
        applyColorToNoteContent(storedColor)

        binding.editNoteFab.setOnClickListener {
            updateNote()
        }
    }

    private fun tryLoadChecklist(jsonString: String): Boolean {
        return try {
            val jsonArray = JSONArray(jsonString)
            editChecklistContainer.removeAllViews()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemText = obj.getString("text")
                val isChecked = obj.getBoolean("isChecked")
                addCheckListRow(itemText, isChecked)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun addCheckListRow(initialText: String = "", checked: Boolean = false): EditText {
        val rowLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val checkBox = CheckBox(requireContext()).apply {
            isChecked = checked
            setBackgroundColor(Color.TRANSPARENT)
        }
        val editText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            textSize = 18f
            setText(initialText)
            background = null
        }

        rowLayout.addView(checkBox)
        rowLayout.addView(editText)
        editChecklistContainer.addView(rowLayout)
        return editText
    }

    private fun updateNote() {
        val noteTitle = binding.editNoteTitle.text.toString().trim()
        if (noteTitle.isEmpty()) {
            Toast.makeText(context, "Please enter note title", Toast.LENGTH_SHORT).show()
            return
        }

        val finalDesc = if (editChecklistContainer.visibility == View.VISIBLE) {
            val jsonArray = JSONArray()
            for (i in 0 until editChecklistContainer.childCount) {
                val row = editChecklistContainer.getChildAt(i) as LinearLayout
                val checkBox = row.getChildAt(0) as CheckBox
                val editText = row.getChildAt(1) as EditText

                val obj = JSONObject()
                obj.put("text", editText.text.toString().trim())
                obj.put("isChecked", checkBox.isChecked)
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } else {
            editNoteDescEditText.text.toString().trim()
        }

        val updatedNote = currentNote.copy(
            noteTitle = noteTitle,
            noteDesc = finalDesc,
            noteColor = currentNote.noteColor
        )
        notesViewModel.updateNote(updatedNote)
        view?.findNavController()?.popBackStack(R.id.homeFragment, false)
    }

    private fun deleteNote() {
        AlertDialog.Builder(requireActivity()).apply {
            setTitle("Delete Note")
            setMessage("Do you want to delete this note?")
            setPositiveButton("Delete") { _, _ ->
                notesViewModel.deleteNote(currentNote)
                Toast.makeText(context, "Note Deleted", Toast.LENGTH_SHORT).show()
                view?.findNavController()?.popBackStack(R.id.homeFragment, false)
            }
            setNegativeButton("Cancel", null)
        }.create().show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_edit_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.deleteMenu -> {
                deleteNote()
                true
            }
            R.id.settingsMenu -> {
                showColorPickerDialog()
                true
            }
            else -> false
        }
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf("White", "Pink", "Green", "Purple", "Orange", "Yellow", "Blue")
        val colorsHex = arrayOf(
            "#FFFFFFFF", // White
            "#FFF2CDC4", // Pink pastel
            "#FFE5F5DC", // Green pastel
            "#FFDBCDF0", // Purple pastel
            "#FFF7D9C4", // Orange pastel
            "#FFFAEDCB", // Yellow pastel
            "#FFC6DEF1"  // Blue pastel
        )

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Choose a color")
            setItems(colors) { _, which ->
                val newColor = colorsHex[which]
                currentNote = currentNote.copy(noteColor = newColor)
                applyColorToNoteContent(newColor)
            }
            create()
            show()
        }
    }

    private fun applyColorToNoteContent(colorHex: String) {
        val chosenColor = Color.parseColor(colorHex)

        binding.editNoteTitle.background = createColorBorderDrawable(requireContext(), colorHex)

        if (editNoteDescEditText.visibility == View.VISIBLE) {
            editNoteDescEditText.background = createColorBorderDrawable(requireContext(), colorHex)
        } else {
            for (i in 0 until editChecklistContainer.childCount) {
                val row = editChecklistContainer.getChildAt(i) as LinearLayout
                val checkBox = row.getChildAt(0) as CheckBox
                val et = row.getChildAt(1) as EditText

                et.setBackgroundColor(chosenColor)
                row.setBackgroundColor(chosenColor)
                checkBox.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        editNoteBinding = null
    }
}
