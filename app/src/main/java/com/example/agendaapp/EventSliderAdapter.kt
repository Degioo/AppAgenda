import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventSliderAdapter(private val events: List<String>) : RecyclerView.Adapter<EventSliderAdapter.EventViewHolder>() {

    class EventViewHolder(val eventText: TextView) : RecyclerView.ViewHolder(eventText)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textSize = 24f
            setPadding(16, 16, 16, 16)
        }
        return EventViewHolder(textView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.eventText.text = events[position]
    }

    override fun getItemCount(): Int = events.size
}
