import android.os.Build
import androidx.annotation.RequiresApi
import java.time.*
import java.time.format.TextStyle
import java.util.*

class CalendarManager {
    private val events = mutableMapOf<LocalDate, MutableList<String>>()

    @RequiresApi(Build.VERSION_CODES.O)
    fun printMonthCalendar(year: Int, month: Int) {
        val firstDayOfMonth = LocalDate.of(year, month, 1)
        val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

        println("Calendario di ${firstDayOfMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)} $year")
        println("L  M  M  G  V  S  D")

        var currentDay = firstDayOfMonth
        for (i in 1 until currentDay.dayOfWeek.value) print("   ")

        while (!currentDay.isAfter(lastDayOfMonth)) {
            print("%2d ".format(currentDay.dayOfMonth))
            if (currentDay.dayOfWeek == DayOfWeek.SUNDAY) println()
            currentDay = currentDay.plusDays(1)
        }
        println("\n")
    }

    fun addEvent(date: LocalDate, event: String) {
        events.computeIfAbsent(date) { mutableListOf() }.add(event)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEvents(date: LocalDate) {
        val eventList = events[date]
        if (eventList.isNullOrEmpty()) {
            println("Nessun evento per il ${date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        } else {
            println("Eventi per il ${date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}:")
            eventList.forEach { println("- $it") }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun main() {
    val calendar = CalendarManager()
    val today = LocalDate.now()

    // Esempi di utilizzo
    calendar.printMonthCalendar(today.year, today.monthValue)
    calendar.addEvent(today, "Incontro con il team")
    calendar.addEvent(today, "Allenamento alle 18:00")
    calendar.getEvents(today)
}
