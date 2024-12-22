package com.example.noteapp.fragments

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Patterns
import android.view.*
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteapp.MainActivity
import com.example.noteapp.R
import com.example.noteapp.databinding.FragmentCheckListBinding
import com.example.noteapp.model.Note
import com.example.noteapp.receiver.ReminderReceiver
import com.example.noteapp.viewmodel.NoteViewModel
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CheckListFragment : Fragment(R.layout.fragment_check_list), MenuProvider {

    private var _binding: FragmentCheckListBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NoteViewModel
    private var currentNote: Note? = null
    private var selectedColorHex: String = "#FFFFFFFF"

    private lateinit var checkListContainer: LinearLayout
    private lateinit var checklistTitleEditText: EditText

    private var folderId: Int = -1 // implicit Notes
    private var isPinned: Boolean = false

    private var reminderTime: Long? = null

    private val imageUriList = mutableListOf<String>()
    private val urlList = mutableListOf<String>()

    private val requestReadMediaImagesPermission = 1004
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyImageToInternalStorage(it)
            if (copiedUri != null) {
                imageUriList.add(copiedUri.toString())
                addImageToContainer(copiedUri)
                Toast.makeText(requireContext(), "Image added", Toast.LENGTH_SHORT).show()
            }
        }
    }
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

        arguments?.let {
            folderId = it.getInt("folderId", -1)
            currentNote = it.getParcelable("note")
            isPinned = currentNote?.isPinned ?: false
            reminderTime = currentNote?.reminderTime
        }

        if (currentNote == null) {
            addCheckListRow()
        } else {
            val noteTitle = currentNote!!.noteTitle
            val noteDesc = currentNote!!.noteDesc
            selectedColorHex = currentNote!!.noteColor ?: "#FFFFFFFF"

            checklistTitleEditText.setText(noteTitle)
            if (noteDesc.isNotEmpty()) {
                loadChecklistFromJson(noteDesc)
            } else {
                addCheckListRow()
            }
            applyColorToDescription(selectedColorHex)
            try {
                val imageUrisJson = JSONArray(currentNote!!.imageUris)
                for (i in 0 until imageUrisJson.length()) {
                    val uriString = imageUrisJson.getString(i)
                    imageUriList.add(uriString)
                    addImageToContainer(Uri.parse(uriString))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            try {
                val urlsJson = JSONArray(currentNote!!.urls)
                for (i in 0 until urlsJson.length()) {
                    val url = urlsJson.getString(i)
                    urlList.add(url)
                    addUrlToContainer(url)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }


    private fun addImageToContainer(uri: Uri) {
        binding.checkImagesContainer.visibility = View.VISIBLE

        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply {
                setMargins(8, 8, 8, 8)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.placeholder_image)
            Picasso.get()
                .load(uri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .resize(500, 500)
                .centerCrop()
                .into(this)
            tag = uri.toString()
        }

        imageView.setOnLongClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Delete Image")
                setMessage("Are you sure you want to delete this image?")
                setPositiveButton("Delete") { _, _ ->
                    binding.checkImagesContainer.removeView(imageView)
                    imageUriList.remove(uri.toString())
                    val file = File(Uri.parse(uri.toString()).path!!)
                    if (file.exists()) {
                        file.delete()
                    }
                    Toast.makeText(requireContext(), "Image deleted", Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("Cancel", null)
                create()
                show()
            }
            true
        }

        binding.checkImagesContainer.addView(imageView)
    }
    private fun copyImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
            null
        }
    }


    private fun addUrlToContainer(url: String) {
        binding.checkUrlsContainer.visibility = View.VISIBLE
        val urlTextView = TextView(requireContext()).apply {
            text = url
            setTextColor(Color.BLUE)
            textSize = 18f
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        urlTextView.setOnLongClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Delete URL")
                setMessage("Are you sure you want to delete this URL?")
                setPositiveButton("Delete") { _, _ ->
                    binding.checkUrlsContainer.removeView(urlTextView)
                    urlList.remove(url)
                    Toast.makeText(requireContext(), "URL deleted", Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("Cancel", null)
                create()
                show()
            }
            true
        }
        binding.checkUrlsContainer.addView(urlTextView)
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
                        val imm =
                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(newEdit, InputMethodManager.SHOW_IMPLICIT)
                        true
                    } else false
                } else false
            }
        }

        rowLayout.addView(checkBox)
        rowLayout.addView(editText)
        checkListContainer.addView(rowLayout)
        return editText
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
        val finalColor = selectedColorHex

        val dateNow = System.currentTimeMillis()

        val imageUrisJson = JSONArray(imageUriList).toString()
        val urlsJson = JSONArray(urlList).toString()

        if (currentNote == null) {
            val newNote = Note(
                id = 0,
                noteTitle = title,
                noteDesc = noteDesc,
                noteColor = finalColor,
                dateCreated = dateNow,
                folderId = folderId,
                isPinned = isPinned,
                reminderTime = reminderTime,
                imageUris = imageUrisJson,
                urls = urlsJson
            )
            notesViewModel.addNote(newNote) { newId ->
                if (reminderTime != null) {
                    val noteWithId = newNote.copy(id = newId.toInt())
                    scheduleReminder(noteWithId)
                }
                Toast.makeText(requireContext(), "Checklist Note Saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            val updatedNote = currentNote!!.copy(
                noteTitle = title,
                noteDesc = noteDesc,
                noteColor = finalColor,
                dateCreated = dateNow,
                isPinned = isPinned,
                reminderTime = reminderTime,
                imageUris = imageUrisJson,
                urls = urlsJson
            )
            notesViewModel.updateNote(updatedNote)
            if (reminderTime != null) {
                scheduleReminder(updatedNote)
            } else {
                cancelReminder(updatedNote)
            }
            Toast.makeText(requireContext(), "Checklist Note Updated", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun scheduleReminder(note: Note) {
        val reminderTimeInMillis = note.reminderTime ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    requireContext(),
                    "Permission for notifications is not granted.",
                    Toast.LENGTH_LONG
                ).show()
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

            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pendingIntent)

            Toast.makeText(requireContext(), "Reminder set successfully", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Permission for exact alarms is not granted.",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun cancelReminder(note: Note) {
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
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
                saveCheckList()
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
            R.id.addImageMenu -> {
                checkAndRequestReadMediaImagesPermission()
                true
            }

            R.id.addUrlMenu -> {
                showAddUrlDialog()
                true
            }

            else -> false
        }
    }

    private fun checkAndRequestReadMediaImagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Permission Required")
                        .setMessage("The app needs permission to access your images to add them to your notes.")
                        .setPositiveButton("Grant") { _, _ ->
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                requestReadMediaImagesPermission
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        requestReadMediaImagesPermission
                    )
                }
            } else {
                openImagePicker()
            }
        } else { // Android versions below 13
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Permission Required")
                        .setMessage("The app needs permission to access your images to add them to your notes.")
                        .setPositiveButton("Grant") { _, _ ->
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                requestReadMediaImagesPermission
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        requestReadMediaImagesPermission
                    )
                }
            } else {
                openImagePicker()
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestReadMediaImagesPermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied to read images.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showAddUrlDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add URL")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        input.hint = "https://example.com"
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val url = input.text.toString().trim()
            if (url.isNotEmpty()) {
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    urlList.add(url)
                    addUrlToContainer(url)
                    Toast.makeText(requireContext(), "URL added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
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
                applyColorToDescription(selectedColorHex)
            }
            create()
            show()
        }
    }

    private fun applyColorToDescription(colorHex: String) {
        selectedColorHex = colorHex
        val chosenColor = Color.parseColor(colorHex)

        checklistTitleEditText.setBackgroundColor(chosenColor)

        for (i in 0 until checkListContainer.childCount) {
            val row = checkListContainer.getChildAt(i) as LinearLayout
            val checkBox = row.getChildAt(0) as CheckBox
            val editText = row.getChildAt(1) as EditText
            row.setBackgroundColor(chosenColor)
            checkBox.setBackgroundColor(Color.TRANSPARENT)
            editText.setBackgroundColor(chosenColor)
        }

        currentNote = currentNote?.copy(noteColor = colorHex) ?: currentNote
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
