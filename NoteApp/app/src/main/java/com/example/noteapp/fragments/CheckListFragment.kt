package com.example.noteapp.fragments

import android.os.Bundle
import android.view.*
import android.view.KeyEvent
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentCheckListBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONObject

class CheckListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentCheckListBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel


    private val args: CheckListFragmentArgs by navArgs()
    private var currentNote: Note? = null

    private lateinit var checkListContainer: LinearLayout
    private lateinit var checklistTitleEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckListBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesViewModel = (activity as MainActivity).noteViewModel

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        checkListContainer = binding.checkListContainer
        checklistTitleEditText = binding.checklistTitle

        currentNote = args.note

        if (currentNote == null) {
            addCheckListRow()
        } else {
            val noteTitle = currentNote!!.noteTitle
            val noteDesc = currentNote!!.noteDesc

            checklistTitleEditText.setText(noteTitle)
            if (noteDesc.isNotEmpty()) {
                loadChecklistFromJson(noteDesc)
            } else {
                addCheckListRow()
            }
        }
    }

    private fun loadChecklistFromJson(jsonString: String) {
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemText = obj.getString("text")
                val isChecked = obj.getBoolean("isChecked")
                addCheckListRow(itemText, isChecked)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            addCheckListRow()
        }
    }


    private fun addCheckListRow(initialText: String = "", checked: Boolean = false) {
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
            hint = "Enter item..."
            setText(initialText)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    addCheckListRow()
                    return@setOnKeyListener true
                }
                false
            }
        }

        rowLayout.addView(checkBox)
        rowLayout.addView(editText)
        checkListContainer.addView(rowLayout)
    }

    private fun saveCheckList() {
        val title = checklistTitleEditText.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonArray = JSONArray()
        for (i in 0 until checkListContainer.childCount) {
            val row = checkListContainer.getChildAt(i) as LinearLayout
            val checkBox = row.getChildAt(0) as CheckBox
            val editText = row.getChildAt(1) as EditText

            val obj = JSONObject()
            obj.put("text", editText.text.toString().trim())
            obj.put("isChecked", checkBox.isChecked)
            jsonArray.put(obj)
        }

        val noteDesc = jsonArray.toString()
        if (currentNote == null) {
            val newNote = Note(id=0, noteTitle=title, noteDesc=noteDesc)
            notesViewModel.addNote(newNote)
            Toast.makeText(requireContext(), "Checklist Note Saved", Toast.LENGTH_SHORT).show()
        } else {
            val updatedNote = Note(
                id = currentNote!!.id,
                noteTitle = title,
                noteDesc = noteDesc
            )
            notesViewModel.updateNote(updatedNote)
            Toast.makeText(requireContext(), "Checklist Note Updated", Toast.LENGTH_SHORT).show()
        }
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_add_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.saveMenu -> {
                saveCheckList()
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
