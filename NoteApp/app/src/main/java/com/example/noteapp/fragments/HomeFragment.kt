package com.example.noteapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.adapter.NoteAdapter
import com.example.noteapp.databinding.FragmentHomeBinding
import com.example.noteapp.model.Note
import com.example.noteapp.viewmodel.NoteViewModel
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager


private const val PREFS_NAME = "note_prefs"
private const val KEY_SORT_INDEX = "sort_index"

class HomeFragment : Fragment(R.layout.fragment_home), SearchView.OnQueryTextListener, MenuProvider{

    private var homeBinding: FragmentHomeBinding? = null
    private val binding get() = homeBinding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private var selectedSortOptionIndex = 1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeBinding = FragmentHomeBinding.inflate(inflater,container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        notesViewModel = (activity as MainActivity).noteViewModel

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedSortOptionIndex = prefs.getInt(KEY_SORT_INDEX, 1) // 1 = Newest first

        setupHomeRecyclerView()

        applySorting(selectedSortOptionIndex)

        binding.addNoteFab.setOnClickListener {
            it.findNavController().navigate(R.id.action_global_addNoteFragment)
        }

        binding.checkBoxFab.setOnClickListener {
            it.findNavController().navigate(R.id.action_global_checkListFragment)
        }
    }



    private fun applySorting(index: Int) {
        when (index) {
            0 -> {
                notesViewModel.getNotesSortedByTitle().observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                    (binding.homeRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.invalidateSpanAssignments()
                }
            }
            1 -> {
                notesViewModel.getNotesSortedByDateDesc().observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                    (binding.homeRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.invalidateSpanAssignments()
                }
            }
            2 -> {
                notesViewModel.getNotesSortedByDateAsc().observe(viewLifecycleOwner) { notes ->
                    noteAdapter.differ.submitList(notes)
                    noteAdapter.notifyDataSetChanged()
                    updateUI(notes)
                    (binding.homeRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.invalidateSpanAssignments()
                }
            }
        }
    }


    private fun updateUI(note: List<Note>?){
        if(note != null){
            if(note.isNotEmpty()){
                binding.emptyNotesImage.visibility = View.GONE
                binding.homeRecyclerView.visibility = View.VISIBLE
            }
            else{
                binding.emptyNotesImage.visibility = View.VISIBLE
                binding.homeRecyclerView.visibility = View.GONE
            }
        }
    }


    private fun setupHomeRecyclerView() {
        noteAdapter = NoteAdapter()
        val layoutManager = GridLayoutManager(context, 2)
        binding.homeRecyclerView.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            adapter = noteAdapter
        }


    }


    private fun searchNote(query: String?) {
        val searchQuery = "%${query}%"
        notesViewModel.searchNote(searchQuery).observe(viewLifecycleOwner) { list ->
            noteAdapter.differ.submitList(list)
        }
    }



    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(newText != null){
            searchNote(newText)
        }
        return true;
    }

    override fun onDestroy() {
        super.onDestroy()
        homeBinding = null
    }
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.home_menu, menu)

        val menuSearch = menu.findItem(R.id.searchMenu).actionView as SearchView
        menuSearch.isSubmitButtonEnabled = false
        menuSearch.setOnQueryTextListener(this)

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.sortMenu -> {
                showSortDialog()
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



}
