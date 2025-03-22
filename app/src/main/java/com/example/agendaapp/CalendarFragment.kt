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
import androidx.viewpager2.widget.ViewPager2
import com.example.agendaapp.R
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore


class CalendarFragment : Fragment() {
    private val events = mutableMapOf<String, String>()
    private lateinit var selectedDate: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar_layout, container, false)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val eventSlider = view.findViewById<ViewPager2>(R.id.eventSlider)
        val addEventButton = view.findViewById<Button>(R.id.addEventButton)

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDate = dateFormatter.format(Date())

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            loadEventsFromFirebase(eventSlider) // 🔥 Carica gli eventi da Firebase
        }


        addEventButton.setOnClickListener {
            showAddEventDialog(eventSlider) // 🔥 Passiamo il ViewPager2 invece di eventTextView
        }

        return view
    }

    private fun showAddEventDialog(eventSlider: ViewPager2){
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Aggiungi Evento")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val eventNameInput = EditText(requireContext())
        eventNameInput.hint = "Nome evento"
        layout.addView(eventNameInput)

        val dateButton = Button(requireContext())
        dateButton.text = "Seleziona Data"
        layout.addView(dateButton)

        val timeButton = Button(requireContext())
        timeButton.text = "Seleziona Ora"
        layout.addView(timeButton)

        val eventDescriptionInput = EditText(requireContext())
        eventDescriptionInput.hint = "Descrizione (opzionale)"
        layout.addView(eventDescriptionInput)

        builder.setView(layout)

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

        builder.setPositiveButton("Salva") { _, _ ->
            val eventName = eventNameInput.text.toString().trim()
            val eventDescription = eventDescriptionInput.text.toString().trim()

            if (eventName.isNotEmpty() && selectedTimeText.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()

                val event = hashMapOf(
                    "time" to selectedTimeText,
                    "name" to eventName,
                    "description" to eventDescription,
                    "timestamp" to FieldValue.serverTimestamp() // 🔥 Timestamp automatico
                )

                db.collection("events").document(selectedDate) // 🔥 Organizza eventi per data
                    .collection("eventList")
                    .add(event)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Evento salvato!", Toast.LENGTH_SHORT).show()
                        loadEventsFromFirebase(eventSlider) // 🔥 Aggiorniamo il ViewPager2
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Compila tutti i campi!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun loadEventsFromFirebase(eventSlider: ViewPager2) {
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(selectedDate) // 🔥 Prende il documento della data selezionata
            .collection("eventList") // 🔥 Prende la lista degli eventi di quel giorno
            .get()
            .addOnSuccessListener { documents ->
                val eventsList = documents.map { doc ->
                    "${doc.getString("name")} - ${doc.getString("time")}\n${doc.getString("description")}"
                }
                if (eventsList.isEmpty()) {
                    eventSlider.adapter = EventSliderAdapter(listOf("Nessun evento per questa data"))
                } else {
                    eventSlider.adapter = EventSliderAdapter(eventsList)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
