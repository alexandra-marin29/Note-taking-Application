package com.example.noteapp.fragments

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.adapter.FoldersAdapter
import com.example.noteapp.databinding.FragmentFoldersBinding
import com.example.noteapp.viewmodel.NoteViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class FoldersFragment : Fragment(R.layout.fragment_folders), MenuProvider {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesViewModel = (activity as MainActivity).noteViewModel
        auth = FirebaseAuth.getInstance()

        requireActivity().title = "NoteApp"

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupRecyclerView()

        notesViewModel.getAllFolders().observe(viewLifecycleOwner) { folders ->
            foldersAdapter.differ.submitList(folders)
            if (folders.isEmpty()) {
                binding.emptyFoldersImage.visibility = View.VISIBLE
                binding.foldersRecyclerView.visibility = View.GONE
            } else {
                binding.emptyFoldersImage.visibility = View.GONE
                binding.foldersRecyclerView.visibility = View.VISIBLE
            }
        }

        binding.addFolderFab.setOnClickListener {
            showNewFolderDialog()
        }

        binding.addNoteFab.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", 1)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_addNoteFragment, bundle)
        }

        binding.checkBoxFab.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", 1)
                putParcelable("note", null)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_checkListFragment, bundle)
        }
    }

    private fun setupRecyclerView() {
        foldersAdapter = FoldersAdapter { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.id)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_homeFragment, bundle)
        }
        binding.foldersRecyclerView.adapter = foldersAdapter
        binding.foldersRecyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 1)
    }

    private fun showNewFolderDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Folder name"
            setPadding(12, 60, 12, 12)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Create New Folder")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    notesViewModel.addFolder(folderName)
                    Toast.makeText(requireContext(), "Folder created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.folder_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.logoutMenu -> {
                logout()
                true
            }
            else -> false
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        val googleSignInClient = GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Successfully logged out.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_foldersFragment_to_loginFragment)
            } else {
                Toast.makeText(requireContext(), "Logout failed.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
