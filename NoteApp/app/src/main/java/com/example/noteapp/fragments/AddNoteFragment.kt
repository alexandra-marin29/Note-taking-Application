package com.example.noteapp.fragments

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentAddNoteBinding
import com.example.noteapp.model.Note
import com.example.noteapp.receiver.ReminderReceiver
import com.example.noteapp.util.createColorBorderDrawable
import com.example.noteapp.viewmodel.NoteViewModel
import java.util.Calendar

class AddNoteFragment : Fragment(R.layout.fragment_add_note), MenuProvider {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var addNoteView: View

    private var selectedColorHex: String = "#FFFFFFFF"

    private var folderId: Int = 1 // Default folderId

    private var isPinned: Boolean = false

    private var reminderTime: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        notesViewModel = (activity as MainActivity).noteViewModel
        addNoteView = view

        arguments?.let {
            folderId = it.getInt("folderId", 1)
        }
    }

    private fun saveNote(view: View) {
        val noteTitle = binding.addNoteTitle.text.toString().trim()
        val noteDesc = binding.addNoteDesc.text.toString().trim()

        if (noteTitle.isNotEmpty()) {
            val note = Note(
                id = 0,
                noteTitle = noteTitle,
                noteDesc = noteDesc,
                noteColor = selectedColorHex,
                dateCreated = System.currentTimeMillis(),
                folderId = folderId,
                isPinned = isPinned,
                reminderTime = reminderTime

            )
            notesViewModel.addNote(note) { newId ->
                if (reminderTime != null) {
                    val noteWithId = note.copy(id = newId.toInt())
                    scheduleReminder(noteWithId)
                }
                Toast.makeText(requireContext(), "Note Saved", Toast.LENGTH_SHORT).show()
                view.findNavController().popBackStack()
            }
        } else {
            Toast.makeText(requireContext(), "Please enter note title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleReminder(note: Note) {
        val reminderTimeInMillis = note.reminderTime ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Exact Alarm Permission Required")
                    setMessage("The app requires permission to set exact alarms for reminders. Please grant it in the app settings.")
                    setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                        startActivity(intent)
                    }
                    setNegativeButton("Cancel", null)
                    create()
                    show()
                }
                return
            }
        }

        try {
            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("noteId", note.id)
                putExtra("noteTitle", note.noteTitle)
                putExtra("noteDesc", note.noteDesc)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                note.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pendingIntent)

            Toast.makeText(requireContext(), "Reminder set successfully", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Permission for exact alarms is not granted.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_add_note, menu)

        val pinMenuItem = menu.findItem(R.id.pinMenu)
        if (isPinned) {
            pinMenuItem.setIcon(R.drawable.baseline_push_pin_24)
        } else {
            pinMenuItem.setIcon(R.drawable.baseline_push_pin_outline_24)
        }

        val reminderMenuItem = menu.findItem(R.id.reminderMenu)
        if (reminderTime != null) {
            reminderMenuItem.setIcon(R.drawable.baseline_time_filled_24)
        } else {
            reminderMenuItem.setIcon(R.drawable.baseline_time_outline_24)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.saveMenu -> {
                saveNote(addNoteView)
                true
            }
            R.id.pinMenu -> {
                isPinned = !isPinned
                if (isPinned) {
                    menuItem.setIcon(R.drawable.baseline_push_pin_24)
                } else {
                    menuItem.setIcon(R.drawable.baseline_push_pin_outline_24)
                }
                true
            }
            R.id.reminderMenu -> {
                openDateTimePicker(menuItem)
                true
            }
            R.id.settingsMenu -> {
                showColorPickerDialog()
                true
            }
            else -> false
        }
    }

    private fun openDateTimePicker(menuItem: MenuItem) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                reminderTime = calendar.timeInMillis
                menuItem.setIcon(R.drawable.baseline_time_filled_24)
            }

            TimePickerDialog(
                requireContext(), timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
            ).show()
        }

        DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                binding.addNoteDesc.background = createColorBorderDrawable(requireContext(), selectedColorHex)
                binding.addNoteTitle.background = createColorBorderDrawable(requireContext(), selectedColorHex)
            }
            create()
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
