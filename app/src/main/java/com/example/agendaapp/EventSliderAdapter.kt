import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventListAdapter(private var events: List<String>) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    class EventViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
            textSize = 16f
        }
        return EventViewHolder(textView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.textView.text = events[position]
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<String>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
