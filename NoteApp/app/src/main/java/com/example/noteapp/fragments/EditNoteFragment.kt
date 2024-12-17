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
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentEditNoteBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import org.json.JSONArray
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
            binding.editNoteDesc.setText(currentNote!!.noteDesc)
            selectedColorHex = currentNote!!.noteColor ?: "#FFFFFFFF"
            applyColorToNoteContent(selectedColorHex)
        }

        binding.editNoteFab.setOnClickListener {
            saveUpdatedNote()
        }
    }

    private fun saveUpdatedNote() {
        val noteTitle = binding.editNoteTitle.text.toString().trim()
        val noteDesc = binding.editNoteDesc.text.toString().trim()

        if (noteTitle.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter note title", Toast.LENGTH_SHORT).show()
            return
        }

        val finalDesc = if (checkListContainerIsVisible()) {
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
            noteDesc
        }

        val updatedNote = currentNote?.copy(
            noteTitle = noteTitle,
            noteDesc = finalDesc,
            noteColor = selectedColorHex,
            dateCreated = System.currentTimeMillis()
        ) ?: Note(
            id = 0,
            noteTitle = noteTitle,
            noteDesc = finalDesc,
            noteColor = selectedColorHex,
            dateCreated = System.currentTimeMillis(),
            folderId = 1
        )

        notesViewModel.updateNote(updatedNote)
        Toast.makeText(requireContext(), "Note Updated", Toast.LENGTH_SHORT).show()
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
        val chosenColor = Color.parseColor(colorHex)

        binding.editNoteTitle.setBackgroundColor(chosenColor)
        binding.editNoteDesc.setBackgroundColor(chosenColor)
        binding.editChecklistContainer.setBackgroundColor(chosenColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
