import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.agendaapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore


class CalendarFragment : Fragment() {
    private val events = mutableMapOf<String, String>()
    private lateinit var selectedDate: String

    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var eventsAdapter: EventListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar_layout, container, false)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val addEventFab = view.findViewById<FloatingActionButton>(R.id.addEventFab)
        val eventDetailTextView = view.findViewById<TextView>(R.id.eventDetailTextView)
        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView)

        eventsAdapter = EventListAdapter(listOf()) { eventId ->
            showEventDetails(eventId)
        }
        eventsRecyclerView.adapter = eventsAdapter
        eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDate = dateFormatter.format(Date())

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            loadEventsFromFirebase()
        }

        addEventFab.setOnClickListener {
            showAddEventBottomSheet()
        }

        loadEventsFromFirebase()

        return view
    }



    private fun showAddEventBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_event, null)

        val eventNameInput = view.findViewById<EditText>(R.id.eventNameInput)
        val dateButton = view.findViewById<Button>(R.id.dateButton)
        val timeButton = view.findViewById<Button>(R.id.timeButton)
        val descriptionInput = view.findViewById<EditText>(R.id.descriptionInput)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        val locationInput = view.findViewById<EditText>(R.id.locationInput)

        // Set text colors (if you didn't do it in XML or want to ensure it)
        eventNameInput.setTextColor(Color.BLACK)
        descriptionInput.setTextColor(Color.BLACK) // Also for the location input
        locationInput.setTextColor(Color.BLACK)

        var selectedDateText = selectedDate
        var selectedTimeText = ""

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDateText = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                    dateButton.text = "Data: $selectedDateText"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedTimeText = String.format("%02d:%02d", hourOfDay, minute)
                    timeButton.text = "Ora: $selectedTimeText"
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }

        saveButton.setOnClickListener {
            val eventName = eventNameInput.text.toString().trim()
            val eventDescription = descriptionInput.text.toString().trim()
            val eventLocation = locationInput.text.toString().trim() // Get location text

            if (eventName.isNotEmpty() && selectedTimeText.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()

                val event = hashMapOf(
                    "time" to selectedTimeText,
                    "name" to eventName,
                    "description" to eventDescription,
                    "location" to eventLocation, // Add location to the map
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("events").document(selectedDateText)
                    .collection("eventList")
                    .add(event)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Evento salvato!", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                        loadEventsFromFirebase()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Compila nome evento e ora!", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bottomSheetDialog.show()
    }


    private fun loadEventsFromFirebase() {
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(selectedDate)
            .collection("eventList")
            .get()
            .addOnSuccessListener { documents ->
                val eventsList = documents.map { doc ->
                    Pair(doc.id, "${doc.getString("time")} - ${doc.getString("name")}")
                }
                if (eventsList.isEmpty()) {
                    eventsAdapter.updateEvents(listOf(Pair("", "Nessun evento per oggi")))
                } else {
                    eventsAdapter.updateEvents(eventsList)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // In CalendarFragment.kt, inside showEventDetails()

    private fun showEventDetails(eventId: String) {
        if (eventId.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(selectedDate)
            .collection("eventList")
            .document(eventId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val name = documentSnapshot.getString("name") ?: "Senza nome"
                    val time = documentSnapshot.getString("time") ?: "Senza orario"
                    val description = documentSnapshot.getString("description") ?: "Nessuna descrizione"
                    val location = documentSnapshot.getString("location") ?: ""

                    // Using Unicode characters for icons (find suitable ones)
                    val timeIcon = "🕒"    // Or "\u23F0"
                    val eventIcon = "📌"   // Or "\uD83D\uDCCC"
                    val locationIcon = "📍" // Or "\uD83D\uDCCD"
                    val descriptionIcon = "📝" // Or "\uD83D\uDCDD"

                    var detailsHtml = "<h2>${eventIcon} $name</h2>" +
                            "<p><b>${timeIcon} Orario:</b> $time</p>"

                    if (location.isNotBlank()) {
                        detailsHtml += "<p><b>${locationIcon} Luogo:</b> $location</p>"
                    }
                    if (description.isNotBlank()) {
                        detailsHtml += "<p><b>${descriptionIcon} Descrizione:</b><br/>$description</p>"
                    }

                    val dialogTitle = SpannableString("Dettagli Evento")
                    // You can style the title if needed, e.g., make it bold or change color

                    AlertDialog.Builder(requireContext())
                        .setTitle(dialogTitle)
                        .setMessage(Html.fromHtml(detailsHtml, Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .setNegativeButton("Apri Mappa") { _, _ -> // Placeholder for map intent
                            if (location.isNotBlank()) {
                                openLocationInMap(location)
                            } else {
                                Toast.makeText(requireContext(), "Nessun luogo specificato", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Dettagli evento non trovati.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Errore caricamento dettagli: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper function to open location in a map app
    private fun openLocationInMap(address: String) {
        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // Attempt to use Google Maps specifically
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback if Google Maps is not installed, try any app that can handle geo URI
            val genericMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}"))
            if (genericMapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(genericMapIntent)
            } else {
                Toast.makeText(requireContext(), "Nessuna app per le mappe trovata.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
