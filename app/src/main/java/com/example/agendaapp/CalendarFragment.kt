import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agendaapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder


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
        if (eventId.isEmpty()) {
            // It's good practice to handle cases where eventId might be empty,
            // e.g., if you have placeholder items in your list.
            // Toast.makeText(requireContext(), "Nessun dettaglio per questo elemento.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(selectedDate) // Assumes 'selectedDate' is a class property
            .collection("eventList")
            .document(eventId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val name = documentSnapshot.getString("name") ?: "Senza nome"
                    val time = documentSnapshot.getString("time") ?: "Senza orario"
                    val description = documentSnapshot.getString("description") ?: "" // Default to empty
                    val location = documentSnapshot.getString("location") ?: ""    // Default to empty

                    // Inflate the custom dialog layout
                    val dialogView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_event_details_minimal, null)

                    // Get references to views in the custom layout
                    val eventNameText = dialogView.findViewById<TextView>(R.id.dialog_event_name_text)
                    val eventTimeText = dialogView.findViewById<TextView>(R.id.dialog_event_time_text)

                    val locationLayout = dialogView.findViewById<LinearLayout>(R.id.dialog_location_layout)
                    val eventLocationText = dialogView.findViewById<TextView>(R.id.dialog_event_location_text)

                    val descriptionLayout = dialogView.findViewById<LinearLayout>(R.id.dialog_description_layout)
                    val eventDescriptionText = dialogView.findViewById<TextView>(R.id.dialog_event_description_text)

                    val mapImageView = dialogView.findViewById<ImageView>(R.id.dialog_event_map_image)

                    // Set data to the views
                    eventNameText.text = name
                    eventTimeText.text = time

                    if (location.isNotBlank()) {
                        locationLayout.visibility = View.VISIBLE
                        eventLocationText.text = location

                        // --- Logic for Static Map Image ---
                        // IMPORTANT: You need a Google Static Maps API key for this to work reliably
                        // and to avoid watermarks or restrictions in production.
                        // For development, it might work without a key for a limited number of requests.
                        // Replace "YOUR_STATIC_MAPS_API_KEY" with your actual key if you have one.
                        // If you don't have a key, it might still work for testing but is not guaranteed.
                        val apiKey = "AIzaSyBjlhMquK2E5h-H3yNtD_H36HQnXgw3jww" // TODO: Replace or manage securely
                        try {
                            val encodedLocation = URLEncoder.encode(location, "UTF-8")
                            val mapUrl = "https://maps.googleapis.com/maps/api/staticmap?" +
                                    "center=$encodedLocation" +
                                    "&zoom=15" + // Adjust zoom level as needed
                                    "&size=600x300" + // Adjust size as needed
                                    "&maptype=roadmap" +
                                    "&markers=color:red%7Clabel:S%7C$encodedLocation" +
                                    "&key=$apiKey" // Add your API key here

                            mapImageView.visibility = View.VISIBLE
                            Glide.with(requireContext())
                                .load(mapUrl)
                                .placeholder(R.drawable.ic_location) // Optional: a placeholder drawable
                                .error(R.drawable.ic_broken_image) // Optional: an error drawable
                                .into(mapImageView)

                            // Make the map image clickable to open in Google Maps app
                            mapImageView.setOnClickListener {
                                openLocationInMapApp(location)
                            }

                        } catch (e: Exception) {
                            // Handle encoding exception if necessary
                            mapImageView.visibility = View.GONE
                            Log.e("ShowEventDetails", "Error encoding location for map URL", e)
                        }
                        // --- End of Static Map Image Logic ---
                    } else {
                        locationLayout.visibility = View.GONE
                        mapImageView.visibility = View.GONE
                    }

                    if (description.isNotBlank()) {
                        descriptionLayout.visibility = View.VISIBLE
                        eventDescriptionText.text = description
                    } else {
                        descriptionLayout.visibility = View.GONE
                    }

                    // Build and show the AlertDialog
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setView(dialogView) // Set the custom view

                    val dialog = builder.create()
                    // Apply the rounded background to the dialog's window
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
                    dialog.show()

                } else {
                    Toast.makeText(requireContext(), "Dettagli evento non trovati.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Errore caricamento dettagli: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("ShowEventDetails", "Error fetching event details", exception)
            }
    }

    // Helper function to open location in a map app (you might have this already)
    private fun openLocationInMapApp(address: String) {
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
