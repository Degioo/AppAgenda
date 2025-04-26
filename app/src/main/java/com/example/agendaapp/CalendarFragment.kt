import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView)

        eventsAdapter = EventListAdapter(listOf())
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

            if (eventName.isNotEmpty() && selectedTimeText.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()

                val event = hashMapOf(
                    "time" to selectedTimeText,
                    "name" to eventName,
                    "description" to eventDescription,
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
                Toast.makeText(requireContext(), "Compila tutti i campi!", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }


    private fun loadEventsFromFirebase() {
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(selectedDate)
            .collection("eventList")
            .get()
            .addOnSuccessListener { documents ->
                val eventsList = documents.map { doc ->
                    "${doc.getString("time")} - ${doc.getString("name")}"
                }
                if (eventsList.isEmpty()) {
                    eventsAdapter.updateEvents(listOf("Nessun evento per oggi"))
                } else {
                    eventsAdapter.updateEvents(eventsList)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }



}
