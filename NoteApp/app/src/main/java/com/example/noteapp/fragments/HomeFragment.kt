package com.example.noteapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.adapter.NoteAdapter
import com.example.noteapp.databinding.FragmentHomeBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.appcompat.app.AlertDialog

private const val PREFS_NAME = "note_prefs"
private const val KEY_SORT_INDEX = "sort_index"

class HomeFragment : Fragment(R.layout.fragment_home), SearchView.OnQueryTextListener, MenuProvider {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private var selectedSortOptionIndex = 1

    private var folderId: Int = 1 // Default folderId
    private var folderName: String = "Notes"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            folderId = it.getInt("folderId", 1)
        }

        notesViewModel = (activity as MainActivity).noteViewModel

        notesViewModel.getFolderByIdLiveData(folderId).observe(viewLifecycleOwner, Observer { folder ->
            if (folder != null) {
                folderName = folder.folderName
                requireActivity().title = folderName
            }
        })

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedSortOptionIndex = prefs.getInt(KEY_SORT_INDEX, 1) // 1 = Newest first

        setupHomeRecyclerView()

        applySorting(selectedSortOptionIndex)

        binding.addNoteFab.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", folderId)
            }
            findNavController().navigate(R.id.action_homeFragment_to_addNoteFragment, bundle)
        }

        binding.checkBoxFab.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", folderId)
                putParcelable("note", null)
            }
            findNavController().navigate(R.id.action_homeFragment_to_checkListFragment, bundle)
        }
    }

    private fun applySorting(index: Int) {
        when (index) {
            0 -> {
                notesViewModel.getNotesSortedByTitle(folderId).observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                }
            }
            1 -> {
                notesViewModel.getNotesSortedByDateDesc(folderId).observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                }
            }
            2 -> {
                notesViewModel.getNotesSortedByDateAsc(folderId).observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                }
            }
        }
    }

    private fun updateUI(note: List<Note>?) {
        if (note != null) {
            if (note.isNotEmpty()) {
                binding.emptyNotesImage.visibility = View.GONE
                binding.homeRecyclerView.visibility = View.VISIBLE
            } else {
                binding.emptyNotesImage.visibility = View.VISIBLE
                binding.homeRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun setupHomeRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            val bundle = Bundle().apply {
                putParcelable("note", note)
            }
            findNavController().navigate(R.id.action_homeFragment_to_editNoteFragment, bundle)
        }
        binding.homeRecyclerView.apply {
            this.adapter = noteAdapter
            layoutManager = GridLayoutManager(context, 2)
            setHasFixedSize(true)
        }
    }

    private fun searchNote(query: String?) {
        val searchQuery = "%${query}%"
        notesViewModel.searchNoteInFolder(searchQuery, folderId).observe(viewLifecycleOwner) { list ->
            noteAdapter.differ.submitList(list)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            searchNote(newText)
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.home_menu, menu)

        val menuSearch = menu.findItem(R.id.searchMenu).actionView as SearchView
        menuSearch.isSubmitButtonEnabled = false
        menuSearch.setOnQueryTextListener(this)

        val deleteMenuItem = menu.findItem(R.id.deleteFolderMenu)
        if (folderId == 1) {
            deleteMenuItem.isVisible = false
        } else {
            deleteMenuItem.isVisible = true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.sortMenu -> {
                showSortDialog()
                true
            }
            R.id.deleteFolderMenu -> {
                showDeleteFolderDialog()
                true
            }
            else -> false
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("Sort by Title", "Sort by Date (Newest First)", "Sort by Date (Oldest First)")

        AlertDialog.Builder(requireContext())
            .setTitle("Sort Notes")
            .setSingleChoiceItems(sortOptions, selectedSortOptionIndex) { _, which ->
                selectedSortOptionIndex = which
            }
            .setPositiveButton("OK") { dialog, _ ->
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_SORT_INDEX, selectedSortOptionIndex).apply()

                applySorting(selectedSortOptionIndex)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun showDeleteFolderDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete the folder \"$folderName\" and all its notes?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteCurrentFolder()
                dialog.dismiss()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun deleteCurrentFolder() {
        notesViewModel.getFolderByIdLiveData(folderId).observe(viewLifecycleOwner, Observer { folder ->
            if (folder != null && folder.folderName != "Notes") {
                notesViewModel.deleteFolder(folder)

                Toast.makeText(requireContext(), "Folder \"$folderName\" and all its notes have been deleted.", Toast.LENGTH_SHORT).show()

                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Cannot delete the default 'Notes' folder.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
