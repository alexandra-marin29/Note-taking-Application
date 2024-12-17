package com.example.noteapp.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentEditNoteBinding
import com.example.noteapp.model.Note
import com.example.noteapp.util.createColorBorderDrawable
import com.example.noteapp.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class EditNoteFragment : Fragment(R.layout.fragment_edit_note), MenuProvider {

    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private var currentNote: Note? = null
    private var selectedColorHex: String = "#FFFFFFFF"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesViewModel = (activity as MainActivity).noteViewModel

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        arguments?.let {
            currentNote = it.getParcelable("note")
        }

        if (currentNote != null) {
            binding.editNoteTitle.setText(currentNote!!.noteTitle)
            selectedColorHex = currentNote!!.noteColor ?: "#FFFFFFFF"
            applyColorToNoteContent(selectedColorHex)

            try {
                val jsonArray = JSONArray(currentNote!!.noteDesc)
                binding.editNoteDesc.visibility = View.GONE
                binding.editChecklistContainer.visibility = View.VISIBLE
                loadChecklistFromJson(jsonArray)
            } catch (e: JSONException) {
                binding.editNoteDesc.visibility = View.VISIBLE
                binding.editChecklistContainer.visibility = View.GONE
                binding.editNoteDesc.setText(currentNote!!.noteDesc)
            }
        }

        binding.editNoteFab.setOnClickListener {
            saveUpdatedNote()
        }
    }


    private fun loadChecklistFromJson(jsonArray: JSONArray) {
        binding.editChecklistContainer.removeAllViews()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val itemText = obj.getString("text")
            val isChecked = obj.getBoolean("isChecked")
            addCheckListRow(itemText, isChecked)
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
            setSingleLine(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(initialText)
            background = null

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        val newEdit = addCheckListRow("", false)
                        newEdit.requestFocus()
                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(newEdit, InputMethodManager.SHOW_IMPLICIT)
                        true
                    } else false
                } else false
            }
        }

        rowLayout.addView(checkBox)
        rowLayout.addView(editText)
        binding.editChecklistContainer.addView(rowLayout)
        return editText
    }

    private fun saveUpdatedNote() {
        val noteTitle = binding.editNoteTitle.text.toString().trim()
        val noteDesc = if (binding.editChecklistContainer.visibility == View.VISIBLE) {
            val jsonArray = JSONArray()
            for (i in 0 until binding.editChecklistContainer.childCount) {
                val row = binding.editChecklistContainer.getChildAt(i) as LinearLayout
                val checkBox = row.getChildAt(0) as CheckBox
                val editText = row.getChildAt(1) as EditText

                val obj = JSONObject()
                obj.put("text", editText.text.toString().trim())
                obj.put("isChecked", checkBox.isChecked)
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } else {
            binding.editNoteDesc.text.toString().trim()
        }

        if (noteTitle.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter note title", Toast.LENGTH_SHORT).show()
            return
        }

        val finalColor = selectedColorHex
        val dateNow = System.currentTimeMillis()

        val updatedNote = currentNote?.copy(
            noteTitle = noteTitle,
            noteDesc = noteDesc,
            noteColor = finalColor,
            dateCreated = dateNow
        ) ?: Note(
            id = 0,
            noteTitle = noteTitle,
            noteDesc = noteDesc,
            noteColor = finalColor,
            dateCreated = dateNow,
            folderId = 1
        )

        if (currentNote == null) {
            notesViewModel.addNote(updatedNote)
            Toast.makeText(requireContext(), "Checklist Note Saved", Toast.LENGTH_SHORT).show()
        } else {
            notesViewModel.updateNote(updatedNote)
            Toast.makeText(requireContext(), "Note Updated", Toast.LENGTH_SHORT).show()
        }

        findNavController().popBackStack()
    }


    private fun checkListContainerIsVisible(): Boolean {
        return binding.editChecklistContainer.visibility == View.VISIBLE
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

    private fun deleteNote() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Note")
            setMessage("Do you want to delete this note?")
            setPositiveButton("Delete") { _, _ ->
                currentNote?.let { notesViewModel.deleteNote(it) }
                Toast.makeText(context, "Note Deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            setNegativeButton("Cancel", null)
        }.create().show()
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
                selectedColorHex = colorsHex[which]
                applyColorToNoteContent(selectedColorHex)
            }
            create()
            show()
        }
    }

    private fun applyColorToNoteContent(colorHex: String) {
        selectedColorHex = colorHex
        val drawable = createColorBorderDrawable(requireContext(), colorHex)

        binding.editNoteTitle.background = drawable
        binding.editNoteDesc.background = drawable
        binding.editChecklistContainer.background = drawable
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
