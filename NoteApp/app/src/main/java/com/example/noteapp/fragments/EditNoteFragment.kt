package com.example.noteapp.fragments

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentEditNoteBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.view.inputmethod.InputMethodManager


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
        // Inflate the layout for this fragment
        editNoteBinding = FragmentEditNoteBinding.inflate(inflater,container,false)
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = checked
        }
        val editText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 18f
            setText(initialText)
            setSingleLine(false)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            val newEdit = addCheckListRow("", false)
                            newEdit.requestFocus()
                            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(newEdit, InputMethodManager.SHOW_IMPLICIT)
                            true
                        }
                        else -> false
                    }
                } else false
            }
        }

        rowLayout.addView(checkBox)
        rowLayout.addView(editText)
        editChecklistContainer.addView(rowLayout)

        return editText
    }


    private fun updateNote() {
        val noteTitle = binding.editNoteTitle.text.toString().trim()
        if(noteTitle.isEmpty()) {
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

        val updatedNote = Note(currentNote.id, noteTitle, finalDesc)
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
                view?.findNavController()?.popBackStack(R.id.homeFragment, true)
            }
            setNegativeButton("Cancel", null)
        }.create().show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_edit_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId)
        {
            R.id.deleteMenu -> {
                deleteNote()
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editNoteBinding = null
    }


}