import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agendaapp.R

class EventListAdapter(
    private var events: List<Pair<String, String>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    class EventViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.eventTitle)
        val timeText: TextView = view.findViewById(R.id.eventTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false) as ViewGroup
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val (id, text) = events[position]
        val parts = text.split(" - ", limit = 2)
        val time = parts.getOrNull(0) ?: ""
        val title = parts.getOrNull(1) ?: text

        holder.titleText.text = title
        holder.timeText.text = time

        holder.itemView.setOnClickListener {
            onItemClick(id)
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Pair<String, String>>) {
        events = newEvents
        notifyDataSetChanged()
    }
}


