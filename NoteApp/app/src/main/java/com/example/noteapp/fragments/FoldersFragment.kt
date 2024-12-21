package com.example.noteapp.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.adapter.FoldersAdapter
import com.example.noteapp.databinding.FragmentFoldersBinding
import com.example.noteapp.network.RetrofitInstance
import com.example.noteapp.viewmodel.NoteViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersFragment : Fragment(R.layout.fragment_folders), MenuProvider {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var auth: FirebaseAuth

    private var hasQuoteBeenFetched = false

    private var currentQuote: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)

        savedInstanceState?.let { bundle ->
            hasQuoteBeenFetched = bundle.getBoolean("QUOTE_FETCHED", false)
            currentQuote = bundle.getString("CURRENT_QUOTE")
        }

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

        binding.addFolderFab.setOnClickListener { showNewFolderDialog() }
        binding.addNoteFab.setOnClickListener {
            val bundle = Bundle().apply { putInt("folderId", 1) }
            findNavController().navigate(R.id.action_foldersFragment_to_addNoteFragment, bundle)
        }
        binding.checkBoxFab.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", 1)
                putParcelable("note", null)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_checkListFragment, bundle)
        }


        if (currentQuote.isNullOrBlank()) {
            binding.quoteTextView.text = "Loading motivational quote..."
        } else {
            binding.quoteTextView.text = currentQuote
        }


        if (!hasQuoteBeenFetched) {
            fetchAndDisplayQuote()
            hasQuoteBeenFetched = true
        }

        startPeriodicQuoteRefresh()
    }


    private fun setupRecyclerView() {
        foldersAdapter = FoldersAdapter { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.id)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_homeFragment, bundle)
        }
        binding.foldersRecyclerView.adapter = foldersAdapter
        binding.foldersRecyclerView.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(context, 1)
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


    private fun fetchAndDisplayQuote() {
        if (!isNetworkAvailable(requireContext())) {
            currentQuote = "No internet connection."
            binding.quoteTextView.text = currentQuote
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getRandomQuote()
                }

                if (response.isSuccessful && response.body() != null) {
                    val quotes = response.body()!!
                    if (quotes.isNotEmpty()) {
                        val quote = quotes[0]
                        val quoteText = "\"${quote.q}\" \n- ${quote.a}"
                        currentQuote = quoteText
                        binding.quoteTextView.text = quoteText
                    } else {
                        currentQuote = "Could not load a motivational quote."
                        binding.quoteTextView.text = currentQuote
                    }
                } else {
                    currentQuote = "Could not load a motivational quote."
                    binding.quoteTextView.text = currentQuote
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentQuote = "Error while loading the quote."
                binding.quoteTextView.text = currentQuote
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun startPeriodicQuoteRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(30000) 
                    fetchAndDisplayQuote()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("QUOTE_FETCHED", hasQuoteBeenFetched)
        outState.putString("CURRENT_QUOTE", currentQuote)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
